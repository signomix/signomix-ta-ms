package com.signomix.messaging.application.port.in;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.signomix.common.User;
import com.signomix.messaging.application.exception.ServiceException;
import com.signomix.messaging.application.usecase.AuthUC;
import com.signomix.messaging.application.usecase.MqttLogic;
import com.signomix.messaging.domain.MailingAction;


@ApplicationScoped
public class MailingPort {
    
    @Inject 
    AuthUC athUC;
    
    @Inject
    MqttLogic mailingUseCase;

    @ConfigProperty(name = "com.signomix.messaging.exception.unauthorized")
    String unauthorized;

    public void sendDocument(String docUid, String target, String sessionToken) throws ServiceException {
/*         User actor = athUC.getUserForToken(sessionToken);
        if(null==actor || !actor.role.contains("admin")){
            throw new ServiceException(unauthorized);
        }
        mailingUseCase.processMailing(docUid, target); */
    }

    public void addPlannedMailing(String docUid, String target, String sessionToken) throws ServiceException {
/*         User actor = athUC.getUserForToken(sessionToken);
        if(null==actor || !actor.role.contains("admin")){
            throw new ServiceException(unauthorized);
        }
        mailingUseCase.addPlannedMailing(docUid, target); */
    }

    public void runMailingAction(MailingAction action){
/*         mailingUseCase.runMailingAction(action.getId()); */
    }
}
