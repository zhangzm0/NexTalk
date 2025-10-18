// StreamResponseParser.java
package com.techstar.nexchat.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.techstar.nexchat.util.FileLogger;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okio.BufferedSource;

public class StreamResponseParser {
    private static final String TAG = "StreamResponseParser";
    
    private FileLogger logger;
    private static final String DATA_PREFIX = "data: ";
    private static final String DONE_INDICATOR = "[DONE]";
    
    public StreamResponseParser(FileLogger logger) {
        this.logger = logger;
    }
    
    public interface ChunkCallback {
        void onContentChunk(String contentChunk);
        void onFunctionCall(String functionName, String arguments);
        void onTokenUpdate(int tokens);
        void onError(String error);
        void onComplete();
    }
    
    public void parseStreamResponse(BufferedSource source, ChunkCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processStreamData(source, callback);
                } catch (IOException e) {
                    logger.e(TAG, "Stream parsing error: " + e.getMessage());
                    callback.onError("流式解析失败: " + e.getMessage());
                } finally {
                    try {
                        source.close();
                    } catch (IOException e) {
                        logger.e(TAG, "Failed to close source: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    
    private void processStreamData(BufferedSource source, ChunkCallback callback) throws IOException {
        while (!source.exhausted()) {
            String line = source.readUtf8Line();
            if (line == null) continue;
            
            logger.d(TAG, "Raw stream line: " + line);
            
            if (line.startsWith(DATA_PREFIX)) {
                String data = line.substring(DATA_PREFIX.length()).trim();
                
                if (data.equals(DONE_INDICATOR)) {
                    logger.i(TAG, "Stream completed with DONE indicator");
                    callback.onComplete();
                    break;
                }
                
                if (!data.isEmpty()) {
                    processDataChunk(data, callback);
                }
            }
            
            // 防止过快循环消耗CPU
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void processDataChunk(String data, ChunkCallback callback) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject delta = choice.getAsJsonObject("delta");
                
                if (delta != null) {
                    // 处理内容更新
                    if (delta.has("content")) {
                        String content = delta.get("content").getAsString();
                        if (content != null && !content.isEmpty()) {
                            callback.onContentChunk(content);
                        }
                    }
                    
                    // 处理函数调用（预留）
                    if (delta.has("function_call")) {
                        processFunctionCall(delta.getAsJsonObject("function_call"), callback);
                    }
                    
                    // 处理token统计（预留）
                    if (json.has("usage")) {
                        processTokenUsage(json.getAsJsonObject("usage"), callback);
                    }
                }
            }
        } catch (Exception e) {
            logger.e(TAG, "Failed to parse data chunk: " + data, e);
        }
    }
    
    private void processFunctionCall(JsonObject functionCall, ChunkCallback callback) {
        // 预留函数调用处理
        logger.d(TAG, "Function call detected: " + functionCall.toString());
    }
    
    private void processTokenUsage(JsonObject usage, ChunkCallback callback) {
        // 预留token统计处理
        if (usage.has("completion_tokens")) {
            int tokens = usage.get("completion_tokens").getAsInt();
            callback.onTokenUpdate(tokens);
        }
    }
}
