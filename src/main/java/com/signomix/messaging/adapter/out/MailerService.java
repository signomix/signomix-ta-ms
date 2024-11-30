package com.signomix.messaging.adapter.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.annotation.OutboundAdapter;
import com.signomix.messaging.application.port.out.NotificationIface;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

@OutboundAdapter
@ApplicationScoped
public class MailerService implements NotificationIface {

    private static final Logger LOG = Logger.getLogger(MailerService.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.username", defaultValue = "not set")
    String mailerUsername;

    public MailerService() {
    }

    @Override
    public String sendEmail(String toAddress, String subject, String body, List<String> bcc) {
        LOG.debug("sendEmail1: " + toAddress + " " + subject);
        new Thread(() -> sendInThread(toAddress, subject, body)).start();
        return "OK";
    }

    @Override
    public String sendHtmlEmail(String toAddress, String subject, String body, List<String> bcc) {
        try {
            LOG.debug("sendEmail3: " + toAddress + " " + subject);
            String[] r = toAddress.split(",");
            ArrayList<String> recipients = new ArrayList<>(Arrays.asList(r));
            Mail mail = Mail.withHtml(recipients.get(0).trim(), subject, body);
            for (int i = 1; i < recipients.size(); i++) {
                mail = mail.addTo(recipients.get(i).trim());
            }
            if(bcc != null){
                mail = mail.setBcc(bcc);
            }
            mailer.send(mail);
        } catch (Exception ex) {
            LOG.error("["+toAddress+"] "+ex.getMessage());
        }
        return "OK";
    }


    private void sendInThread(String toAddress, String subject, String body){
        try {
            LOG.debug("sendEmail2: " + toAddress + " " + subject);
            String[] r = toAddress.split(",");
            ArrayList<String> recipients = new ArrayList<>(Arrays.asList(r));
            Mail mail = Mail.withHtml(recipients.get(0).trim(), subject, body);
            for (int i = 1; i < recipients.size(); i++) {
                mail = mail.addTo(recipients.get(i).trim());
            }
            mailer.send(mail);
        } catch (Exception ex) {
            LOG.error("["+toAddress+"] "+ex.getMessage());
        }
    }

    @Override
    public String send(String recipient, String subject, String body) {
        LOG.info("sending email to " + recipient + " with message: " + body);
        try {
            String[] r = recipient.split(",");
            ArrayList<String> recipients = new ArrayList<>(Arrays.asList(r));
            Mail mail = Mail.withText(recipients.get(0).trim(), subject, body);
            for (int i = 1; i < recipients.size(); i++) {
                mail = mail.addTo(recipients.get(i).trim());
            }
            mailer.send(mail);
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
