package com.signomix.messaging.domain.user;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDao;
import com.signomix.common.db.UserDaoIface;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class UserLogic {

    @Inject
    Logger logger;

    /* @Inject
    MailerService mailerService; */

    @Inject
    MessageProcessorAdapter messagePort;

    @ConfigProperty(name = "signomix.mqtt.field.separator", defaultValue = ";")
    String mqttFieldSeparator;

    @ConfigProperty(name = "signomix.service.url", defaultValue = "https://cloud.locelhost")
    String serviceUrl;

    @Inject
    @DataSource("user")
    AgroalDataSource userDataSource;

    AuthDaoIface dao;
    UserDaoIface userDao;

    void onStart(@Observes StartupEvent ev) {
        userDao = new UserDao();
        userDao.setDatasource(userDataSource);
    }

    public void processUserEvent(String message) {
        logger.info("User event: " + message);
        String[] parts = message.split(mqttFieldSeparator);
        if (parts.length < 3) {
            logger.error("Wrong user event message: " + message);
            return;
        }
        String eventType = parts[0];
        String admin = parts[1];
        String userId = parts[2];

        switch (eventType.toLowerCase()) {
            case "created":
                sendAskToConfirmEmail(userId);
                break;
            case "activated":
            case "created_and_activated":
                sendConfirmationEmail(userId);
                break;
            case "updated":
                break;
            case "remove_requested":
                sendAccountRemoveRequestedEmail(userId);
                break;
            case "removed":
                sendAccountRemovedEmail(userId);
                break;
            default:
        }
    }

    private void sendAskToConfirmEmail(String userLogin) {
        User user = null;
        try {
            user = userDao.getUser(userLogin);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == user) {
            logger.error("User not found: " + userLogin);
            return;
        }
        String subject = "Confirm Your Signomix Registration";

        String message = 
            "<p>Thank you for registering with Signomix.</p>"
            +"<p>To activate your account, please confirm your registration by clicking on <a href=\"" + serviceUrl + "/api/confirm?key="+ user.confirmString + "\">this link</a>.<br>"
            +"If you have any concerns or did not initiate this registration, please send e-mail to signomix@signomix.com.<br>"
            +"If you prefer not to register, you can simply ignore this email.<br>"
            +"We look forward to welcoming you to Signomix.</p>"
            +"<p>Best regards,<br>"
            +"Grzegorz Skorupa</p>"
            +"<p></p>"
            +"<p>Dziękuję za rejestrację w Signomix.</p>"
            +"<p>Aby aktywować swoje konto, proszę o potwierdzenie rejestracji, klikając na <a href=\"" + serviceUrl + "/api/confirm?key="+ user.confirmString + "\">ten link</a>.<br>"
            +"Jeśli masz jakiekolwiek wątpliwości lub nie zainicjowałeś/aś tej rejestracji, skontaktuj się wysyłając e-mail na adres signomix@signomix.com.<br>"
            +"Jeśli nie chcesz się rejestrować, możesz po prostu zignorować tę wiadomość.<br>"
            +"Czekamy z niecierpliwością na powitanie Cię w Signomiksie.</p>"
            +"<p>Pozdrawiam,<br>"
            +"Grzegorz Skorupa</p>";
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.message=message;
        envelope.subject=subject;
        envelope.user=user;
        messagePort.processDirectEmail(envelope);
    }

    private void sendConfirmationEmail(String user) {

    }

    private void sendAccountRemoveRequestedEmail(String user) {

    }

    private void sendAccountRemovedEmail(String user) {

    }

}