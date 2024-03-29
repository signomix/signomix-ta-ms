package com.signomix.messaging.adapter.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.messaging.application.usecase.MqttLogic;
import com.signomix.messaging.application.usecase.ProcessNotificationMessageUC;

@ApplicationScoped
public class MqClient {

    private static final Logger LOG = Logger.getLogger(MqClient.class);



    @Inject
    ProcessNotificationMessageUC processMessageUseCase;

    /* @Incoming("mailing")
    public void processMailing(byte[] bytes) {
        processMessageUseCase.processMailing(bytes);
    } */

    @Incoming("events")
    public void processEvent(byte[] bytes) {
        processMessageUseCase.processEvent(bytes);
    }

    @Incoming("admin_email")
    public void processAdminEmail(byte[] bytes) {
        processMessageUseCase.processAdminEmail(bytes);
    }

    @Incoming("notifications")
    public void processNotification(byte[] bytes) {
        processMessageUseCase.processNotification(bytes);
    }

    @Incoming("events_db")
    public void processDbEvent(byte[] bytes) {
        
    }
    @Incoming("events_device")
    public void processDeviceEvent(byte[] bytes) {
        
    }

}
