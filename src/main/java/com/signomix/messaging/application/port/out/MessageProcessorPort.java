package com.signomix.messaging.application.port.out;

public interface MessageProcessorPort {
    public void processMailing(byte[] bytes);
    public void processEvent(byte[] bytes);
    public void processAdminEmail(byte[] bytes);
    public void processNotification(byte[] bytes);
}
