package com.signomix.messaging.dto;

public class DiscordMessage {

    public String username;
    public String content;

    public DiscordMessage(String username, String content) {
        this.username = username;
        this.content = content;
    }
}
