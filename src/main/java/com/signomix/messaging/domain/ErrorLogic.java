package com.signomix.messaging.domain;

import com.signomix.messaging.adapter.out.MailerService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@ApplicationScoped
public class ErrorLogic {
    private static final Logger LOG = Logger.getLogger(ErrorLogic.class);
    
    @Inject
    MailerService mailerService;

    @ConfigProperty(name = "signomix.admin.email")
    String adminEmail;

    public void processError(String subject, String message) {
        mailerService.sendEmail(adminEmail, subject, message, null);
    }

}
