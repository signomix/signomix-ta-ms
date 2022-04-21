package com.signomix.messaging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.signomix.messaging.discord.DiscordService;
import com.signomix.messaging.dto.DiscordMessage;
import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.MessageWrapper;
import com.signomix.messaging.email.MailerService;
import com.signomix.messaging.pushover.PushoverService;
import com.signomix.messaging.slack.SlackService;
import com.signomix.messaging.telegram.TelegramService;
import com.signomix.messaging.webhook.WebhookService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class MessageProcessor {

    private static final Logger LOG = Logger.getLogger(MessageProcessor.class);

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    MailerService mailerService;

    @Inject
    TelegramService telegramService;

    @Inject
    SlackService slackService;

    @Inject
    PushoverService pushoverService;

    @ConfigProperty(name = "signomix.queue.notifications")
    String notificationsQueue;

    @ConfigProperty(name = "signomix.queue.mailing")
    String mailingQueue;

    @ConfigProperty(name = "signomix.queue.admin_email")
    String adminEmailQueue;

    String eventsQueue="events";

    private Channel channel;

    public void onApplicationStart(@Observes StartupEvent event) {
        // on application start prepare the queus and message listener
        setupQueues();
        setupReceiving();
    }

    private void setupQueues() {
        try {
            Connection connection = rabbitMQClient.connect();
            channel = connection.createChannel();
            channel.queueDeclare(notificationsQueue, true, false, false, new HashMap<>());
            channel.queueDeclare(mailingQueue, true, false, false, new HashMap<>());
            channel.queueDeclare(adminEmailQueue, true, false, false, new HashMap<>());
            
            channel.exchangeDeclare("events", "fanout");
            channel.queueDeclare("messaging", true, false, false, null);
            channel.queueBind("messaging", "events", "");

        } catch (IOException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            throw new UncheckedIOException(e);
        }
    }

    private void setupReceiving() {
        try {
            channel.basicConsume(notificationsQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // just print the received message.
                    processNotification(new String(body, StandardCharsets.UTF_8));
                }
            });
            channel.basicConsume(mailingQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // just print the received message.
                    processMailing(new String(body, StandardCharsets.UTF_8));
                }
            });
            channel.basicConsume(adminEmailQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // just print the received message.
                    processAdminEmail(new String(body, StandardCharsets.UTF_8));
                }
            });
            channel.basicConsume("messaging", true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // just print the received message.
                    processEvent(new String(body, StandardCharsets.UTF_8));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            throw new UncheckedIOException(e);
        }
    }

    private void processNotification(String message) {
        LOG.info(message);
        MessageWrapper wrapper;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wrapper = objectMapper.readValue(message, MessageWrapper.class);
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        if ("DIRECT_EMAIL".equalsIgnoreCase(wrapper.type)||"MIAILING".equalsIgnoreCase(wrapper.type)) {
            processDirectEmail(wrapper);
            return;
        }
        String address = null;
        String messageChannel = null;
        String[] channelConfig = wrapper.user.getChannelConfig(wrapper.type);
        if (channelConfig == null || channelConfig.length < 2) {
            LOG.info("Channel not configured " + wrapper.type + " " + channelConfig.length);
        } else {
            LOG.info(channelConfig[0] + " " + channelConfig[1]);
            messageChannel = channelConfig[0];
            if (channelConfig.length == 2) {
                address = channelConfig[1];
            } else {
                //in case when address has ':'
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
                /*case "SMS":
                if (user.getCredits() > 0) {
                    response = smsNotification.send(user.getUid(), user.getPhonePrefix() + address, nodeName, message);
                }
                if (!response.startsWith("ERROR")) {
                    //TODO: decrease user credits
                }
                break;
                 */
                default:
                    LOG.warnf("Unsupported message type %1s", wrapper.type);
            }
        }
    }

    private void processMailing(String message) {
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

    private void processAdminEmail(String message) {
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

    private void processDirectEmail(MessageWrapper wrapper) {
        LOG.info("DIRECT_EMAIL");
        mailerService.sendEmail(wrapper.user.email, wrapper.subject, wrapper.message);
    }

    private void processEvent(String message) {
        LOG.info("EVENT: "+message);
    }

}
