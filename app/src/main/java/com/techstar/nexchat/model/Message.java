package com.techstar.nexchat.model;

public class Message {
    public static final int ROLE_USER = 0;
    public static final int ROLE_ASSISTANT = 1;
    
    private int id;
    private int chatId;
    private int role; // 0: user, 1: assistant
    private String content;
    private long timestamp;
    private int tokens;
    private String model;
    
    public Message() {}
    
    public Message(int chatId, int role, String content) {
        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.tokens = 0;
    }
    
    // Getter and Setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getChatId() { return chatId; }
    public void setChatId(int chatId) { this.chatId = chatId; }
    
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public boolean isUser() {
        return role == ROLE_USER;
    }
    
    public boolean isAssistant() {
        return role == ROLE_ASSISTANT;
    }
    
    public boolean isUser() {
        return role == ROLE_USER;
    }
    
    public boolean isAssistant() {
        return role == ROLE_ASSISTANT;
    }
}