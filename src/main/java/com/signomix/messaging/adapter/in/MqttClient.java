package com.signomix.messaging.adapter.in;

import com.signomix.messaging.application.usecase.MqttLogic;
import com.signomix.messaging.application.usecase.ProcessNotificationMessageUC;
import com.signomix.messaging.domain.deviceCommands.CommandSendingLogic;
import com.signomix.messaging.domain.order.OrderLogic;
import com.signomix.messaging.domain.user.UserLogic;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Inject
    MqttLogic mqttLogic;

    @Inject
    UserLogic userLogic;

    @Inject
    OrderLogic orderLogic;

    @Inject
    CommandSendingLogic commandSendingLogic;

    @Inject
    ProcessNotificationMessageUC processMessageUseCase;

    @Incoming("alerts")
    public void processAlert(byte[] bytes) {
        try {
            logger.info("Alert received: " + new String(bytes));
            mqttLogic.processMqttAlerts(bytes);
        } catch (Exception e) {
            logger.error("Error processing alert: " + e.getMessage());
        }
    }

    @Incoming("user-events")
    public void processUserEvent(byte[] bytes) {
        try {
            logger.info("User event received");
            logger.info("User event received: " + new String(bytes));
            String msg = new String(bytes);
            userLogic.processUserEvent(msg);
        } catch (Exception e) {
            logger.error("Error processing user event: " + e.getMessage());
        }
    }

    @Incoming("data-created")
    public void processDataCreated(byte[] bytes) {
        try {
            logger.info("Data created event received: " + new String(bytes));
            mqttLogic.processDataCreated(bytes);
        } catch (Exception e) {
            logger.error("Error processing data created event: " + e.getMessage());
        }
    }

    @Incoming("order")
    public void processOrderEvent(byte[] bytes) {
        try {
            logger.info("Order event received: " + new String(bytes));
            String msg = new String(bytes);
            orderLogic.processOrderEvent(msg);
        } catch (Exception e) {
            logger.error("Error processing order event: " + e.getMessage());
        }
    }

    @Incoming("device-commands")
    public void processSendDeviceCommands(byte[] bytes) {
        try {
            logger.info("Send device commands event: " + new String(bytes));
            commandSendingLogic.sendWaitingCommands(bytes);
        } catch (Exception e) {
            logger.error("Error processing device-commands event: " + e.getMessage());
        }
    }

    @Incoming("notifications")
    public void processNotification(byte[] bytes) {
        try {
            processMessageUseCase.processNotification(bytes);
        } catch (Exception e) {
            logger.error("Error processing notification: " + e.getMessage());
        }
    }

    @Incoming("adminemail")
    public void processAdminEmail(byte[] bytes) {
        try {
            logger.debug("Admin email received (MQTT): " + new String(bytes));
            processMessageUseCase.processAdminEmail(bytes);
        } catch (Exception e) {
            logger.error("Error processing admin email: " + e.getMessage());
        }
    }

}
