package com.signomix.messaging.application.port.in;

public interface ForCommandSendingIface {
    public void sendWaitingCommands(byte[] bytes);
}
