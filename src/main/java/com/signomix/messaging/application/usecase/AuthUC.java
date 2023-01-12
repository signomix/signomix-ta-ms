package com.signomix.messaging.application.usecase;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.signomix.common.db.AuthDao;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.messaging.application.port.out.UserServiceClient;
import com.signomix.common.User;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class AuthUC {
    private static final Logger LOG = Logger.getLogger(AuthUC.class);
    
    @ConfigProperty(name = "signomix.app.key", defaultValue = "not_configured")
    String appKey;
    @ConfigProperty(name = "signomix.auth.host", defaultValue = "not_configured")
    String authHost;


    @Inject
    @DataSource("auth")
    AgroalDataSource dataSource;
    
    AuthDaoIface dao;

    void onStart(@Observes StartupEvent ev) {   
        dao=new AuthDao();
        dao.setDatasource(dataSource);
    }

    public User getUser(String sessionToken) {
        LOG.debug("token "+ sessionToken);
        String uid=dao.getUser(sessionToken);
        if(null==uid || uid.isEmpty()){
            LOG.error("token not found");
            return null;
        }
        UserServiceClient client;
        User completedUser = null;
        try {
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(authHost))
                    .followRedirects(true)
                    .build(UserServiceClient.class);
            completedUser = client.getUser(uid, appKey);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        } catch (ProcessingException ex) {
            LOG.error(ex.getMessage());
        } catch (WebApplicationException ex) {
            LOG.error(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        }
        return completedUser;
    }
}
