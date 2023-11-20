package com.signomix.messaging.application.usecase;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.adapter.out.MailerService;
import com.signomix.messaging.adapter.out.MailingActionRepository;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;
import com.signomix.messaging.application.port.out.MessageProcessorPort;
import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.Status;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.NonBlocking;


@ApplicationScoped
public class MailingUC {
    private static final Logger LOG = Logger.getLogger(MqttLogic.class);

    @Inject
    @DataSource("iot")
    AgroalDataSource dataSource;

    IotDatabaseIface dao;
    
    @Inject
    MailerService mailerService;

    @Inject
    MailingActionRepository mailingRepository;

    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;

    // @ConfigProperty(name = "messaging.processor.class")
    //String usecaseClassName;

    @Inject MessageProcessorAdapter messageAdapter = null;

    void onStart(@Observes StartupEvent ev) {
        dao = new IotDatabaseDao();
        dao.setDatasource(dataSource);
        // If there are several adapters to choose from, I can decide which one to use.
        try {
            //LOG.info("messagingProcessorClassName:" + usecaseClassName);
            //messageAdapter = new MessageProcessorAdapter();
            // messageAdapter=(MessageProcessorPort)Class.forName(usecaseClassName).getDeclaredConstructor().newInstance();
            messageAdapter.setMailerService(mailerService);
            messageAdapter.setApplicationKey(appKey);
            messageAdapter.setAuthHost(authHost);
            messageAdapter.setDao(dao);
            messageAdapter.setMailingRepository(mailingRepository);
            // } catch (InstantiationException | IllegalAccessException |
            // IllegalArgumentException | InvocationTargetException
            // | NoSuchMethodException | SecurityException |
            // ClassNotFoundException|NullPointerException e) {
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return;
        }
    }

    @ConsumeEvent("mailing")
    @NonBlocking
    public void processMailingEvent(String message) {
        String[] params= message.split("\t");
        //messageAdapter.processMailing(params[1], params[0]);
        System.out.println("hello");
    }

    @Transactional
    public void processMailing(String docUid, String target) {
        messageAdapter.processMailing(docUid, target);
    }

    public void addPlannedMailing(String docUid, String target) {
        MailingAction mailingAction=new MailingAction();
        mailingAction.setDocUid(docUid);
        mailingAction.setTarget(target);
        mailingAction.setCreatedAt(new Date());
        mailingAction.setPlannedAt(new Date());
        mailingAction.setStatus(Status.Planned);
        mailingRepository.persist(mailingAction);
    }

    @Transactional
    public void runMailingAction(long actionId){
        MailingAction mailingAction=mailingRepository.findById(actionId);
        messageAdapter.processMailing(mailingAction);
    }

    /* public void processMailing(byte[] bytes) {
        messageAdapter.processMailing(bytes);
    } */

    public void processEvent(byte[] bytes) {
        messageAdapter.processEvent(bytes);
    }

    public void processAdminEmail(byte[] bytes) {
        messageAdapter.processAdminEmail(bytes);
    }

    public void processNotification(byte[] bytes) {
        messageAdapter.processNotification(bytes);
    }

}
