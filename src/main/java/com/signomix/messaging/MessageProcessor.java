package com.signomix.messaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.messaging.discord.DiscordService;
import com.signomix.messaging.dto.DiscordMessage;
import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.MessageWrapper;
import com.signomix.messaging.dto.User;
import com.signomix.messaging.email.MailerService;
import com.signomix.messaging.pushover.PushoverService;
import com.signomix.messaging.slack.SlackService;
import com.signomix.messaging.telegram.TelegramService;
import com.signomix.messaging.webhook.WebhookService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageProcessor {

    private static final Logger LOG = Logger.getLogger(MessageProcessor.class);

    @Inject
    MailerService mailerService;

    @Inject
    TelegramService telegramService;

    @Inject
    SlackService slackService;

    @Inject
    PushoverService pushoverService;

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;

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
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    @Incoming("events")
    public void processEvent(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.info("EVENT: " + message);
    }

    @Incoming("admin_email")
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

    @Incoming("notifications")
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
                        mailerService.send(address, wrapper.eui, wrapper.message);
                    }
                    break;
                case "WEBHOOK":
                    wrapper.user = null;
                    if (null != address && !address.isEmpty()) {
                        new WebhookService().send(address, new Message(wrapper.eui, wrapper.message));
                    }
                    break;
                case "DISCORD":
                    if (null != address && !address.isEmpty()) {
                        new DiscordService().send(address, new DiscordMessage(wrapper.eui, wrapper.message));
                    }
                    break;
                case "PUSHOVER":
                    if (null != address && !address.isEmpty()) {
                        pushoverService.send(address, new Message(wrapper.eui, wrapper.message));
                    }
                    break;
                case "SLACK":
                    if (null != address && !address.isEmpty()) {
                        slackService.send(address, new Message(wrapper.eui, wrapper.message));
                    }
                    break;
                case "TELEGRAM":
                    if (null != address && !address.isEmpty()) {
                        telegramService.send(address, new Message(wrapper.eui, wrapper.message));
                    }
                    break;
                /*
                 * case "SMS":
                 * if (user.getCredits() > 0) {
                 * response = smsNotification.send(user.getUid(), user.getPhonePrefix() +
                 * address, nodeName, message);
                 * }
                 * if (!response.startsWith("ERROR")) {
                 * //TODO: decrease user credits
                 * }
                 * break;
                 */
                default:
                    LOG.warnf("Unsupported message type %1s", wrapper.type);
            }
        }
    }

    private void processDirectEmail(MessageWrapper wrapper) {
        LOG.info("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    private User getUser(User user) {
        if(null!=user.email && !user.email.isEmpty()){
            return user;
        }
        String uid=user.uid;
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

}
