package com.signomix.messaging.application.port.out;

import com.signomix.common.db.IotDatabaseIface;
import com.signomix.messaging.adapter.out.MailerService;
import com.signomix.messaging.adapter.out.MailingActionRepository;
import com.signomix.messaging.domain.MailingAction;

public interface MessageProcessorIface {
    public void processMailing(MailingAction action);
    public void processMailing(String docUid, String target);
    //public void processMailing(byte[] bytes);
    public void processEvent(byte[] bytes);
    public void processAdminEmail(byte[] bytes);
    public void processNotification(byte[] bytes);
    public void processAlertMessage(byte[] bytes);
    public void setMailerService(MailerService service);
    public void setApplicationKey(String key);
    public void setAuthHost(String authHost);
    public void setDao(IotDatabaseIface dao);
    public void setMailingRepository(MailingActionRepository repo);
    public void processDataCreated(byte[] bytes);
}
