package com.signomix.messaging.discord;

import com.signomix.messaging.dto.DiscordMessage;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/")
public interface DiscordClient {

    @POST
    @Produces("application/json")
    String sendMessage(DiscordMessage message) throws ProcessingException, WebApplicationException;

}
