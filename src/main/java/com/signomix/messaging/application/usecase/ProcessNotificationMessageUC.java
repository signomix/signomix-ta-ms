package com.signomix.messaging.application.usecase;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.messaging.application.port.out.MessageProcessorPort;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ProcessNotificationMessageUC {
    private static final Logger LOG = Logger.getLogger(ProcessNotificationMessageUC.class);

    @ConfigProperty(name = "messaging.notification.usecase.class")
    String usecaseClassName;

    private MessageProcessorPort messageAdapter=null;

    void onStart(@Observes StartupEvent ev) {   
        // If there are several adapters to choose from, I can decide which one to use.
        try {
            messageAdapter=(MessageProcessorPort)Class.forName(usecaseClassName).getConstructor(String.class).newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOG.error(e.getMessage());
            return;
        }
    }

    public void processMailing(byte[] bytes){
        messageAdapter.processMailing(bytes);
    }
    public void processEvent(byte[] bytes){
        messageAdapter.processEvent(bytes);
    }
    public void processAdminEmail(byte[] bytes){
        messageAdapter.processAdminEmail(bytes);
    }
    public void processNotification(byte[] bytes){
        messageAdapter.processNotification(bytes);
    }

}
