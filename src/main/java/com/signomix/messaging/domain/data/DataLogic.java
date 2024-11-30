package com.signomix.messaging.domain.data;


import java.nio.charset.StandardCharsets;
import javax.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;


@ApplicationScoped
public class DataLogic {
    private static final Logger LOG = Logger.getLogger(DataLogic.class);

    public void processDataCreated(byte[] bytes) {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOG.debug("DATA_CREATED2: " + message);
    }

}
