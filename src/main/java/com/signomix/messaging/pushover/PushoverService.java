package com.signomix.messaging.pushover;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import com.signomix.messaging.dto.Message;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Singleton
public class PushoverService {

    private static final Logger LOG = Logger.getLogger(PushoverService.class);

    // Patrz: https://quarkus.io/guides/rest-client-multipart
    PushoverClient client;

    @ConfigProperty(name = "pushover.token", defaultValue="not_configured")
    String pushoverToken;

    String pushoverUrl="https://api.pushover.net/1/messages.json";

    public void send(String userKey, Message message) {
        LOG.info("pushover.token="+pushoverToken);
        try {
            Form form = new Form();
            form.param("token", pushoverToken);
            form.param("user",userKey);
            form.param("message",message.eui + ": " + message.content); //URLEncoder.encode(message.eui + ": " + message.content, "UTF-8")

            /*HashMap<String,String>form =new HashMap<>();
            form.put("token", token);
            form.put("user", userKey);
            form.put("message",URLEncoder.encode(message.eui + ": " + message.content, "UTF-8"));
*/
            LOG.info("sending " + message.content);
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(pushoverUrl))
                    .followRedirects(true)
                    .build(PushoverClient.class);
            String response = client.sendMultipartData(form);
            LOG.debug(response);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
            //TODO: notyfikacja użytkownika o błędzie 
        } catch (ProcessingException ex) {
            LOG.error(ex.getMessage());
        } catch (WebApplicationException ex) {
            LOG.error(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            //TODO: notyfikacja użytkownika o błędzie
        }
    }

}
