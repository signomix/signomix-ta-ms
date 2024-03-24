package com.signomix.messaging.adapter.out;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.signomix.messaging.domain.Document;

@ApplicationScoped
@RegisterRestClient(configKey="hcms-api")
@Path("/api/docs")
public interface HcmsClient {

    /**
     * Get a document from HCMS repository.
     * @param path document path
     * @return
     */
    @GET
    public Document getDocument(@QueryParam("path") String path);

}
