package com.signomix.messaging.domain;

import java.util.HashMap;

public class Document {

    public String path="";
    public String name="";
    public String content="";
    public byte[] binaryContent=null;
    public long updateTimestamp=0;
    public HashMap<String, String> metadata = new HashMap<>();
    public boolean binaryFile=false;
    public String mediaType="";
    public Document() {
    } 

    public String getFileName() {
        return name.substring(path.length());
    }

}
