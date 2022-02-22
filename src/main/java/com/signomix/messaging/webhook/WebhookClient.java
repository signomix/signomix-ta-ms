package com.signomix.messaging.webhook;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import com.signomix.messaging.dto.Message;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;

@RegisterRestClient
@Path("/")
public interface WebhookClient {

    @POST
    @Produces("application/json")
    String sendMessage(@HeaderParam("Authorization") String token, Message message) throws ProcessingException, WebApplicationException;

}
