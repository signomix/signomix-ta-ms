package com.signomix.messaging.adapter.out;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.signomix.messaging.domain.Document;

@ApplicationScoped
public class HcmsService {

    @Inject
    Logger logger;

    @Inject
    @RestClient HcmsClient client;
    

    public Document getDocument(String path) {
        logger.info("Getting document from HCMS: "+path);
        Document doc = client.getDocument(path);
        return doc;
    }
}
