package com.signomix.messaging.application.usecase;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.signomix.messaging.application.port.out.SmsplanetClient;

@ApplicationScoped
public class SendSmsUC {

    @ConfigProperty(name = "signomix.sms.provider", defaultValue = "")
    String smsProvider;

    SmsplanetClient smsplanetClient;

    public void sendSms() {
        long smsId=0;
        String phone="";
        String text="";
        if ("smsplanet.pl".equalsIgnoreCase(smsProvider)) {
            smsId = sendWithSmsplanet(phone, text);
        }
        if(smsId>0){
            saveSmsLog(smsId,phone,text);
        }
    }

    private long sendWithSmsplanet(String phone, String text) {
        smsplanetClient.sendSms(phone, text);
        return 0;
    }

    private void saveSmsLog(long smsId, String phone, String text){

    }
}
