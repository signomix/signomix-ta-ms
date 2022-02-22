package com.signomix.messaging.slack;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;

import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.SlackMessage;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

/**
 * Sends messages to Slack channel.
 * See: https://api.slack.com/messaging/sending
 */
@Singleton
public class SlackService {
    private static final Logger LOG = Logger.getLogger(SlackService.class);

    String slackApiUrl="https://slack.com/api/chat.postMessage";
    SlackClient client;

    public void send(String connectionParams, Message message) {

        String[] params = connectionParams.split("@");
        try {
            LOG.info("sending " + message.content);
            Form form = new Form();
            form.param("channel", params[1]);
            form.param("text",message.eui + ": " + message.content); //URLEncoder.encode(message.eui + ": " + message.content, "UTF-8")

            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(slackApiUrl))
                    .followRedirects(true)
                    .build(SlackClient.class);
            String response = client.sendMessage("Bearer "+params[0], form);
            LOG.info(response);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        } catch (ProcessingException ex) {
            LOG.error(ex.getMessage());
        } catch (WebApplicationException ex) {
            LOG.error(ex.getMessage());
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            // TODO: notyfikacja użytkownika o błędzie
        }
    }

}
