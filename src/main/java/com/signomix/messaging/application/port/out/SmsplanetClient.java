package com.signomix.messaging.application.port.out;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface SmsplanetClient {

    public String sendSms(String phone, String text);
    
}
