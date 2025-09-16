package com.techstar.nexchat.model;

public class Message {
    public String role;      // "user" 或 "assistant"
    public String content;
    public long   timestamp;

    public Message(String role, String content) {
        this.role      = role;
        this.content   = content;
        this.timestamp = System.currentTimeMillis();
    }
}
