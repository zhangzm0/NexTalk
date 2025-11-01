package com.techstar.nexchat.service;

import android.content.Context;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private Context context;
    private FileLogger logger;
    private OkHttpClient client;
    private Gson gson;
    
    public ApiClient(Context context) {
        this.context = context;
        this.logger = FileLogger.getInstance(context);
        this.gson = new Gson();
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public interface ModelsCallback {
        void onSuccess(List<String> models);
        void onError(String error);
    }
    
    public interface ChatCallback {
        void onResponse(String content);
        void onError(String error);
        void onComplete();
    }
    
    public interface StreamChatCallback {
        void onContentChunk(String content);
        void onReasoningChunk(String reasoning);
        void onError(String error);
        void onComplete();
    }
    
    public void fetchModels(ApiProvider provider, final ModelsCallback callback) {
        String url = provider.getApiUrl() + "/models";
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + provider.getApiKey())
                .header("Content-Type", "application/json")
                .get()
                .build();
        
        logger.i(TAG, "Fetching models from: " + url);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.e(TAG, "Failed to fetch models: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    logger.e(TAG, "Failed to fetch models: " + error);
                    callback.onError(error);
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    logger.d(TAG, "Models response: " + responseBody);
                    
                    List<String> models = parseModelsResponse(responseBody);
                    logger.i(TAG, "Parsed " + models.size() + " models");
                    callback.onSuccess(models);
                    
                } catch (Exception e) {
logger.e(TAG, "Failed to parse models response: " + e.getMessage());
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }
    
    public void streamChat(ApiProvider provider, String model, List<com.techstar.nexchat.model.Message> messages, 
                          final StreamChatCallback callback) {
        String url = provider.getApiUrl() + "/chat/completions";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("stream", true);
        
        JsonArray messagesArray = new JsonArray();
        for (com.techstar.nexchat.model.Message message : messages) {
            JsonObject msg = new JsonObject();
            if (message.isUser()) {
                msg.addProperty("role", "user");
            } else {
                msg.addProperty("role", "assistant");
            }
            msg.addProperty("content", message.getContent());
            messagesArray.add(msg);
        }
        requestBody.add("messages", messagesArray);
        
        RequestBody body = RequestBody.create(JSON, requestBody.toString());
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + provider.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        
        logger.i(TAG, "Streaming chat to: " + url + " with model: " + model);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.e(TAG, "Failed to stream chat: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    logger.e(TAG, "Failed to stream chat: " + error);
                    callback.onError(error);
                    return;
                }
                
                try {
                    // 处理真正的流式响应
                    processStreamResponse(response, callback);
                    
                } catch (Exception e) {
logger.e(TAG, "Failed to process stream response: " + e.getMessage());
                    callback.onError("处理流式响应失败: " + e.getMessage());
                }
            }
        });
    }
    
    private void processStreamResponse(Response response, StreamChatCallback callback) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
        String line;
        
        try {
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    
                    if (data.equals("[DONE]")) {
                        logger.i(TAG, "Stream completed");
                        callback.onComplete();
                        break;
                    }
                    
                    try {
                        JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                        JsonArray choices = json.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            
                            if (delta != null) {
                                // 处理内容块
                                if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                    String content = delta.get("content").getAsString();
                                    if (content != null && !content.isEmpty()) {
                                        callback.onContentChunk(content);
                                    }
                                }
                                
                                // 处理思考过程块（如果有）
                                if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                                    String reasoning = delta.get("reasoning_content").getAsString();
                                    if (reasoning != null && !reasoning.isEmpty()) {
                                        callback.onReasoningChunk(reasoning);
                                    }
                                }
                            }
                        }
} catch (Exception e) {
                        logger.w(TAG, "Failed to parse stream data: " + data + ", error: " + e.getMessage());
                    }
                }
            }
        } finally {
            reader.close();
        }
    }
    
    public void chat(ApiProvider provider, String model, List<com.techstar.nexchat.model.Message> messages, 
                    final ChatCallback callback) {
        String url = provider.getApiUrl() + "/chat/completions";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("stream", false);
        
        JsonArray messagesArray = new JsonArray();
        for (com.techstar.nexchat.model.Message message : messages) {
            JsonObject msg = new JsonObject();
            if (message.isUser()) {
                msg.addProperty("role", "user");
            } else {
                msg.addProperty("role", "assistant");
            }
            msg.addProperty("content", message.getContent());
            messagesArray.add(msg);
        }
        requestBody.add("messages", messagesArray);
        
        RequestBody body = RequestBody.create(JSON, requestBody.toString());
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + provider.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        
        logger.i(TAG, "Chat to: " + url + " with model: " + model);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.e(TAG, "Failed to chat: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    logger.e(TAG, "Failed to chat: " + error);
                    callback.onError(error);
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    logger.d(TAG, "Chat response: " + responseBody);
                    
                    String content = parseChatResponse(responseBody);
                    if (content != null) {
                        callback.onResponse(content);
                        callback.onComplete();
                    } else {
                        callback.onError("解析聊天响应失败");
                    }
                    
                } catch (Exception e) {
logger.e(TAG, "Failed to parse chat response: " + e.getMessage());
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }
    
    private List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JsonObject model = data.get(i).getAsJsonObject();
                    String modelId = model.get("id").getAsString();
                    models.add(modelId);
                }
            }
        } catch (Exception e) {
logger.e(TAG, "Failed to parse models response: " + e.getMessage());
        }
        return models;
    }
    
    private String parseChatResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                if (message != null) {
                    return message.get("content").getAsString();
                }
            }
        } catch (Exception e) {
logger.e(TAG, "Failed to parse chat response: " + e.getMessage());
        }
        return null;
    }
}