package com.signomix.messaging.domain;

public class Message {
    public String eui;
    public String content;
    public Message(String eui, String content){
        this.eui=eui;
        this.content=content;
    }
}
