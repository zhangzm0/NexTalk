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
    private int tokens; // 消耗的tokens
    private String responseId; // 响应ID
    private int promptTokens; // 输入tokens
    private int completionTokens; // 输出tokens
    private int totalTokens; // 总tokens

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.id = "msg_" + System.currentTimeMillis();
        this.tokens = 0;
        this.promptTokens = 0;
        this.completionTokens = 0;
        this.totalTokens = 0;
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

    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }



    // 获取格式化时间
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
	
	// 在ChatMessage类中添加这些方法
	public void setTokensInfo(int promptTokens, int completionTokens, int totalTokens) {
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.totalTokens = totalTokens;
		this.tokens = totalTokens; // 向后兼容
	}

// 在ChatMessage.java中修改getTokensText方法
	public String getTokensText() {
		if (totalTokens > 0) {
			if (promptTokens > 0 && completionTokens > 0) {
				return promptTokens + "+" + completionTokens + "=" + totalTokens + " tokens";
			} else {
				return totalTokens + " tokens";
			}
		} else if (tokens > 0) {
			return tokens + " tokens";
		} else {
			return "无统计"; // 改为"无统计"更准确
		}
	}
	
	
	
}
