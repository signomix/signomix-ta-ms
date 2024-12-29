package com.signomix.messaging.domain.notification;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.signomix.messaging.adapter.out.MessageProcessorAdapter;

@ApplicationScoped
public class NotificationLogic {
    private static final Logger LOG = Logger.getLogger(NotificationLogic.class);

    @Inject 
    MessageProcessorAdapter messagePort;

    public void processEvent(byte[] bytes) {
        messagePort.processEvent(bytes);
    }

    public void processAdminEmail(byte[] bytes) {
        messagePort.processAdminEmail(bytes);
    }

    public void processNotification(byte[] bytes) {
        messagePort.processNotification(bytes);
    }

    public void processEmailMessage(byte[] bytes) {
        messagePort.processEmailMessage(bytes);
    }

    public void processAlert(byte[] bytes) {
        messagePort.processAlertMessage(bytes);
    }

}
