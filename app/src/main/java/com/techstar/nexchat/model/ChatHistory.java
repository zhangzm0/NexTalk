package com.techstar.nexchat.model;

public class ChatHistory {
    private String id;
    private String title;
    private String preview;
    private long timestamp;
    private int messageCount;
    private String providerId;
    private String modelName;

    public ChatHistory() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatHistory(String title, String preview) {
        this();
        this.title = title;
        this.preview = preview;
    }

    // Getter and Setter 方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    // 获取格式化时间
    public String getFormattedTime() {
        // 简单的时间格式化，你可以根据需要完善
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (days > 0) return days + "天前";
        if (hours > 0) return hours + "小时前";
        if (minutes > 0) return minutes + "分钟前";
        return "刚刚";
    }
}
