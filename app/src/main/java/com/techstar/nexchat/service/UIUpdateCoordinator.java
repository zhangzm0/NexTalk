// UIUpdateCoordinator.java
package com.techstar.nexchat.service;

import com.techstar.nexchat.util.FileLogger;
import com.techstar.nexchat.adapter.MessageAdapter;
import com.techstar.nexchat.model.StreamingMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UIUpdateCoordinator {
    private static final String TAG = "UIUpdateCoordinator";
    
    private static UIUpdateCoordinator instance;
    private FileLogger logger;
    
    // 管理消息状态：messageId -> StreamingMessage
    private Map<Integer, StreamingMessage> streamingMessages;
    // 管理适配器引用：chatId -> MessageAdapter
    private Map<Integer, MessageAdapter> activeAdapters;
    // 防抖动控制
    private Map<Integer, Long> lastUpdateTime;
    private static final long UPDATE_THROTTLE_MS = 50; // 50ms更新间隔
    
    private UIUpdateCoordinator(FileLogger logger) {
        this.logger = logger;
        this.streamingMessages = new ConcurrentHashMap<>();
        this.activeAdapters = new ConcurrentHashMap<>();
        this.lastUpdateTime = new ConcurrentHashMap<>();
    }
    
    public static synchronized UIUpdateCoordinator getInstance(FileLogger logger) {
        if (instance == null) {
            instance = new UIUpdateCoordinator(logger);
        }
        return instance;
    }
    
    public void registerAdapter(int chatId, MessageAdapter adapter) {
        activeAdapters.put(chatId, adapter);
        logger.d(TAG, "Registered adapter for chat: " + chatId);
    }
    
    public void unregisterAdapter(int chatId) {
        activeAdapters.remove(chatId);
        logger.d(TAG, "Unregistered adapter for chat: " + chatId);
    }
    
    public void registerStreamingMessage(int messageId, StreamingMessage streamingMessage) {
        streamingMessages.put(messageId, streamingMessage);
        logger.d(TAG, "Registered streaming message: " + messageId);
    }
    
    public void updateMessageContent(int chatId, int messageId, String contentChunk) {
        StreamingMessage streamingMessage = streamingMessages.get(messageId);
        if (streamingMessage != null) {
            // 追加内容
            streamingMessage.appendContent(contentChunk);
            streamingMessage.setStreaming(true);
            
            // 防抖动检查
            long currentTime = System.currentTimeMillis();
            Long lastUpdate = lastUpdateTime.get(messageId);
            
            if (lastUpdate == null || (currentTime - lastUpdate) >= UPDATE_THROTTLE_MS) {
                // 执行UI更新
                updateUI(chatId, messageId);
                lastUpdateTime.put(messageId, currentTime);
            }
        } else {
            logger.w(TAG, "Streaming message not found: " + messageId);
        }
    }
    
    public void completeStreaming(int chatId, int messageId) {
        StreamingMessage streamingMessage = streamingMessages.get(messageId);
        if (streamingMessage != null) {
            streamingMessage.setStreaming(false);
            updateUI(chatId, messageId);
            
            // 清理状态
            streamingMessages.remove(messageId);
            lastUpdateTime.remove(messageId);
            
            logger.i(TAG, "Completed streaming for message: " + messageId);
        }
    }
    
    private void updateUI(int chatId, int messageId) {
        MessageAdapter adapter = activeAdapters.get(chatId);
        if (adapter != null) {
            // 使用新的更新方法
            adapter.updateMessageContent(messageId);
        } else {
            logger.w(TAG, "No adapter registered for chat: " + chatId);
        }
    }
    
    public StreamingMessage getStreamingMessage(int messageId) {
        return streamingMessages.get(messageId);
    }
    
    // 在 UIUpdateCoordinator.java 中修改 cleanupChat 方法

	public void cleanupChat(int chatId) {
		// 清理该聊天的所有流式消息 - 不使用 lambda
		java.util.Iterator<java.util.Map.Entry<Integer, StreamingMessage>> iterator = 
			streamingMessages.entrySet().iterator();

		while (iterator.hasNext()) {
			java.util.Map.Entry<Integer, StreamingMessage> entry = iterator.next();
			StreamingMessage streamingMessage = entry.getValue();
			if (streamingMessage != null && streamingMessage.getMessage().getChatId() == chatId) {
				iterator.remove();
				logger.d(TAG, "Cleaned up streaming message: " + entry.getKey());
			}
		}

		// 清理时间记录 - 不使用 lambda
		java.util.Iterator<java.util.Map.Entry<Integer, Long>> timeIterator = 
			lastUpdateTime.entrySet().iterator();

		while (timeIterator.hasNext()) {
			java.util.Map.Entry<Integer, Long> entry = timeIterator.next();
			Integer messageId = entry.getKey();
			StreamingMessage msg = streamingMessages.get(messageId);
			if (msg != null && msg.getMessage().getChatId() == chatId) {
				timeIterator.remove();
			}
		}

		activeAdapters.remove(chatId);
		logger.i(TAG, "Cleaned up all streaming data for chat: " + chatId);
	}
}
