// StreamingMessage.java
package com.techstar.nexchat.model;

public class StreamingMessage {
    private Message message;
    private String fullContent;
    private boolean isStreaming;
    private int receivedChunks;
    
    public StreamingMessage(Message message) {
        this.message = message;
        this.fullContent = message.getContent();
        this.isStreaming = false;
        this.receivedChunks = 0;
    }
    
    // Getter and Setter methods
    public Message getMessage() { return message; }
    
    public String getFullContent() { return fullContent; }
    public void setFullContent(String content) { this.fullContent = content; }
    
    public boolean isStreaming() { return isStreaming; }
    public void setStreaming(boolean streaming) { isStreaming = streaming; }
    
    public int getReceivedChunks() { return receivedChunks; }
    public void incrementReceivedChunks() { receivedChunks++; }
    
    // 便捷方法
    public void appendContent(String chunk) {
        this.fullContent += chunk;
        incrementReceivedChunks();
    }
    
    public int getId() { return message.getId(); }
    public String getContent() { return fullContent; }
    public int getRole() { return message.getRole(); }
    public boolean isUser() { return message.isUser(); }
    public boolean isAssistant() { return message.isAssistant(); }
}
