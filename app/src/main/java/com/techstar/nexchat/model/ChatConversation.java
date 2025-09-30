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
    private boolean isPinned; // 是否置顶

    public ChatConversation() {
        this.id = "conv_" + System.currentTimeMillis();
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.messages = new ArrayList<>();
        this.isPinned = false;
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

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        updateTime = System.currentTimeMillis();
        messageCount = messages.size();
    }

    // 获取格式化时间
    public String getFormattedTime() {
        long diff = System.currentTimeMillis() - updateTime;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (days > 0) return days + "天前";
        if (hours > 0) return hours + "小时前";
        if (minutes > 0) return minutes + "分钟前";
        return "刚刚";
    }

    // 获取最后一条消息的预览
    public String getPreview() {
        if (messages.isEmpty()) return "新对话";
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getType() == ChatMessage.TYPE_ASSISTANT && !message.getContent().isEmpty()) {
                String content = message.getContent();
                return content.length() > 30 ? content.substring(0, 30) + "..." : content;
            }
        }
        // 如果没有AI回复，显示用户最后一条消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getType() == ChatMessage.TYPE_USER && !message.getContent().isEmpty()) {
                String content = message.getContent();
                return content.length() > 30 ? content.substring(0, 30) + "..." : content;
            }
        }
        return "空对话";
    }
}

