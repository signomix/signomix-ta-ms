package com.signomix.messaging.webhook;

import java.net.http.HttpResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.jboss.logging.Logger;

import com.signomix.messaging.domain.Message;

public class WebhookService {
    private static final Logger LOG = Logger.getLogger(WebhookService.class);

    WebhookStandardClient client = new WebhookStandardClient();

    public void send(String uri, Message message) {
        new Thread(() -> sendInThread(uri, message)).start();
    }

    public void sendInThread(String uri, Message message) {
        String headerName = "";
        String headerValue = "";
        String webhookUrl;
        int index1 = uri.indexOf("@");
        int index2 = uri.indexOf("http");
        webhookUrl = uri.substring(index2);
        if (index1 > -1 && index1 < index2) {
            String[] params = uri.substring(0, index1).split(":");
            if (params.length > 1) {
                headerName = params[0];
                headerValue = params[1];
            } else {
                headerName = "Authorization";
                headerValue = params[0];
            }
        } else {
            webhookUrl = uri;
        }
        try {
            LOG.debug("sending " + message.content + " to " + webhookUrl);
            HttpResponse<String> response;
            if (headerName.isEmpty()) {
                response = client.sendMessage(webhookUrl, message);
            } else {
                response = client.sendMesaage(webhookUrl, headerName, headerValue, message);
            }
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

}
