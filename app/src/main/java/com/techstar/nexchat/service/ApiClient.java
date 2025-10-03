package com.techstar.nexchat.service;

import android.content.Context;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
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
                    logger.e(TAG, "Failed to parse models response", e);
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }
    
    public void streamChat(ApiProvider provider, String model, List<Message> messages, 
                          final ChatCallback callback) {
        String url = provider.getApiUrl() + "/chat/completions";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("stream", true);
        
        JsonArray messagesArray = new JsonArray();
        for (Message message : messages) {
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
                    // 这里需要处理流式响应
                    // 由于流式响应比较复杂，我们先实现非流式版本
                    // 后续可以扩展为真正的流式处理
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
                    logger.e(TAG, "Failed to parse chat response", e);
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
            logger.e(TAG, "Failed to parse models response", e);
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
            logger.e(TAG, "Failed to parse chat response", e);
        }
        return null;
    }
    
    // 内部消息类，用于构建请求
    public static class Message {
        private String role;
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}