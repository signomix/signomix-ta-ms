package com.signomix.messaging.dto;

public class SlackMessage {
    public String channel;
    public String text;
    public SlackMessage(String channelID, String text){
        this.text=text;
        this.channel=channelID;
    }
}
