package com.signomix.messaging.dto;

import java.util.UUID;

public class MessageWrapper {
    public UUID uuid;
    public String type;
    public String eui;
    public String subject;
    public String message;
    public User user;
    
    public MessageWrapper(){
        uuid=UUID.randomUUID();
    }
}
