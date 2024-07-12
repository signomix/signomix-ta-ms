package com.signomix.messaging.domain;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.Token;
import com.signomix.common.User;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDaoIface;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

/**
 * Klasa zawierająca logikę biznesową dotyczącą autoryzacji.
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class AuthLogic {
    private static final Logger LOG = Logger.getLogger(AuthLogic.class);

    // TODO: move to config
    private long sessionTokenLifetime = 30; // minutes
    private long permanentTokenLifetime = 10 * 365 * 24 * 60; // 10 years in minutes

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    AuthDaoIface authDao;
    UserDaoIface userDao;

    @ConfigProperty(name = "signomix.database.type")
    String databaseType;

    @ConfigProperty(name = "questdb.client.config")
    String questDbConfig;

    void onStart(@Observes StartupEvent ev) {
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            authDao = new com.signomix.common.tsdb.AuthDao();
            authDao.setDatasource(tsDs, questDbConfig);
            userDao = new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(tsDs);
        }

    }

    public String getUserId(String token) {
        LOG.info("getUserId: " + token);
        return authDao.getUserId(token, sessionTokenLifetime, permanentTokenLifetime);
    }

    public User getUserFromToken(String token) {
        try {
            return userDao.getUser(getUserId(token));
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public User getUser(String uid) {
        User user = null;
        try {
            user = userDao.getUser(uid);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return user;
    }

    public Token getToken(String token) {
        Token tokenObj = null;
        try {
            tokenObj = authDao.getToken(token, sessionTokenLifetime, permanentTokenLifetime);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return tokenObj;
    }

}
