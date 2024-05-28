package com.signomix.messaging.domain.user;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.messaging.adapter.out.HcmsService;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;
import com.signomix.messaging.domain.Document;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class UserLogic {

    @Inject
    Logger logger;

    /*
     * @Inject
     * MailerService mailerService;
     */

    @Inject
    MessageProcessorAdapter messagePort;

    @ConfigProperty(name = "signomix.mqtt.field.separator", defaultValue = ";")
    String mqttFieldSeparator;

    @ConfigProperty(name = "signomix.api.url", defaultValue = "https://localhost")
    String apiUrl;
    @ConfigProperty(name = "signomix.webapp.url", defaultValue = "https://cloud.localhost")
    String webappUrl;

    @ConfigProperty(name = "signomix.hcms.api.path", defaultValue = "")
    String hcmsApiPath;
    /**
     * Path to the document with the ask-to-confirm message. Resulting path using
     * defaultValue
     * given below will be: hcmsApiPath + /templates/{language}/ +
     * askToConfirmDocumentPath
     */
    @ConfigProperty(name = "signomix.hcms.ask-to-confirm", defaultValue = "ask-to-confirm.html")
    String askToConfirmDocumentPath;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;

    AuthDaoIface dao;
    UserDaoIface userDao;

    @Inject
    HcmsService hcmsService;

    void onStart(@Observes StartupEvent ev) {
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
    }

    public void processUserEvent(String message) {
        logger.info("User event: " + message);
        String[] parts = message.split(mqttFieldSeparator);
        if (parts.length < 3) {
            logger.error("Wrong user event message: " + message);
            return;
        }
        String eventType = parts[0];
        String admin = parts[1];
        String userId = parts[2];

        switch (eventType.toLowerCase()) {
            case "created":
                sendAskToConfirmEmail(userId);
                break;
            case "activated":
            case "created_and_activated":
                sendConfirmationEmail(userId);
                break;
            case "updated":
                break;
            case "remove_requested":
                sendAccountRemoveRequestedEmail(userId);
                break;
            case "removed":
                sendAccountRemovedEmail(userId);
                break;
            case "password_reset":
                sendPasswordResetEmail(userId);
                break;
            default:
        }
    }

    /**
     * Sends an email with a confirmation link to the user.
     * @param userLogin
     */
    private void sendAskToConfirmEmail(String userLogin) {
        User user = null;
        try {
            user = userDao.getUser(userLogin);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == user) {
            logger.error("User not found: " + userLogin);
            return;
        }

        Document doc = null;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            doc = hcmsService.getDocument(hcmsApiPath + "/pl/" + askToConfirmDocumentPath);
        } else {
            doc = hcmsService.getDocument(hcmsApiPath + "/en/" + askToConfirmDocumentPath);
        }
        if (null == doc) {
            logger.info("Document not found: " + doc.path);
            return;
        }
        String message = doc.content;
        HashMap<String,String> valueMap = new HashMap<>();
        valueMap.put("API_URL", apiUrl);
        valueMap.put("WEBAPP_URL", webappUrl);

        message = replacePlaceholders(message, user, valueMap);
        String subject = doc.metadata.get("subject");

        /*
        String subject_en = "Confirm Your Signomix Registration";
        String subject_pl = "Potwierdź rejestrację w Signomix";
        String message_en = "<p>Thank you for registering with Signomix.</p>"
                + "<p>To activate your account, please confirm your registration by clicking on <a href=\"" + apiUrl
                + "/api/account/confirm?key=" + user.confirmString + "&r=" + webappUrl + "\">this link</a>.<br>"
                + "If you have any concerns or did not initiate this registration, please send e-mail to signomix@signomix.com.<br>"
                + "If you prefer not to register, you can simply ignore this email.<br>"
                + "We look forward to welcoming you to Signomix.</p>"
                + "<p>Best regards,<br>"
                + "Grzegorz Skorupa</p>";
        String message_pl = "<p>Dziękuję za rejestrację w Signomix.</p>"
                + "<p>Aby aktywować swoje konto, proszę o potwierdzenie rejestracji, klikając na <a href=\"" + apiUrl
                + "/api/account/confirm?key=" + user.confirmString + "&r=" + webappUrl + "\">ten link</a>.<br>"
                + "Jeśli masz jakiekolwiek wątpliwości lub nie zainicjowałeś/aś tej rejestracji, skontaktuj się wysyłając e-mail na adres signomix@signomix.com.<br>"
                + "Jeśli nie chcesz się rejestrować, możesz po prostu zignorować tę wiadomość.<br>"
                + "Czekamy z niecierpliwością na powitanie Cię w Signomiksie.</p>"
                + "<p>Pozdrawiam,<br>"
                + "Grzegorz Skorupa</p>";
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            message = message_pl;
            subject=subject_pl;
        } else {
            message = message_en;
            subject=subject_en;
        }
        */
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.message = message;
        envelope.subject = subject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);
    }

    private void sendPasswordResetEmail(String userLogin) {
        User user = null;
        try {
            user = userDao.getUser(userLogin);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == user) {
            logger.error("User not found: " + userLogin);
            return;
        }
        String subject_en = "Signomix Password Reset";
        String subject_pl = "Reset hasła w Signomix";

        String message_en = "<p>Dear User,</p>"
                + "<p>We have received a request to reset your password.</p>"
                + "<p>To reset your password, please click on <a href=\"" + webappUrl
                + "/account/setpassword?key=" + user.confirmString + "&login=" + user.uid + "&language="
                + user.preferredLanguage + "\">this link</a>.<br>"
                + "If you have any concerns or did not initiate this request, please send e-mail to signomix@signomix.com.<br>"
                + "If you prefer not to reset your password, you can simply ignore this email.</p>"
                + "<p>Best regards,<br>"
                + "Grzegorz Skorupa</p>";
        String message_pl = "<p>Szanowny Użytkowniku,</p>"
                + "<p>Otrzymaliśmy prośbę o zresetowanie Twojego hasła.</p>"
                + "<p>Aby zresetować hasło, proszę o kliknięcie na <a href=\"" + webappUrl
                + "/account/setpassword?key=" + user.confirmString + "&login=" + user.uid + "&language="
                + user.preferredLanguage + "\">ten link</a>.<br>"
                + "Jeśli masz jakiekolwiek wątpliwości lub nie zainicjowałeś/aś tej prośby, skontaktuj się wysyłając e-mail na adres signomix@signomix.com.<br>"
                + "Jeśli nie chcesz zresetować hasła, możesz po prostu zignorować tę wiadomość.</p>"
                + "<p>Pozdrawiam,<br>"
                + "Grzegorz Skorupa</p>";

        String message;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            message = message_pl;
        } else {
            message = message_en;
        }
        String subject;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            subject = subject_pl;
        } else {
            subject = subject_en;
        }
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.message = message;
        envelope.subject = subject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);
    }

    /**
     * Sends an email with a confirmation link to the user.
     * @param userLogin
     */
    private void sendConfirmationEmail(String userLogin) {
        User user = null;
        try {
            user = userDao.getUser(userLogin);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == user) {
            logger.error("User not found: " + userLogin);
            return;
        }

        Document doc = null;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            doc = hcmsService.getDocument(hcmsApiPath + "/pl/" + askToConfirmDocumentPath);
        } else {
            doc = hcmsService.getDocument(hcmsApiPath + "/en/" + askToConfirmDocumentPath);
        }
        if (null == doc) {
            logger.info("Document not found: " + doc.path);
            return;
        }
        String message = doc.content;
        HashMap<String,String> valueMap = new HashMap<>();
        valueMap.put("API_URL", apiUrl);
        valueMap.put("WEBAPP_URL", webappUrl);

        message = replacePlaceholders(message, user, valueMap);
        String subject = doc.metadata.get("subject");
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.message = message;
        envelope.subject = subject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);
    }

    private void sendAccountRemoveRequestedEmail(String userLogin) {
        String subject_en = "Signomix: Account Removal Requested";
        String subject_pl = "Signomix: Prośba o usunięcie konta";
        String message_en = "<p>Dear User,</p>"
                + "<p>We have received a request to remove your account from Signomix.</p>"
                + "<p>If you did not initiate this request or changed your mind, please send e-mail to signomix@signomix.com.<br>"
                + "Otherwise your account will be removed in 7 days.</p>";
        String message_pl = "<p>Szanowny Użytkowniku,</p>"
                + "<p>Otrzymaliśmy prośbę o usunięcie Twojego konta z Signomix.</p>"
                + "<p>Jeśli nie zainicjowałeś/aś tej prośby lub zmieniłeś/aś zdanie, proszę o wysłanie e-maila na adres signomix@signomix.com.<br>"
                + "W przeciwnym razie Twoje konto zostanie usunięte za 7 dni.</p>";

        User user = null;
        try {
            user = userDao.getUser(userLogin);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == user) {
            logger.error("User not found: " + userLogin);
            return;
        }
        String message;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            message = message_pl;
        } else {
            message = message_en;
        }
        String subject;
        if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            subject = subject_pl;
        } else {
            subject = subject_en;
        }

        MessageEnvelope envelope = new MessageEnvelope();
        envelope.message = message;
        envelope.subject = subject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);

    }

    private void sendAccountRemovedEmail(String user) {
        // TODO: send email
        // TODO: remove user from database only after sending email
    }

    /**
     * Finds all words starting with { and ending with }.
     * 
     * @param text
     * @return list of words
     */
    public static String[] findPlaceholders(String text) {
        return text.split("\\{[^\\}]*\\}");
    }

    /**
     * Replaces placeholders in the text with values and User fields.
     * 
     * @param text
     * @param user
     * @param valueMap
     * @return
     */
    public static String replacePlaceholders(String text, User user, HashMap<String,String> valueMap) {
        String result = text;
        String[] placeholders = findPlaceholders(text);
        result = result.replace("{USER_NAME}", user.name);
        result = result.replace("{USER_SURNAME}", user.surname);
        result = result.replace("{USER_EMAIL}", user.email);
        result = result.replace("{USER_SECRET}", user.confirmString);
        for(int i=0;i<placeholders.length;i++) {
            String key = placeholders[i].substring(1,placeholders[i].length()-1);
            if(valueMap.containsKey(key)) {
                result = result.replace("{"+key+"}", valueMap.get(key));
            }
        }
        return result;
    }

}
