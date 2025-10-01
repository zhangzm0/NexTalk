package com.techstar.nexchat.model;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

	private int tokens; // 消耗的tokens
	private String responseId; // 响应ID

	public ChatMessage() {
		this.timestamp = System.currentTimeMillis();
		this.id = "msg_" + System.currentTimeMillis();
		this.tokens = 0;
	}

	public ChatMessage(int type, String content) {
		this();
		this.type = type;
		this.content = content;
	}

	// Getter and Setter
	public int getTokens() { return tokens; }
	public void setTokens(int tokens) { this.tokens = tokens; }

	public String getResponseId() { return responseId; }
	public void setResponseId(String responseId) { this.responseId = responseId; }

	// 获取格式化时间
	public String getFormattedTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
		return sdf.format(new Date(timestamp));
	}
}
