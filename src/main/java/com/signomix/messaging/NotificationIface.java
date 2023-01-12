package com.signomix.messaging;

public interface NotificationIface {
    public String sendEmail(String recipient, String subject, String text);
    public String sendHtmlEmail(String recipient, String subject, String text);
    public String send(String userID, String recipient, String nodeName, String message);
    public String send(String recipient, String nodeName, String message);
    public String getChatID(String recipent);
}
