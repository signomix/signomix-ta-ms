package com.signomix.messaging.domain;

import javax.enterprise.context.Dependent;

@Dependent
public class User {

    public Integer type;
    public String uid;
    public String email;
    public String name;
    public String surname;
    public String role;
    public Boolean confirmed;
    public Boolean unregisterRequested;
    public String confirmString;
    public String password;
    public String generalNotificationChannel = "";
    public String infoNotificationChannel = "";
    public String warningNotificationChannel = "";
    public String alertNotificationChannel = "";
    public Integer authStatus;
    public Long createdAt;
    public Long number;
    public Integer services;
    public String phonePrefix;
    public Long credits;
    public Boolean autologin;
    public String preferredLanguage;
    public Long organization;
    public String sessionToken;
    public String organizationCode;
    public String path="";
    public Integer phone;
    public Integer tenant;
    public String pathRoot="";
    public User() {
    }

    public String[] getChannelConfig(String eventTypeName) {
        String channel = "";
        switch (eventTypeName.toUpperCase()) {
            case "GENERAL":
            case "DEVICE_LOST":
                channel = generalNotificationChannel;
                break;
            case "INFO":
                channel = infoNotificationChannel;
                break;
            case "WARNING":
                channel = warningNotificationChannel;
                break;
            case "ALERT":
                channel = alertNotificationChannel;
                break;
        }
        if (channel == null) {
            channel = "";
        }
        return channel.split(":");
    }

}
