package com.signomix.messaging.dto;

public class TelegramMessage {
    public String chat_id;
    public String text;
    public boolean disable_notification;

    public TelegramMessage(String chatID, String text, boolean disableChatNotification){
        this.chat_id=chatID;
        this.text=text;
        this.disable_notification=disableChatNotification;
    }
}
