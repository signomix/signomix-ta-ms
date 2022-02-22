package com.signomix.messaging.telegram;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class MultipartBody {

    @FormParam("token")
    @PartType(MediaType.TEXT_PLAIN)
    public String token;
    
    @FormParam("user")
    @PartType(MediaType.TEXT_PLAIN)
    public String user;
    
    @FormParam("message")
    @PartType(MediaType.TEXT_PLAIN)
    public String message;
}
