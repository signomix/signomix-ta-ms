package com.signomix.messaging.sms;

import java.net.http.HttpResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.jboss.logging.Logger;

import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.User;
import com.signomix.messaging.webhook.WebhookStandardClient;

public class SmsplanetService {
    private static final Logger LOG = Logger.getLogger(SmsplanetService.class);

    WebhookStandardClient client = new WebhookStandardClient();

    public void send(User user, String phoneNumber, Message message) {
        new Thread(() -> sendInThread(phoneNumber, message)).start();
    }

    public void sendInThread(String phoneNumber, Message message) {
        String webhookUrl=null; //TODO
        try {
            LOG.info("sending " + message.content + " to " + phoneNumber);
            HttpResponse<String> response=client.sendMessage(webhookUrl, message);
            processResponse(response);
        } catch (ProcessingException ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage());
        } catch (WebApplicationException ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage());
            // TODO: inform user
        }

    }

    private void processResponse(HttpResponse<String> response){
        //TODO
        if(response.statusCode()<200 || response.statusCode()>201){
            //TODO
            return;
        }
        //TODO: get SMS ID and store in the database
    }

}
