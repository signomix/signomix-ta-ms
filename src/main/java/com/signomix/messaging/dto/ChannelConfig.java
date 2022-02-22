package com.signomix.messaging.dto;

public class ChannelConfig {
    public String channelType=null;
    public String address=null;
    
    public ChannelConfig(){
    }
    
    public ChannelConfig(String[] config){
        if (config == null || config.length < 2) {
            return;
        }
        String messageChannel = config[0];
        String address;
        if (config.length == 2) {
            address = config[1];
        } else {
            StringBuilder sb=new StringBuilder();
            for (int i = 1; i < config.length - 1; i++) {
                sb.append(config[i]).append(i<config.length-1?":":"");
            }
            address=sb.toString();
        }
    }
}
