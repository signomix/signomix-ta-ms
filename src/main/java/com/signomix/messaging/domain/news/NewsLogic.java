package com.signomix.messaging.domain.news;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.NewsDaoIface;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.hcms.Document;
import com.signomix.common.news.NewsDefinition;
import com.signomix.common.news.NewsEnvelope;
import com.signomix.common.news.UserNewsDto;
import com.signomix.messaging.adapter.out.HcmsService;
import com.signomix.messaging.adapter.out.MailerService;
import com.signomix.messaging.domain.user.UserLogic;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NewsLogic {

    @Inject
    Logger logger = Logger.getLogger(NewsLogic.class);

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;
    UserDaoIface userDao;

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDataSource;
    NewsDaoIface newsDao;

    @Inject
    UserLogic userLogic;
    @Inject
    HcmsService hcmsService;
    @Inject
    MailerService mailerService;

    @ConfigProperty(name = "signomix.admin.email", defaultValue = "")
    String adminEmail;

    @ConfigProperty(name = "signomix.default.language", defaultValue = "pl")
    String defaultLanguage;

    void onStart(@Observes StartupEvent ev) {
        userDao = new com.signomix.common.tsdb.UserDao();
        userDao.setDatasource(userDataSource);
        newsDao = new com.signomix.common.tsdb.NewsDao();
        newsDao.setDatasource(tsDataSource);
    }

    public UserNewsDto getNewsForUser(User user, String language, Long limit, Long offset) {
        UserNewsDto userNews = new UserNewsDto();
        try {
            userNews = newsDao.getUserNews(user.uid, language, null, limit, offset);
        } catch (IotDatabaseException e) {
            logger.error("Error getting news for user: " + e.getMessage());
            userNews.errorMessage = e.getMessage();
        }
        return userNews;
    }

    public Document getNewsIssue(User user, Long newsId, String language) {
        // get news issue
        try {
            Document document = newsDao.getNewsDocument(newsId, language);
            if (document == null) {
                document = newsDao.getNewsDocument(newsId, defaultLanguage);
            }
            return document;
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Document doc = new Document();
            doc.content = "Document not found<br>" + e.getMessage();
            doc.name = "Error";
            doc.metadata.put("title", "Error");
            return doc;
        }
    }

    public void sendNews(User user, NewsDefinition news) {
        // send news
        if (user.type != User.OWNER) {
            logger.warn("User is not owner, news not sent");
            return;
        }
        if (news.name == null || news.name.isEmpty()) {
            logger.warn("News name is empty, news not sent");
            return;
        }
        if (news.type == null) {
            logger.warn("News type is empty, news not sent");
            return;
        }
        HashMap<String, Document> documents = new HashMap<>();
        for (String key : news.documents.keySet()) {
            // get document from Hcms
            Document doc = hcmsService.getDocument(news.documents.get(key));
            if (doc != null) {
                documents.put(key, doc);
            }
        }
        if (documents.isEmpty()) {
            logger.warn("No documents found for news: " + news.name);
            return;
        }
        // check if all languages are present
        // if not, use document in default language
        for (String key : news.documents.keySet()) {
            if (documents.get(key) == null) {
                Document doc = documents.get(defaultLanguage);
                if (doc != null) {
                    documents.put(key, doc);
                }
            }
        }

        // save news definition
        long newsId;
        try {
            newsId = newsDao.saveNewsDefinition(news);
        } catch (IotDatabaseException e) {
            logger.error("Error saving news definition: " + e.getMessage());
            return;
        }
        // save news documents
        news.id = newsId;
        try {
            newsDao.saveNewsDocuments(newsId, (Map<String, Document>) documents);
        } catch (IotDatabaseException e) {
            logger.error("Error saving news documents: " + e.getMessage());
            return;
        }

        if (news.userId != null) {
            // send news to user
            sendNewsToUser(news.userId, news, documents);
        } else if (news.organizationId != null && news.tenant == null) {
            // send news to organization
            logger.info("Sending news to organization: " + news.organizationId);
        } else if (news.organizationId != null && news.tenant != null) {
            // send news to tenant
            logger.info("Sending news to tenant: " + news.tenant);
        } else if (news.target != null) {
            // send news to target
            logger.info("Sending news to target group: " + news.target);
            sendNewsToTargetGroup(news.id, news.target, documents);
        } else {
            logger.error("Unable to determine news recipients. News not sent.");
        }

    }

    private void sendNewsToUser(String uid, NewsDefinition news, HashMap<String, Document> documents) {
        logger.info("Sending news to user: " + uid);
        try {
            User user = userDao.getUser(uid);
            if (user == null) {
                logger.warn("User not found: " + uid);
                return;
            }
            Document doc = documents.get(user.preferredLanguage);
            if (doc == null) {
                doc = documents.get(defaultLanguage);
            }
            if (doc == null) {
                logger.warn("No document found for user: " + uid);
                return;
            }
            // save news envelope
            String language;
            if (documents.get(user.preferredLanguage) != null) {
                language = user.preferredLanguage;
            } else {
                language = defaultLanguage;
            }
            saveNewsEnvelope(user, news.id, language);

            // send email
            String subject = doc.metadata.get("title");
            if (subject == null) {
                subject = doc.name;
            }
            logger.info("subject: " + subject);
            logger.info("content: " + doc.content);
            mailerService.sendHtmlEmail(user.email, subject, doc.content, null, null);
        } catch (IotDatabaseException e) {
            logger.warn("Error getting user: " + uid);
        }
    }

    private void saveNewsEnvelope(User user, long newsId, String language) {
        NewsEnvelope newsEnvelope = new NewsEnvelope();
        newsEnvelope.newsId = newsId;
        newsEnvelope.userId = user.uid;
        newsEnvelope.language = language;
        try {
            newsDao.saveNewsEnvelope(newsEnvelope);
        } catch (IotDatabaseException e) {
            logger.error("Error saving news envelope: " + e.getMessage());
        }
    }

    private void sendNewsToTargetGroup(long newsId, String targetGroup, HashMap<String, Document> documents) {
        logger.info("Sending news to target group: " + targetGroup);
        List<User> users = new ArrayList<>();
        try {
            if (targetGroup == null || targetGroup.isEmpty() || targetGroup.equals("*")) {
                users = userDao.getUsers(10000,0, null, null);
            } else {
                users = userDao.getUsersByRole(targetGroup);
            }
        } catch (IotDatabaseException e) {
            logger.warn("Error getting users for target group: " + targetGroup);
            return;
        }

        if (users == null || users.isEmpty()) {
            if (targetGroup == null || targetGroup.isEmpty()) {
                logger.warn("No users found");
            } else {
                logger.warn("No users found for target group: " + targetGroup);
            }
            return;
        }

        Document doc;
        // get list of documents keys
        List<String> languages = new ArrayList<>();
        for (String key : documents.keySet()) {
            languages.add(key);
        }
        String language;
        for (int i = 0; i < languages.size(); i++) {
            language = languages.get(i);
            doc = documents.get(language);
            // send email
            String subject = doc.metadata.get("title");
            if (subject == null) {
                subject = doc.name;
            }
            logger.info("subject: " + subject);
            logger.info("content: " + doc.content);
            int maxCounter = 50;
            int counter = 0;
            List<String> emails = new ArrayList<>();
            for (User user : users) {
                if(user.email==null || user.email.isEmpty()){
                    continue;
                }
                if (user.preferredLanguage.equals(language)) {
                    saveNewsEnvelope(user, newsId, language);
                    emails.add(user.email.trim());
                } else if (language.equals(defaultLanguage) && !languages.contains(user.preferredLanguage)) {
                    saveNewsEnvelope(user, newsId, language);
                    emails.add(user.email.trim());
                }
                counter++;
                if (counter > maxCounter) {
                    mailerService.sendHtmlEmail(adminEmail, subject, doc.content, emails, null);
                    emails.clear();
                    counter = 0;
                }
            }
            if (!emails.isEmpty()) {
                mailerService.sendHtmlEmail(adminEmail, subject, doc.content, emails, null);
            }
        }

    }

}
