package com.signomix.messaging.adapter.out;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.signomix.messaging.domain.SmsPlanetResponse;

@ApplicationScoped
@RegisterRestClient(configKey="smsplanet-api")
@Path("/sms") // https://api2.smsplanet.pl
public interface SmsplanetClient {

    /**
     * Send SMS message to the given number using smsplanet.pl API.
     * @param key API key
     * @param password API password
     * @param from sender name
     * @param to recipient number
     * @param title message title
     * @param msg message content
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public SmsPlanetResponse sendSms(
        @FormParam("key") String key, 
        @FormParam("password") String password, 
        @FormParam("from") String from, 
        @FormParam("to") String to, 
        @FormParam("name") String title,
        @FormParam("msg") String msg);
    
}
