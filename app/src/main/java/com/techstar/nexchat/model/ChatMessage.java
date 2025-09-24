package com.techstar.nexchat.model;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ASSISTANT = 1;
    public static final int TYPE_SYSTEM = 2;

    private String id;
    private int type;
    private String content;
    private long timestamp;
    private boolean isStreaming;
    private String model;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.id = "msg_" + System.currentTimeMillis();
    }

    public ChatMessage(int type, String content) {
        this();
        this.type = type;
        this.content = content;
    }

    // Getter and Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isStreaming() { return isStreaming; }
    public void setStreaming(boolean streaming) { isStreaming = streaming; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
