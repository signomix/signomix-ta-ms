package com.signomix.messaging.adapter.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.messaging.application.usecase.MqttLogic;
import com.signomix.messaging.domain.order.OrderLogic;
import com.signomix.messaging.domain.user.UserLogic;

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

    @Incoming("data-created")
    public void processDataCreated(byte[] bytes) {
        logger.info("Data created event received: "+new String(bytes));
        mqttLogic.processDataCreated(bytes);
    }

    @Incoming("order")
    public void processOrderEvent(byte[] bytes) {
        logger.info("Order event received: "+new String(bytes));
        String msg = new String(bytes);
        orderLogic.processOrderEvent(msg);
    }
    
}
