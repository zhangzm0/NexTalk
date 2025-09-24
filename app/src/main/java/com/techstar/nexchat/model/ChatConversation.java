package com.techstar.nexchat.model;

import java.util.ArrayList;
import java.util.List;

public class ChatConversation {
    private String id;
    private String title;
    private long createTime;
    private long updateTime;
    private List<ChatMessage> messages;
    private String providerId;
    private String model;
    private int messageCount;

    public ChatConversation() {
        this.id = "conv_" + System.currentTimeMillis();
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.messages = new ArrayList<>();
    }

    public ChatConversation(String title) {
        this();
        this.title = title;
    }

    // Getter and Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        updateTime = System.currentTimeMillis();
        messageCount = messages.size();
    }

    public String getPreview() {
        if (messages.isEmpty()) return "新对话";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getType() == ChatMessage.TYPE_ASSISTANT) {
                String content = messages.get(i).getContent();
                return content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }
        }
        return messages.get(messages.size() - 1).getContent();
    }
}
