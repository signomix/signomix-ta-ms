package com.signomix.messaging.adapter.out;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.EventEnvelope;
import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.hcms.Document;
import com.signomix.common.iot.Device;
import com.signomix.messaging.application.port.out.MessageProcessorIface;
import com.signomix.messaging.domain.AuthLogic;
import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.Message;
import com.signomix.messaging.domain.SmsPlanetResponse;
import com.signomix.messaging.domain.Status;
import com.signomix.messaging.domain.device.DeviceLogic;
import com.signomix.messaging.domain.user.UserLogic;
import com.signomix.messaging.webhook.WebhookService;
import com.signomix.proprietary.ExtensionPoints;

@ApplicationScoped
public class MessageProcessorAdapter implements MessageProcessorIface {
    private static final Logger LOG = Logger.getLogger(MessageProcessorAdapter.class);

    protected MailerService mailerService;
    IotDatabaseIface dao;

    /*
     * String appKey;
     * String authHost;
     */

    @Inject
    AuthLogic authUC;

    @Inject
    DeviceLogic deviceUC;

    @Inject
    UserLogic userLogic;

    // @RestClient
    // @Inject
    // SmsplanetClient smsplanetClient;

    @Inject
    SmsplanetService smsService;

    // @ConfigProperty(name = "signomix.smsplanet.key", defaultValue = "")
    // String smsKey;

