package com.signomix.messaging.adapter.out;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.cricketmsf.microsite.cms.Document;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.EventEnvelope;
import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.messaging.application.port.out.ContentServiceClient;
import com.signomix.messaging.application.port.out.MessageProcessorPort;
import com.signomix.messaging.application.usecase.AuthUC;
import com.signomix.messaging.application.usecase.DeviceUC;
import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.Message;
import com.signomix.messaging.domain.Status;
import com.signomix.messaging.webhook.WebhookService;

@ApplicationScoped
public class MqttProcessorAdapter implements MessageProcessorPort {
    private static final Logger LOG = Logger.getLogger(MessageProcessorAdapter.class);

    protected MailerService mailerService;
    IotDatabaseIface dao;

    String appKey;
    String authHost;

    @Inject
    AuthUC authUC;

    @Inject
    DeviceUC deviceUC;

    @Inject
    SmsplanetService smsService;

    MailingActionRepository mailingRepository;

    public MqttProcessorAdapter() {
    }

    // @ConfigProperty(name = "signomix.admin.email", defaultValue = "")
    String adminEmail = ConfigProvider.getConfig().getValue("signomix.admin.email", String.class);
    // @ConfigProperty(name = "signomix.document.welcome.uid", defaultValue = "")
    String welcomeDocUid = ConfigProvider.getConfig().getValue("signomix.document.welcome.uid", String.class);

    @Override
    public void setMailingRepository(MailingActionRepository repository) {
        mailingRepository = repository;
    }

    @Override
    public void processEvent(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.debug("EVENT: " + message);
        EventEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        LOG.debug(wrapper.type + " " + wrapper.uuid + " " + wrapper.payload);
    }

    @Override
    public void processAdminEmail(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.debug("ADMIN_EMAIL " + message);
        MessageEnvelope wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, MessageEnvelope.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        String emailAddress = wrapper.user.email;
        if (null == emailAddress || emailAddress.isEmpty()) {
            emailAddress = adminEmail;
            LOG.warn("email not set, usinng admin email from application properties");
            if (null == emailAddress || emailAddress.isEmpty()) {
                LOG.error("application property signomix.admin.email not set");
                return;
            }
        }
        mailerService.sendEmail(emailAddress, wrapper.subject, wrapper.message);
    }

