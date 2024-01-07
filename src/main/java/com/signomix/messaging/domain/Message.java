package com.signomix.messaging.domain;

public class Message {
    public String eui;
    public String content;
    public String subject;
    public Message(String eui, String content, String subject){
        this.eui=eui;
        this.content=content;
    }
}
