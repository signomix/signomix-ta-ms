package com.signomix.messaging.adapter.out;

import java.net.http.HttpResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.messaging.domain.Message;
import com.signomix.messaging.domain.SmsPlanetResponse;

@ApplicationScoped
public class SmsplanetService {
    @Inject
    Logger LOG;

    @Inject
    @RestClient
    SmsplanetClient client;

    @ConfigProperty(name = "signomix.smsplanet.key", defaultValue = "")
    String smsKey;

    @ConfigProperty(name = "signomix.smsplanet.password", defaultValue = "")
    String smsPassword;

    public void send(User user, String phoneNumber, Message message) {
        SmsPlanetResponse response= client.sendSms(smsKey, smsPassword, "SIGNOMIX", phoneNumber, message.eui, message.content);
        if(response.errorCode!=null){
            LOG.error("SMS error: "+response.errorCode+" "+response.errorMsg);
        }
    }

}
