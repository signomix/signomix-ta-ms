package com.signomix.messaging.application.usecase;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.db.IotDatabaseDao;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.adapter.out.MailerService;
import com.signomix.messaging.adapter.out.MailingActionRepository;
import com.signomix.messaging.adapter.out.MqttProcessorAdapter;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;


@ApplicationScoped
public class MqttLogic {
    private static final Logger LOG = Logger.getLogger(MailingUC.class);

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


    @Inject MqttProcessorAdapter messageAdapter = null;

    void onStart(@Observes StartupEvent ev) {
        dao = new IotDatabaseDao();
        dao.setDatasource(dataSource);
        try {
            messageAdapter.setMailerService(mailerService);
            messageAdapter.setApplicationKey(appKey);
            messageAdapter.setAuthHost(authHost);
            messageAdapter.setDao(dao);
            messageAdapter.setMailingRepository(mailingRepository);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return;
        }
    }

    public void processMqttAlerts(byte[] bytes) {
        messageAdapter.processNotification(bytes);
    }

}
