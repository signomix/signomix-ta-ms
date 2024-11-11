package com.signomix.messaging.application.usecase;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import com.signomix.messaging.adapter.out.MailerService;


@ApplicationScoped
public class ErrorUC {
    private static final Logger LOG = Logger.getLogger(ErrorUC.class);
    
    @Inject
    MailerService mailerService;

    @ConfigProperty(name = "signomix.admin.email")
    String adminEmail;

    public void processError(String subject, String message) {
        mailerService.sendEmail(adminEmail, subject, message, null);
    }

}
