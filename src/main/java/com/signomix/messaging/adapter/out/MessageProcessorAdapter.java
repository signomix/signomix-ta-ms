package com.signomix.messaging.adapter.out;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.cricketmsf.microsite.cms.Document;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.messaging.application.port.out.ContentServiceClient;
import com.signomix.messaging.application.port.out.DeviceServiceClient;
import com.signomix.messaging.application.port.out.MessageProcessorPort;
import com.signomix.messaging.application.port.out.UserServiceClient;
import com.signomix.messaging.dto.EventWrapper;
import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.MessageWrapper;
import com.signomix.messaging.dto.User;
import com.signomix.messaging.email.MailerService;
import com.signomix.messaging.pushover.PushoverService;
import com.signomix.messaging.slack.SlackService;
import com.signomix.messaging.telegram.TelegramService;
import com.signomix.messaging.webhook.WebhookService;

public class MessageProcessorAdapter implements MessageProcessorPort {
    private static final Logger LOG = Logger.getLogger(MessageProcessorAdapter.class);

    MailerService mailerService;
    TelegramService telegramService;
    SlackService slackService;
    PushoverService pushoverService;
    IotDatabaseIface dao;

    String appKey;
    String authHost;

    public MessageProcessorAdapter() {
    }

    @Override
    public void processMailing(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.info("MAILING " + message);
        MessageWrapper wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, MessageWrapper.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        if ("NEXTMAILING".equalsIgnoreCase(wrapper.type)) {
            processMailing(wrapper);
        }
        if ("MAILING".equalsIgnoreCase(wrapper.type)) {
            mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
        }
    }

    @Override
    public void processEvent(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.info("EVENT: " + message);
        EventWrapper wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, EventWrapper.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        LOG.info(wrapper.type + " " + wrapper.id + " " + wrapper.payload);
    }

    @Override
    public void processAdminEmail(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.info("ADMIN_EMAIL " + message);
        MessageWrapper wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, MessageWrapper.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    @Override
    public void processNotification(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.info(message);
        MessageWrapper wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, MessageWrapper.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        if ("DIRECT_EMAIL".equalsIgnoreCase(wrapper.type) || "MIAILING".equalsIgnoreCase(wrapper.type)) {
            processDirectEmail(wrapper);
            return;
        }
        String address = null;
        String messageChannel = null;
        User user = getUser(wrapper.user);
        Device device = getDevice(wrapper.eui);
        sendDeviceDefined(device, wrapper);
        String[] channelConfig = user.getChannelConfig(wrapper.type);
        if (channelConfig == null || channelConfig.length < 2) {
            LOG.info("Channel not configured " + wrapper.type + " " + channelConfig.length);
        } else {
            LOG.info(channelConfig[0] + " " + channelConfig[1]);
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

            switch (messageChannel.toUpperCase()) {
                case "SMTP":
                    if (null != address && !address.isEmpty()) {
                        // mailerService.send(address, wrapper.eui, wrapper.message);
                        mailerService.sendEmail(address, wrapper.eui, wrapper.message);
                    }
                    break;
                case "WEBHOOK":
                    wrapper.user = null;
                    if (null != address && !address.isEmpty()) {
                        new WebhookService().send(address, new Message(wrapper.eui, wrapper.message));
                    }
                    break;
                case "SMS":
                    /*
                    if (user.getCredits() > 0) {
                        response = smsNotification.send(user.getUid(), user.getPhonePrefix() +
                        address, nodeName, message);
                    }
                    if (!response.startsWith("ERROR")) {
                        // TODO: decrease user credits
                    }
                    */
                    break;

                default:
                    LOG.warnf("Unsupported message type %1s", wrapper.type);
            }
        }
    }

    public void processMailing(String docUid, String target) {
        Document docPl = getDocument(docUid, "pl");
        Document docEn = getDocument(docUid, "en");
        if (null == docPl && null == docEn) {
            LOG.error("document not found " + docUid);
        }
        User user;
        Document docToSend;
        String subject;
        String content;
        List<User> users = getUsers(target);
        try {
            for (int i = 0; i < users.size(); i++) {
                user = users.get(i);
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
            }
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex.getMessage());
        }
    }

    private void processMailing(MessageWrapper wrapper) {
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

    private void processDirectEmail(MessageWrapper wrapper) {
        LOG.info("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    private List<User> getUsers(String role) {
        UserServiceClient client;
        List<User> users;
        try {
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(authHost))
                    .followRedirects(true)
                    .build(UserServiceClient.class);
            users = client.getUsers(appKey, role);
            return users;
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
        return new ArrayList<>();
    }

    private User getUser(User user) {
        String uid = user.uid;
        UserServiceClient client;
        User completedUser = null;
        try {
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(authHost))
                    .followRedirects(true)
                    .build(UserServiceClient.class);
            completedUser = client.getUser(uid, appKey);
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
        System.out.println(completedUser.toString());
        return completedUser;
    }

    private Device getDevice(String eui) {
        DeviceServiceClient client;
        Device device = null;
        try {
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(authHost))
                    .followRedirects(true)
                    .build(DeviceServiceClient.class);
            device = client.getDevice(eui, appKey);
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
        return device;
    }

    private void sendDeviceDefined(Device device, MessageWrapper wrapper) {
        /*
        if (null == device) {
            LOG.error("device not found");
            return;
        } else if (null == device.getConfiguration()) {
            LOG.error("device not configured");
        }
        GuardianConfig config = JsonHelper.deserializeGuardianConfig(device.getConfiguration());
        String[] emails = config.email.split(",");
        for (int i = 0; i < emails.length; i++) {
            if (!emails[i].trim().isEmpty()) {
                mailerService.sendEmail(emails[i].trim(), wrapper.eui, wrapper.message);
            }
        }
        */
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
