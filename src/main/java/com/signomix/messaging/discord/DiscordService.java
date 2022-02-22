package com.signomix.messaging.discord;

import com.signomix.messaging.dto.DiscordMessage;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

public class DiscordService {
    private static final Logger LOG = Logger.getLogger(DiscordService.class);
    
    String discordUrl="https://discord.com/api/webhooks/";
    DiscordClient client;    
    
    public void send(String discordWebhook, DiscordMessage message){
    
        try {
            LOG.info("sending "+message.content);
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(discordUrl+discordWebhook))
                    .followRedirects(true)
                    .build(DiscordClient.class);
            String response=client.sendMessage(message);
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
