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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
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
import com.signomix.messaging.domain.SmsPlanetResponse;
import com.signomix.messaging.domain.Status;
import com.signomix.messaging.webhook.WebhookService;

@ApplicationScoped
public class MessageProcessorAdapter implements MessageProcessorPort {
    private static final Logger LOG = Logger.getLogger(MessageProcessorAdapter.class);

    protected MailerService mailerService;
    IotDatabaseIface dao;

/*     String appKey;
    String authHost; */

    @Inject
    AuthUC authUC;

    @Inject
    DeviceUC deviceUC;

    @RestClient
    @Inject
    SmsplanetClient smsplanetClient;

    @ConfigProperty(name = "signomix.smsplanet.key", defaultValue = "")
    String smsKey;

    @ConfigProperty(name = "signomix.smsplanet.password", defaultValue = "")
    String smsPassword;

    MailingActionRepository mailingRepository;

    public MessageProcessorAdapter() {
    }

    // @ConfigProperty(name = "signomix.admin.email", defaultValue = "")
    String adminEmail = ConfigProvider.getConfig().getValue("signomix.admin.email", String.class);
    // @ConfigProperty(name = "signomix.document.welcome.uid", defaultValue = "")
    String welcomeDocUid = ConfigProvider.getConfig().getValue("signomix.document.welcome.uid", String.class);

    @Override
    public void setMailingRepository(MailingActionRepository repository) {
        mailingRepository = repository;
    }