    // @ConfigProperty(name = "signomix.smsplanet.password", defaultValue = "")
    // String smsPassword;

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
    public void processAlertMessage(byte[] bytes) {
        try {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOG.info("Received: " + message); // "userId\teui\tmessageType\tmessageText"
            String[] params = message.split("\t");
            String userId = params[0];
            String deviceEui = params[1];
            String messageType = params[2];
            String messageText = params[3];
            String messageSubject = "";
            if (params.length > 4) {
                messageSubject = params[4];
            }

            String address = null;
            String messageChannel = null;
            User user = getUser(userId);
            if (null == user) {
                LOG.warn("user not found " + userId);
                return;
            }
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
                            if (null == messageSubject || messageSubject.isEmpty()) {
                                messageSubject = "Signomix notification";
                            }
                            mailerService.sendEmail(address, messageSubject, messageText, null, null);
                            break;
                        case "WEBHOOK":
                            LOG.info("sending with WEBHOOK");
                            new WebhookService().send(address, new Message(deviceEui, messageText, messageSubject));
                            break;
                        case "SMS":
                            // check user credits if required
                            if (ExtensionPoints.isControlled() && userLogic.getServicePoints(user.uid) <= 0) {
                                LOG.warn("User has no credits: " + user.uid);
                                return;
                            }
                            if (address == null || address.trim().isEmpty()) {
                                LOG.warn("SMS address is empty; " + user.uid);
                                return;
                            }
                            String phoneNumber;
                            if (user.phonePrefix != null && !user.phonePrefix.isEmpty()) {
                                phoneNumber = user.phonePrefix + address;
                            } else {
                                phoneNumber = address;
                            }
                            smsService.send(
                                    phoneNumber,
                                    new Message(userId, messageText, messageSubject),
                                    false);
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

    public void processAdminEmail(MessageEnvelope wrapper) {
        String emailAddress = wrapper.user.email;
        if (null == emailAddress || emailAddress.isEmpty()) {
            emailAddress = adminEmail;
            LOG.warn("email not set, usinng admin email from application properties");
            if (null == emailAddress || emailAddress.isEmpty()) {
                LOG.error("application property signomix.admin.email not set");
                return;
            }
        }
        mailerService.sendEmail(emailAddress, wrapper.subject, wrapper.message, null, null);
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
        mailerService.sendEmail(emailAddress, wrapper.subject, wrapper.message, null, null);
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

            // otherwise
            processNotification(wrapper);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void processEmailMessage(byte[] bytes) {
        try {
            int index0, index1, index2;
            String message = new String(bytes, StandardCharsets.UTF_8);
            index0 = message.indexOf("\n"); // email
            index1 = message.indexOf("\n", index0 + 1); // subject
            index2 = message.indexOf("\n", index1 + 1); // attachment file name
            String email=message.substring(0, index0);
            String subject=message.substring(index0+1, index1);
            String fileName = message.substring(index1+1, index2);
            String content=message.substring(index2+1);
            processDirectEmail(email, subject, content, fileName);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void processNotification(MessageEnvelope wrapper) {
        String address = null;
        String messageChannel = null;
        User user = getUser(wrapper.user);
        Device device = getDevice(wrapper.eui);

        if (sendDeviceDefined(device, wrapper)) {
            // sendDeviceDefined(device, wrapper) is not implemented yet
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
                            mailerService.sendEmail(address, wrapper.eui, wrapper.message, null, null);
                        } else {
                            mailerService.sendEmail(address, wrapper.subject, wrapper.message, null, null);
                        }
                        break;
                    case "WEBHOOK":
                        new WebhookService().send(address,
                                new Message(wrapper.eui, wrapper.message, wrapper.subject));
                        break;
                    case "SMS":
                        if (ExtensionPoints.isControlled() && userLogic.getServicePoints(user.uid) <= 0) {
                            LOG.warn("User has no credits: " + user.uid);
                            return;
                        }
                        if (address == null || address.trim().isEmpty()) {
                            LOG.warn("SMS address is empty; " + user.uid);
                            return;
                        }
                        String phoneNumber;
                        if (user.phonePrefix != null && !user.phonePrefix.isEmpty()) {
                            phoneNumber = user.phonePrefix.trim() + address;
                        } else {
                            phoneNumber = address;
                        }
                        SmsPlanetResponse response = smsService.send(
                                phoneNumber,
                                new Message(user.uid, wrapper.type + ": " + wrapper.message, wrapper.type),
                                false);
                        break;

                    default:
                        LOG.warnf("Unsupported message type %1s", wrapper.type);
                }
            }
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
        subjectPL = docPl.metadata.get("title");
        contentPL = docPl.content;
        subjectEN = docEn.metadata.get("title");
        contentEN = docEn.content;
        /*
         * try {
         * subjectPL = URLDecoder.decode(docPl.getTitle(), "UTF-8");
         * contentPL = URLDecoder.decode(docPl.getContent(), "UTF-8");
         * subjectEN = URLDecoder.decode(docEn.getTitle(), "UTF-8");
         * contentEN = URLDecoder.decode(docEn.getContent(), "UTF-8");
         * } catch (UnsupportedEncodingException ex) {
         * LOG.error(ex.getMessage());
         * action.setStatus(Status.Failed);
         * action.setError(ex.getMessage());
         * // save?
         * return;
         * }
         */
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
            mailerService.sendHtmlEmail(user.email, subject, content, null, null);
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
        doc.metadata.put("title", "Test welcome");
        doc.content = "Test content";
        return doc;
    }

    private Document getMailingDocument(String uid, String language) {
        Document doc = new Document();
        doc.metadata.put("title", "Test mai  ling");
        doc.content = "Test content";
        return doc;
    }

    public void processDirectEmail(MessageEnvelope wrapper) {
        LOG.debug("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message, null, null);
    }

    private void processDirectEmail(String email, String subject, String message, String fileName) {
        LOG.debug("DIRECT_EMAIL");
        mailerService.sendEmail(email, subject, message, null, fileName);
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
        switch (user.preferredLanguage.toUpperCase()) {
            case "PL":
                docToSend = docPl;
                break;
            // case "EN":
            default:
                docToSend = docEn;
                break;
        }
        subject = docToSend.metadata.get("title");
        content = docToSend.content;
        content = content.replaceFirst("\\$user.name", user.name);
        content = content.replaceFirst("\\$user.surname", user.surname);
        content = content.replaceFirst("\\$user.uid", user.uid);
        mailerService.sendEmail(user.email, subject, content, null, null);
        mailerService.sendEmail(user.email, subject, content, null, null); ;
    }

    private User getUser(User user) {
        String uid = user.uid;
        return getUser(uid);
    }

    private User getUser(String uid) {
        User completedUser = null;
        LOG.info("getUser " + uid);
        LOG.info("authUC " + authUC);
        completedUser = authUC.getUser(uid);
        // System.out.println(completedUser.toString());
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
        // this.appKey = key;

    }

    @Override
    public void setAuthHost(String authHost) {
        // this.authHost = authHost;
    }

    @Override
    public void setDao(IotDatabaseIface dao) {
        this.dao = dao;
    }

    @Override
    public void processDataCreated(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.debug("DATA_CREATED2: " + message);
    }

}
