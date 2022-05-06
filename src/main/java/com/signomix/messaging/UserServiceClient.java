package com.signomix.messaging;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import com.signomix.messaging.dto.User;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/api/user")
public interface UserServiceClient {

    @GET
    @Produces("application/json")
    @Path("/{uid}")
    User getUser(@PathParam("uid") String uid, @QueryParam("appkey") String appKey) throws ProcessingException, WebApplicationException;

}
