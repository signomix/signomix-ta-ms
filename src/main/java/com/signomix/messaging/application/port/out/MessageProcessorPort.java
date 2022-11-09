package com.signomix.messaging.application.port.out;

import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.email.MailerService;

public interface MessageProcessorPort {
    public void processMailing(byte[] bytes);
    public void processEvent(byte[] bytes);
    public void processAdminEmail(byte[] bytes);
    public void processNotification(byte[] bytes);
    public void setMailerService(MailerService service);
    public void setApplicationKey(String key);
    public void setAuthHost(String authHost);
    public void setDao(IotDatabaseIface dao);
}
