// StreamingRequestManager.java
package com.techstar.nexchat.service;

import com.techstar.nexchat.util.FileLogger;
import okhttp3.Call;
import okhttp3.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamingRequestManager {
    private static final String TAG = "StreamingRequestManager";
    
    private static StreamingRequestManager instance;
    private FileLogger logger;
    
    // 管理活跃的流式请求：chatId -> Call
    private Map<Integer, Call> activeStreams;
    // 管理消息状态：messageId -> 流式状态
    private Map<Integer, StreamState> messageStates;
    
    public enum StreamState {
        PENDING,
        STREAMING, 
        COMPLETED,
        FAILED,
        INTERRUPTED
    }
    
    private StreamingRequestManager(FileLogger logger) {
        this.logger = logger;
        this.activeStreams = new ConcurrentHashMap<>();
        this.messageStates = new ConcurrentHashMap<>();
    }
    
    public static synchronized StreamingRequestManager getInstance(FileLogger logger) {
        if (instance == null) {
            instance = new StreamingRequestManager(logger);
        }
        return instance;
    }
    
    public void registerStream(int chatId, int messageId, Call call) {
        activeStreams.put(chatId, call);
        messageStates.put(messageId, StreamState.PENDING);
        logger.i(TAG, "Registered stream for chat: " + chatId + ", message: " + messageId);
    }
    
    public void updateMessageState(int messageId, StreamState state) {
        messageStates.put(messageId, state);
        logger.d(TAG, "Updated message " + messageId + " state to: " + state);
    }
    
    public StreamState getMessageState(int messageId) {
        return messageStates.getOrDefault(messageId, StreamState.COMPLETED);
    }
    
    public void interruptStream(int chatId) {
        Call call = activeStreams.get(chatId);
        if (call != null) {
            if (!call.isCanceled()) {
                call.cancel();
                logger.i(TAG, "Interrupted stream for chat: " + chatId);
            }
            activeStreams.remove(chatId);
        }
    }
    
    public void completeStream(int chatId, int messageId) {
        activeStreams.remove(chatId);
        messageStates.put(messageId, StreamState.COMPLETED);
        logger.i(TAG, "Completed stream for chat: " + chatId + ", message: " + messageId);
    }
    
    public void cleanup() {
        // 取消所有活跃的流式请求
        for (Call call : activeStreams.values()) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        activeStreams.clear();
        messageStates.clear();
        logger.i(TAG, "Cleaned up all streaming requests");
    }
    
    public boolean hasActiveStream(int chatId) {
        return activeStreams.containsKey(chatId);
    }
}
