package com.signomix.messaging.application.usecase;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.adapter.out.MailerService;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;
import com.signomix.messaging.adapter.out.MqttProcessorAdapter;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ProcessNotificationMessageUC {
    private static final Logger LOG = Logger.getLogger(ProcessNotificationMessageUC.class);

    @Inject
    @DataSource("oltp")
    AgroalDataSource ds;

    IotDatabaseIface dao;

    @Inject
    MailerService mailerService;

    // @Inject
    // TelegramService telegramService;

    // @Inject
    // SlackService slackService;

    // @Inject
    // PushoverService pushoverService;

/*     @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost; */

    @ConfigProperty(name = "messaging.processor.class")
    String usecaseClassName;

    @Inject 
    MessageProcessorAdapter messagePort;
    @Inject
    MqttProcessorAdapter mqttPort;

    void onStart(@Observes StartupEvent ev) {
        dao = new IotDatabaseDao();
        dao.setDatasource(ds);
        // If there are several adapters to choose from, I can decide which one to use.
        /* if (null == usecaseClassName || usecaseClassName.isEmpty()) {
            messagePort = new MessageProcessorAdapter();
        } else {
            try {
                LOG.info("messagingProcessorClassName:" + usecaseClassName);
                messagePort = (MessageProcessorPort) Class.forName(usecaseClassName).getDeclaredConstructor()
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
                LOG.error(e.getMessage());
                return;
            } catch (Exception e) {
                LOG.error(e.getMessage());
                return;
            }
            messagePort.setMailerService(mailerService);
            messagePort.setApplicationKey(appKey);
            messagePort.setAuthHost(authHost);
            messagePort.setDao(dao);
        } */
    }

    /* public void processMailing(byte[] bytes) {
        messagePort.processMailing(bytes);
    } */

    public void processEvent(byte[] bytes) {
        messagePort.processEvent(bytes);
    }

    public void processAdminEmail(byte[] bytes) {
        messagePort.processAdminEmail(bytes);
    }

    public void processNotification(byte[] bytes) {
        messagePort.processNotification(bytes);
    }

}
