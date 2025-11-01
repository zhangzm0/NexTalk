package com.techstar.nexchat.model;

public class Message {
    public static final int ROLE_USER = 0;
    public static final int ROLE_ASSISTANT = 1;
    
    // 消息状态常量
    public static final int STATUS_SENDING = 0;
    public static final int STATUS_STREAMING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_ERROR = 3;
    
    private int id;
    private int chatId;
    private int role; // 0: user, 1: assistant
    private String content;
    private long timestamp;
    private int tokens;
    private String model;
    
    // 新增字段：树状对话支持
    private String reasoningContent; // 思考过程内容
    private String toolCalls; // 工具调用信息
    private int parentId; // 父消息ID，0表示根消息
    private String branchId; // 分支ID
    private boolean hasReasoning; // 是否有思考过程
    private int status; // 消息状态
    
    public Message() {
        this.parentId = 0;
        this.branchId = "main";
        this.hasReasoning = false;
        this.status = STATUS_COMPLETED;
    }
    
    public Message(int chatId, int role, String content) {
        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.tokens = 0;
        this.parentId = 0;
        this.branchId = "main";
        this.hasReasoning = false;
        this.status = STATUS_COMPLETED;
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
    
    // 新增字段的Getter和Setter
    public String getReasoningContent() { return reasoningContent; }
    public void setReasoningContent(String reasoningContent) { 
        this.reasoningContent = reasoningContent; 
        this.hasReasoning = reasoningContent != null && !reasoningContent.isEmpty();
    }
    
    public String getToolCalls() { return toolCalls; }
    public void setToolCalls(String toolCalls) { this.toolCalls = toolCalls; }
    
    public int getParentId() { return parentId; }
    public void setParentId(int parentId) { this.parentId = parentId; }
    
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    
    public boolean hasReasoning() { return hasReasoning; }
    public void setHasReasoning(boolean hasReasoning) { this.hasReasoning = hasReasoning; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    // 状态检查方法
    public boolean isSending() { return status == STATUS_SENDING; }
    public boolean isStreaming() { return status == STATUS_STREAMING; }
    public boolean isCompleted() { return status == STATUS_COMPLETED; }
    public boolean isError() { return status == STATUS_ERROR; }
    
    public boolean isUser() {
        return role == ROLE_USER;
    }
    
    public boolean isAssistant() {
        return role == ROLE_ASSISTANT;
    }
}