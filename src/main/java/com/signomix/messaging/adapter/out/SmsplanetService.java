package com.signomix.messaging.adapter.out;

import com.signomix.messaging.domain.Message;
import com.signomix.messaging.domain.SmsPlanetResponse;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SmsplanetService {
    @Inject
    Logger LOG;

    @Inject
    @RestClient
    SmsplanetClient client;

    @Inject
    @Channel("sms-sent")
    Emitter<String> smsSentEmitter;

    @ConfigProperty(name = "signomix.smsplanet.token", defaultValue = "")
    String smsToken;

    public SmsPlanetResponse send(String phoneNumber, Message message, boolean test) {
        SmsPlanetResponse response= client.sendSms(
            "Bearer "+smsToken,
            "SIGNOMIX", 
            phoneNumber, 
            message.content,
            test ? 1 : 0);
        if(response.errorCode!=null){
            errorHandling(message.source, response);
        }else{
            successHandling(message.source, response);
        }
        return response;
    }

    private void successHandling(String userId, SmsPlanetResponse response) {
        LOG.debug("SMS sent: "+response.messageId);
        smsSentEmitter.send(userId+";"+response.messageId);
    }

    private void errorHandling(String userId, SmsPlanetResponse response) {
        if(response.errorCode==null){
            LOG.error("SMS error: this should not happen");
            return;
        }
        // error codes: https://api2.smsplanet.pl/docs
        LOG.warn("SMS error: "+response.errorCode+" "+response.errorMsg);
        // TODO: sms-error event
    }

}
