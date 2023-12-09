package com.signomix.messaging.adapter.in;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.messaging.application.usecase.MqttLogic;
import com.signomix.messaging.domain.user.UserLogic;

public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Inject
    MqttLogic mqttLogic;

    @Inject
    UserLogic userLogic;

    @Incoming("alerts")
    public void processNotification(byte[] bytes) {
        logger.info("Alert received: "+new String(bytes));
        mqttLogic.processMqttAlerts(bytes);
    }

    @Incoming("user")
    public void processUserEvent(byte[] bytes) {
        logger.info("User event received: "+new String(bytes));
        String msg = new String(bytes);
        userLogic.processUserEvent(msg);
    }
    
}
