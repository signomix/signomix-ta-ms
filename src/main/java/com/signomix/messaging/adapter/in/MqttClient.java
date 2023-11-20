package com.signomix.messaging.adapter.in;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.messaging.application.usecase.MqttLogic;

public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Inject
    MqttLogic mqttLogic;

    @Incoming("alerts")
    public void processNotification(byte[] bytes) {
        logger.info("Alert received: "+new String(bytes));
        mqttLogic.processMqttAlerts(bytes);
    }
    
}
