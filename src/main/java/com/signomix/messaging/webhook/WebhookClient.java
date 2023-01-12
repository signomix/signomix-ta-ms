package com.signomix.messaging.webhook;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;

import com.signomix.messaging.domain.Message;

@RegisterRestClient
@Path("/")
public interface WebhookClient {

    @POST
    @Produces("application/json")
    String sendMessage(@HeaderParam("Authorization") String token, Message message) throws ProcessingException, WebApplicationException;

}
