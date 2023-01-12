package com.signomix.messaging.application.port.out;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.cricketmsf.microsite.cms.Document;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/api/cs")
public interface ContentServiceClient  extends AutoCloseable{

    @GET
    @Produces("application/json")
    @Path("/{path}")
    Document getDocument(@PathParam("path") String uid, @QueryParam("appkey") String appKey, @QueryParam("language") String language) throws ProcessingException, WebApplicationException;

}
