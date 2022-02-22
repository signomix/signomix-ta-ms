package com.signomix.messaging.webhook;

import com.signomix.messaging.slack.SlackService;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import com.signomix.messaging.dto.Message;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;
import com.signomix.messaging.slack.SlackClient;

public class WebhookService {
    private static final Logger LOG = Logger.getLogger(WebhookService.class);
    
    WebhookClient client;    
    
    public void send(String uri, Message message){
        String[] params=uri.split("@");
        try {
            LOG.info("sending "+message.content);
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(params[1]))
                    .followRedirects(true)
                    .build(WebhookClient.class);
            String response=client.sendMessage(params[0], message);
            LOG.debug(response);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
            //TODO: notyfikacja użytkownika o błędzie 
        }catch(ProcessingException ex){
            LOG.error(ex.getMessage());
        }catch(WebApplicationException ex){
            LOG.error(ex.getMessage());
        }catch(Exception ex){
            LOG.error(ex.getMessage());
            //TODO: notyfikacja użytkownika o błędzie
        }
    }

}
