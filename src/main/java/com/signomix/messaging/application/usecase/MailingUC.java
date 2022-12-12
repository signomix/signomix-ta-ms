package com.signomix.messaging.application.usecase;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.adapter.out.SmtpAdapter;
import com.signomix.messaging.application.port.out.MessageProcessorPort;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class MailingUC {
    private static final Logger LOG = Logger.getLogger(MailingUC.class);

    @Inject
    @DataSource("iot")
    AgroalDataSource dataSource;

    IotDatabaseIface dao;
    
    @Inject
    SmtpAdapter mailerService;

    //@Inject
    //TelegramService telegramService;

    //@Inject
    //SlackService slackService;

    //@Inject
    //PushoverService pushoverService;

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;

    @ConfigProperty(name = "messaging.processor.class")
    String usecaseClassName;

    private MessageProcessorPort messageAdapter=null;

    void onStart(@Observes StartupEvent ev) {   
        dao = new IotDatabaseDao();
        dao.setDatasource(dataSource);
        // If there are several adapters to choose from, I can decide which one to use.
        try {
            LOG.info("messagingProcessorClassName:"+usecaseClassName);
            messageAdapter=(MessageProcessorPort)Class.forName(usecaseClassName).getDeclaredConstructor().newInstance();
            messageAdapter.setMailerService(mailerService);
            messageAdapter.setApplicationKey(appKey);
            messageAdapter.setAuthHost(authHost);
            messageAdapter.setDao(dao);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException|NullPointerException e) {
            LOG.error(e.getMessage());
            return;
        }
    }

    public void processMailing(String docUid, String target){
        messageAdapter.processMailing(docUid, target);
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
