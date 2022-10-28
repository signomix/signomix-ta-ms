package com.signomix.messaging.dto;

import java.util.UUID;

public class EventWrapper {
    public UUID uuid;
    public String type;
    public String id;
    public String payload;
    public long timestamp;
    
    public EventWrapper(){
        uuid=UUID.randomUUID();
    }
}