    /*
     * @Override
     * public void processMailing(byte[] bytes) {
     * String message = new String(bytes, StandardCharsets.UTF_8);
     * LOG.debug("MAILING " + message);
     * MessageEnvelope wrapper;
     * ObjectMapper objectMapper = new ObjectMapper();
     * try {
     * wrapper = objectMapper.readValue(message, MessageEnvelope.class);
     * } catch (JsonProcessingException ex) {
     * LOG.error(ex.getMessage());
     * return;
     * }
     * if ("NEXTMAILING".equalsIgnoreCase(wrapper.type)) {
     * processMailing(wrapper);
     * }
     * if ("MAILING".equalsIgnoreCase(wrapper.type)) {
     * mailerService.sendEmail(wrapper.user.email, wrapper.subject,
     * wrapper.message);
     * }
     * }
     */

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
            LOG.debug(message);
            MessageEnvelope wrapper;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                wrapper = objectMapper.readValue(message, MessageEnvelope.class);
            } catch (JsonProcessingException ex) {
                LOG.error(ex.getMessage());
                return;
            }
            if ("DIRECT_EMAIL".equalsIgnoreCase(wrapper.type) || "MIAILING".equalsIgnoreCase(wrapper.type)) {
                processDirectEmail(wrapper);
                return;
            }
            if ("DIRECT_MAILING".equalsIgnoreCase(wrapper.type)) {
                processWelcomeEmail(wrapper);
                return;
            }
            String address = null;
            String messageChannel = null;
            User user = getUser(wrapper.user);
            Device device = getDevice(wrapper.eui);
            if (sendDeviceDefined(device, wrapper)) {
                return;
            }
            String[] channelConfig = user.getChannelConfig(wrapper.type);
            if (channelConfig == null || channelConfig.length < 2) {
                LOG.debug("Channel not configured " + wrapper.type + " " + channelConfig.length);
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
                            if (null == wrapper.subject || wrapper.subject.isEmpty()) {
                                mailerService.sendEmail(address, wrapper.eui, wrapper.message);
                            } else {
                                mailerService.sendEmail(address, wrapper.subject, wrapper.message);
                            }
                            break;
                        case "WEBHOOK":
                            new WebhookService().send(address,
                                    new Message(wrapper.eui, wrapper.message, wrapper.subject));
                            break;
                        case "SMS":
                            if (user.credits > 0) {
                                // SmsplanetService smsService = new SmsplanetService();
                                // smsService.send(user, address, new Message(wrapper.eui, wrapper.message));
                                LOG.debug("SMSPLANET: " + " SIGNOMIX " + user.phonePrefix + address + " test "
                                        + wrapper.message);
                                SmsPlanetResponse response = smsplanetClient.sendSms(
                                        smsKey,
                                        smsPassword,
                                        "SIGNOMIX",
                                        user.phonePrefix + address,
                                        wrapper.type,
                                        wrapper.type + ": " + wrapper.message);
                                LOG.debug("SMSPLANET RESPONSE: " + response.messageId + " " + response.errorCode + " "
                                        + response.errorMsg);
                            } else {
                                // TODO: error
                                LOG.debug(messageChannel + " not sent, no credits");
                            }
                            break;

                        default:
                            LOG.warnf("Unsupported message type %1s", wrapper.type);
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
        Document docPl = getMailingDocument(docUid, "pl");
        Document docEn = getMailingDocument(docUid, "en");
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
                        content = content.replaceFirst("\\$user.surname", user.name);
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
                        content = content.replaceFirst("\\$user.surname", user.name);
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

    private Document getWelcomeDocument(String uid, String language) {
        Document doc = new Document();
        doc.setUid(uid);
        switch (language) {
            case "pl":
                doc.setTitle("Test welcome");
                doc.setContent("Test content");
                break;
            default:
                doc.setTitle("Test welcome");
                doc.setContent("Test content");
                break;
        }


        return doc;
    }

    private Document getMailingDocument(String uid, String language) {
        Document doc = new Document();
        doc.setUid(uid);
        doc.setTitle("Test mailing");
        doc.setContent("Test content");
        return doc;
    }

    public void processDirectEmail(MessageEnvelope wrapper) {
        LOG.debug("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    private void processWelcomeEmail(MessageEnvelope wrapper) {
        LOG.debug("WELCOME_EMAIL");
        long userNumber = wrapper.user.number;
        // UserServiceClient client;
        User user = null;

        // get user by number
        /*
         * try {
         * client = RestClientBuilder.newBuilder()
         * .baseUri(new URI(authHost))
         * .followRedirects(true)
         * .build(UserServiceClient.class);
         * user = client.getUserByNumber(userNumber, appKey);
         * } catch (URISyntaxException ex) {
         * LOG.error(ex.getMessage());
         * // TODO: notyfikacja użytkownika o błędzie
         * } catch (ProcessingException ex) {
         * LOG.error(ex.getMessage());
         * } catch (WebApplicationException ex) {
         * LOG.error(ex.getMessage());
         * } catch (Exception ex) {
         * LOG.error(ex.getMessage());
         * // TODO: notyfikacja użytkownika o błędzie
         * }
         */
        user = authUC.getUser(userNumber);
        if (null == user) {
            LOG.error("user " + userNumber + " not found");
            return;
        }
        if (user.email.isEmpty()) {
            LOG.error("user " + userNumber + " e-mail not set");
            return;
        }
        Document docPl = getWelcomeDocument(welcomeDocUid, "pl");
        Document docEn = getWelcomeDocument(welcomeDocUid, "en");
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
                    content = content.replaceFirst("\\$user.surname", user.surname);
                    content = content.replaceFirst("\\$user.uid", user.uid);
                    mailerService.sendEmail(user.email, subject, content);
                    break;
                // case "EN":
                default:
                    docToSend = docEn;
                    subject = URLDecoder.decode(docToSend.getTitle(), "UTF-8");
                    content = URLDecoder.decode(docToSend.getContent(), "UTF-8");
                    content = content.replaceFirst("\\$user.name", user.name);
                    content = content.replaceFirst("\\$user.surname", user.surname);
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

    /*
     * private List<User> getUsers(String role) {
     * //UserServiceClient client;
     * List<User> users;
     * try {
     * client = RestClientBuilder.newBuilder()
     * .baseUri(new URI(authHost))
     * .followRedirects(true)
     * .build(UserServiceClient.class);
     * users = client.getUsers(appKey, role);
     * return users;
     * } catch (URISyntaxException ex) {
     * LOG.error(ex.getMessage());
     * // TODO: notyfikacja użytkownika o błędzie
     * } catch (ProcessingException ex) {
     * LOG.error(ex.getMessage());
     * } catch (WebApplicationException ex) {
     * LOG.error(ex.getMessage());
     * } catch (Exception ex) {
     * LOG.error(ex.getMessage());
     * // TODO: notyfikacja użytkownika o błędzie
     * }
     * return new ArrayList<>();
     * }
     */

    private User getUser(User user) {
        String uid = user.uid;
        // UserServiceClient client;
        User completedUser = null;
        /*
         * try {
         * client = RestClientBuilder.newBuilder()
         * .baseUri(new URI(authHost))
         * .followRedirects(true)
         * .build(UserServiceClient.class);
         * completedUser = client.getUser(uid, appKey);
         * } catch (URISyntaxException ex) {
         * LOG.error(ex.getMessage());
         * // TODO: notyfikacja użytkownika o błędzie
         * } catch (ProcessingException ex) {
         * LOG.error(ex.getMessage());
         * } catch (WebApplicationException ex) {
         * LOG.error(ex.getMessage());
         * } catch (Exception ex) {
         * LOG.error(ex.getMessage());
         * // TODO: notyfikacja użytkownika o błędzie
         * }
         */
        LOG.info("getUser " + uid);
        LOG.info("authUC " + authUC);
        completedUser = authUC.getUser(uid);
        System.out.println(completedUser.toString());
        return completedUser;
    }

    private Device getDevice(String eui) {
        return deviceUC.getDevice(eui);
    }

    /*
     * private Device getDevice(String eui) {
     * //DeviceServiceClient client;
     * Device device = null;
     * try {
     * client = RestClientBuilder.newBuilder()
     * .baseUri(new URI(authHost))
     * .followRedirects(true)
     * .build(DeviceServiceClient.class);
     * device = client.getDevice(eui, appKey);
     * } catch (URISyntaxException ex) {
     * LOG.error(ex.getMessage());
     * // TODO: notyfikacja użytkownika o błędzie
     * } catch (ProcessingException ex) {
     * LOG.error(ex.getMessage());
     * } catch (WebApplicationException ex) {
     * LOG.error(ex.getMessage());
     * } catch (Exception ex) {
     * LOG.error(ex.getMessage());
     * // TODO: notyfikacja użytkownika o błędzie
     * }
     * return device;
     * }
     */

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
        //this.appKey = key;

    }

    @Override
    public void setAuthHost(String authHost) {
        //this.authHost = authHost;
    }

    @Override
    public void setDao(IotDatabaseIface dao) {
        this.dao = dao;
    }

    @Override
    public void processDataCreated(byte[] bytes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processDataCreated'");
    }

}
