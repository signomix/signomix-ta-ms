package com.signomix.messaging.application.port.out;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.signomix.common.iot.Device;

@RegisterRestClient
@Path("/api/iot/device")
public interface DeviceServiceClient {

    @GET
    @Produces("application/json")
    @Path("/{eui}")
    Device getDevice(@PathParam("eui") String uid, @QueryParam("appkey") String appKey) throws ProcessingException, WebApplicationException;

}
