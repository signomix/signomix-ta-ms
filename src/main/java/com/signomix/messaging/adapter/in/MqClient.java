package com.signomix.messaging.adapter.in;

import com.signomix.messaging.application.usecase.ProcessNotificationMessageUC;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MqClient {

    private static final Logger LOG = Logger.getLogger(MqClient.class);

    @Inject
    ProcessNotificationMessageUC processMessageUseCase;

    /*
     * @Incoming("mailing")
     * public void processMailing(byte[] bytes) {
     * processMessageUseCase.processMailing(bytes);
     * }
     */

    @Incoming("events")
    public void processEvent(byte[] bytes) {
        try {
            processMessageUseCase.processEvent(bytes);
        } catch (Exception e) {
            LOG.error("Error processing event: " + e.getMessage());
        }
    }

    @Incoming("admin_email")
    public void processAdminEmail(byte[] bytes) {
        try {
            LOG.info("Admin email received");
            processMessageUseCase.processAdminEmail(bytes);
        } catch (Exception e) {
            LOG.error("Error processing admin email: " + e.getMessage());
        }
    }

    /* @Incoming("notifications")
    public void processNotification(byte[] bytes) {
        try {
            processMessageUseCase.processNotification(bytes);
        } catch (Exception e) {
            LOG.error("Error processing notification: " + e.getMessage());
        }
    } */

    // @Incoming("events_db")
    // public void processDbEvent(byte[] bytes) {

    // }

    // @Incoming("events_device")
    // public void processDeviceEvent(byte[] bytes) {

    // }

}