    @Override
    public void processNotification(byte[] bytes) {
        try {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOG.info("Received: " + message); // "userId\teui\tmessageType\tmessageText"
            String[] params = message.split("\t");
            String userId = params[0];
            String deviceEui = params[1];
            String messageType = params[2];
            String messageText = params[3];

            String address = null;
            String messageChannel = null;
            User user = getUser(userId);
            String[] channelConfig = user.getChannelConfig(messageType);
            if (channelConfig == null || channelConfig.length < 2) {
                LOG.debug("Channel not configured " + messageType + " " + channelConfig.length);
            } else {
                LOG.debug(channelConfig[0] + " " + channelConfig[1]);
                messageChannel = channelConfig[0];
                if (channelConfig.length == 2) {
                    address = channelConfig[1];
                } else {
                    // in case when address has ':'
                    address = "";
                    for (int i = 1; i < channelConfig.length - 1; i++) {
                        address = address + channelConfig[i] + ":";
                    }
                    address = address + channelConfig[channelConfig.length - 1];
                }

                if (null != address && !address.isEmpty()) {
                    switch (messageChannel.toUpperCase()) {
                        case "SMTP":
                            LOG.info("sending with SMTP");
                            mailerService.sendEmail(address, deviceEui, messageText);
                            break;
                        case "WEBHOOK":
                            LOG.info("sending with WEBHOOK");
                            new WebhookService().send(address, new Message(deviceEui, messageText));
                            break;
                        case "SMS":
                            LOG.info("sending with SMS");
                            if (user.credits > 0) {
                                
                                smsService.send(user, address, new Message(deviceEui, messageText));
                            } else {
                                // TODO: error
                            }
                            break;
                        default:
                            LOG.warnf("Unsupported message type %1s", messageType);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void processMailing(MailingAction action) {
        String docUid = action.getDocUid();
        String err = null;
        String target = action.getTarget();
        Document docPl = getDocument(docUid, "pl");
        Document docEn = getDocument(docUid, "en");
        if (null == docPl && null == docEn) {
            err = "document not found";
            LOG.error(err + " " + docUid);
            action.setStatus(Status.Failed);
            action.setError(err);
            return;
        }
        User user;
        String subjectPL;
        String contentPL;
        String subjectEN;
        String contentEN;
        try {
            subjectPL = URLDecoder.decode(docPl.getTitle(), "UTF-8");
            contentPL = URLDecoder.decode(docPl.getContent(), "UTF-8");
            subjectEN = URLDecoder.decode(docEn.getTitle(), "UTF-8");
            contentEN = URLDecoder.decode(docEn.getContent(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex.getMessage());
            action.setStatus(Status.Failed);
            action.setError(ex.getMessage());
            // save?
            return;
        }
        action.setStartedAt(new Date());
        String subject;
        String content;
        List<User> users = authUC.getUsers(target);
        for (int i = 0; i < users.size(); i++) {
            user = users.get(i);
            // skip if no e-mail is provided
            if (null == user.email || user.email.isEmpty()) {
                continue;
            }
            switch (user.preferredLanguage.toUpperCase()) {
                case "PL":
                    subject = subjectPL;
                    content = contentPL;
                    try {
                        content = content.replaceFirst("\\$user.name", user.name);
                        content = content.replaceFirst("\\$mailing.name", user.surname);
                        content = content.replaceFirst("\\$user.uid", user.uid);
                    } catch (Exception e) {
                        LOG.warn(e);
                    }
                    break;
                // case "EN":
                default:
                    subject = subjectEN;
                    content = contentEN;
                    try {
                        content = content.replaceFirst("\\$user.name", user.name);
                        content = content.replaceFirst("\\$mailing.name", user.surname);
                        content = content.replaceFirst("\\$user.uid", user.uid);
                    } catch (Exception e) {
                        LOG.warn(e);
                    }
                    break;
            }
            mailerService.sendHtmlEmail(user.email, subject, content);
        }
        action.setFinishedAt(new Date());
        action.setStatus(Status.Finished);
    }

    public void processMailing(String docUid, String target) {
        LOG.info("mailing " + docUid + " " + target);
        String err;
        MailingAction action = new MailingAction();
        action.setCreatedAt(new Date());
        action.setStatus(Status.Started);
        action.setDocUid(docUid);
        action.setTarget(target);
        action.setError("");
        mailingRepository.persist(action);
        processMailing(action);
    }

    private void processMailing(MessageEnvelope wrapper) {
        processMailing(wrapper.message, wrapper.user.role);
    }

    private Document getDocument(String uid, String language) {
        ContentServiceClient client;
        Document document = null;
        try {
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(authHost))
                    .followRedirects(true)
                    .build(ContentServiceClient.class);
            document = client.getDocument(uid, appKey, language);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        } catch (ProcessingException ex) {
            LOG.error(ex.getMessage());
        } catch (WebApplicationException ex) {
            LOG.error(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        }
        return document;
    }

    private void processDirectEmail(MessageEnvelope wrapper) {
        LOG.debug("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    private void processWelcomeEmail(MessageEnvelope wrapper) {
        LOG.debug("WELCOME_EMAIL");
        long userNumber = wrapper.user.number;
        User user = null;
        user = authUC.getUser(userNumber);
        if (null == user) {
            LOG.error("user " + userNumber + " not found");
            return;
        }
        if (user.email.isEmpty()) {
            LOG.error("user " + userNumber + " e-mail not set");
            return;
        }
        Document docPl = getDocument(welcomeDocUid, "pl");
        Document docEn = getDocument(welcomeDocUid, "en");
        if (null == docPl && null == docEn) {
            LOG.error("document not found " + welcomeDocUid);
            return;
        }
        Document docToSend;
        String subject = "";
        String content = "";
        try {
            switch (user.preferredLanguage.toUpperCase()) {
                case "PL":
                    docToSend = docPl;
                    subject = URLDecoder.decode(docToSend.getTitle(), "UTF-8");
                    content = URLDecoder.decode(docToSend.getContent(), "UTF-8");
                    content = content.replaceFirst("\\$user.name", user.name);
                    content = content.replaceFirst("\\$mailing.name", user.surname);
                    content = content.replaceFirst("\\$user.uid", user.uid);
                    mailerService.sendEmail(user.email, subject, content);
                    break;
                // case "EN":
                default:
                    docToSend = docEn;
                    subject = URLDecoder.decode(docToSend.getTitle(), "UTF-8");
                    content = URLDecoder.decode(docToSend.getContent(), "UTF-8");
                    content = content.replaceFirst("\\$user.name", user.name);
                    content = content.replaceFirst("\\$mailing.name", user.surname);
                    content = content.replaceFirst("\\$user.uid", user.uid);
                    mailerService.sendEmail(user.email, subject, content);
                    break;
            }
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        mailerService.sendEmail(user.email, subject, content);
    }

    private User getUser(String uid) {
        User user = null;
        LOG.info("getUser " + uid);
        LOG.info("authUC " + authUC);
        user = authUC.getUser(uid);
        System.out.println(user.toString());
        return user;
    }

    protected boolean sendDeviceDefined(Device device, MessageEnvelope wrapper) {
        // project/device implementation goes here
        return false;
    }

    @Override
    public void setMailerService(MailerService service) {
        this.mailerService = service;
    }

    @Override
    public void setApplicationKey(String key) {
        this.appKey = key;

    }

    @Override
    public void setAuthHost(String authHost) {
        this.authHost = authHost;
    }

    @Override
    public void setDao(IotDatabaseIface dao) {
        this.dao = dao;
    }

}
