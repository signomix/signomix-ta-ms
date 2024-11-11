package com.signomix.messaging.application.usecase;

import com.signomix.common.Token;
import com.signomix.common.User;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDaoIface;
import com.signomix.common.tsdb.AuthDao;
import com.signomix.common.tsdb.UserDao;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthUC {
    private static final Logger LOG = Logger.getLogger(AuthUC.class);

    //TODO: move to config
    private long sessionTokenLifetime = 30; // minutes
    private long permanentTokenLifetime = 10 * 365 * 24 * 60; // 10 years in minutes


    @Inject
    @DataSource("auth")
    AgroalDataSource dataSource;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;
    
    AuthDaoIface dao;
    UserDaoIface userDao;

    @ConfigProperty(name = "questdb.client.config")
    String questDbConfig;

    void onStart(@Observes StartupEvent ev) {   
        dao=new AuthDao();
        dao.setDatasource(dataSource, questDbConfig);
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
    }

    public User getUser(String uid){
        try{
            return userDao.getUser(uid);
        }catch(IotDatabaseException ex){
            LOG.error(ex.getMessage());
            return null;
        }
    }

    public User getUserForToken(String sessionToken) {
        LOG.debug("token "+ sessionToken);
        Token token=dao.getToken(sessionToken, sessionTokenLifetime,permanentTokenLifetime);
        if(null==token){
            LOG.error("token not found");
            return null;
        }
        String uid=token.getUid();
        if(null==uid || uid.isEmpty()){
            LOG.error("token not found");
            return null;
        }
        User user=null;
        try {
            user = userDao.getUser(uid);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
        if(null==user){
            LOG.error("user not found");
        }
        return user;
    }

    public User getUser(long id){
        User user=null;
        try {
            user = userDao.getUser(id);
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
        if(null==user){
            LOG.error("user not found");
        }
        return user;
    }

    public List<User> getUsers(String role){
        try {
            return userDao.getUsersByRole(role);
        } catch (IotDatabaseException e) {
            LOG.warn(e.getMessage());
            return new ArrayList<>();
        }
    }

    /* public User getUser(String sessionToken) {
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
    } */
}
