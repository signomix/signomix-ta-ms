package com.signomix.messaging.telegram;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.signomix.messaging.dto.TelegramMessage;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/")
public interface TelegramClient {

    @POST
    @Path("/{token}/sendMessage")
    @Produces("application/json")
    String send(@PathParam("token")String token, TelegramMessage message);

}
