package com.techstar.nexchat.model;

public class ChatHistory {
    private int id;
    private String title;
    private String preview;
    private long timestamp;
    private int messageCount;
    
    public ChatHistory() {}
    
    public ChatHistory(String title) {
        this.title = title;
        this.preview = "";
        this.timestamp = System.currentTimeMillis();
        this.messageCount = 0;
    }
    
    // Getter and Setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}