package com.signomix.messaging.email;

import com.signomix.annotation.OutboundAdapter;
import com.signomix.messaging.NotificationIface;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;

@OutboundAdapter
@ApplicationScoped
public class MailerService implements NotificationIface {

    private static final Logger LOG = Logger.getLogger(MailerService.class);

    @Inject
    Mailer mailer;

    public MailerService() {
    }

    public String sendEmail(String toAddress, String subject, String body) {
        try {
            LOG.info("sendEmail: " + toAddress + " " + subject);
            mailer.send(Mail.withHtml(toAddress, subject, body));
            return "OK";
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage());
            return "ERROR";
        }
    }

    @Override
    public String send(String recipient, String nodeName, String message) {
        try {
            mailer.send(Mail.withText(recipient, nodeName, message));
            return "OK";
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return "ERROR";
        }
    }

    @Override
    public String send(String userID, String recipient, String nodeName, String message) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
                                                                       // Tools | Templates.
    }

    @Override
    public String getChatID(String recipent) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
                                                                       // Tools | Templates.
    }
}
