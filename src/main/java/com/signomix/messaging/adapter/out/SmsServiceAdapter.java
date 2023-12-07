package com.signomix.messaging.adapter.out;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.signomix.messaging.domain.SmsPlanetResponse;

public class SmsServiceAdapter {
    
    @RestClient
    SmsplanetClient smsplanetClient;

    SmsPlanetResponse sendSms(){
        String key="";
        String password="";
        String from="";
        String msg="";
        String to="";
        String name="";
        return smsplanetClient.sendSms(key, password, from, msg, to, name);
    }
}
