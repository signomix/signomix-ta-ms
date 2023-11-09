package com.signomix.messaging.adapter.in;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

public class MqttClient {

    @Inject
    Logger logger = Logger.getLogger(MqttClient.class);

    @Incoming("data-received")
    public void receive(byte[] eui) {
        logger.info("Data received: "+new String(eui));
    }
    
}
