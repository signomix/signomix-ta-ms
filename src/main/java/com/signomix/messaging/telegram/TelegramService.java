package com.signomix.messaging.telegram;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.signomix.messaging.dto.Message;
import com.signomix.messaging.dto.TelegramMessage;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

@Singleton
public class TelegramService {

    private static final Logger LOG = Logger.getLogger(TelegramService.class);

    TelegramClient client;
    String telegramUrlStart="https://api.telegram.org";

    public void send(String address, Message message) {
        String[] params=address.split("@");
        String telegramToken="bot"+params[0];
        String chatID = params[1];
        String url= telegramUrlStart;
        String text=message.eui+" "+message.content;
        try {
            LOG.info("sending " + message.content);
            client = RestClientBuilder.newBuilder()
                    .baseUri(new URI(url))
                    .followRedirects(true)
                    .build(TelegramClient.class);
            String response = client.send(telegramToken, new TelegramMessage(chatID, text, true));
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
