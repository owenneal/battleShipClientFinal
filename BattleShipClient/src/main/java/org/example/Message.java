package org.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private String sender; //sender of message
    private List<String> receiver; //receiver of message
    private String type; //type of message server or client

    private final String group; //group name
    private final String message;

    //message constructor, only need one because we are using a list even if only one recipient
    public Message(String sender, List<String>recipients, String message, String type, String groupName) {
        this.sender = sender;
        this.receiver = recipients;
        this.type = type;
        this.group = groupName;
        this.message = message;
    }

    //getters
    public String getSender() {
        return sender;
    }

    public List<String> getReceiver() {
        return receiver;
    }

    public String getGroupName() {
        return group;
    }


    public String getType() {
        return type;
    }

    //toString method
    public String toString() {
        return "From: " + sender + " To: " + receiver + " Message:    Timestamp: ";
    }

    public String getMessage() {
        return this.message;
    }

    //setters
    public void setType(String type) {
        this.type = type;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(List<String> receiver) {
        this.receiver = receiver;
    }

}
