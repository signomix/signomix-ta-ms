package com.signomix.messaging.domain;

public class Message {
    public String source; // eg. EUI or user login
    public String content;
    public String subject;
    public Message(String source, String content, String subject){
        this.source=source;
        this.content=content;
        this.subject=subject;
    }
}
