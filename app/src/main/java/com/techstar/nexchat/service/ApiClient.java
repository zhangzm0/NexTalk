package com.techstar.nexchat.service;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

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
    
    // 在 ApiClient.java 中更新接口
	public interface ChatCallback {
		void onResponse(String content); // 现在这是分片内容，不是完整内容
		void onError(String error);
		void onComplete();
		// 添加新方法支持流式状态
		void onStreamStart();
		void onStreamEnd();
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
    
    // 在 ApiClient.java 中修改 streamChat 方法
	public void streamChat(ApiProvider provider, String model, List<com.techstar.nexchat.model.Message> messages, 
						   final int chatId, final int pendingMessageId, final ChatCallback callback) {

		logger.i(TAG, "=== API CLIENT START ===");
		logger.i(TAG, "Provider: " + provider.getName());
		logger.i(TAG, "Model: " + model);
		logger.i(TAG, "Messages count: " + messages.size());
		logger.i(TAG, "Chat ID: " + chatId);
		logger.i(TAG, "Pending Message ID: " + pendingMessageId);
		logger.i(TAG, "API URL: " + provider.getApiUrl());

		// ... 原有代码
	
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

		logger.i(TAG, "Starting true streaming chat to: " + url + " with model: " + model);

		// 创建流式管理器实例
		final StreamingRequestManager streamManager = StreamingRequestManager.getInstance(logger);
		final StreamResponseParser streamParser = new StreamResponseParser(logger);

		Call call = client.newCall(request);

		// 注册流式请求
		streamManager.registerStream(chatId, pendingMessageId, call);

		call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					logger.e(TAG, "Failed to stream chat: " + e.getMessage());
					streamManager.updateMessageState(pendingMessageId, StreamingRequestManager.StreamState.FAILED);
					callback.onError("网络请求失败: " + e.getMessage());
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					if (!response.isSuccessful()) {
						String error = "HTTP " + response.code() + ": " + response.message();
						logger.e(TAG, "Failed to stream chat: " + error);
						streamManager.updateMessageState(pendingMessageId, StreamingRequestManager.StreamState.FAILED);
						callback.onError(error);
						return;
					}

					try {
						// 更新状态为流式中
						streamManager.updateMessageState(pendingMessageId, StreamingRequestManager.StreamState.STREAMING);

						// 使用真正的流式解析
						BufferedSource source = response.body().source();
						streamParser.parseStreamResponse(source, new StreamResponseParser.ChunkCallback() {
								private StringBuilder fullContent = new StringBuilder();

								@Override
								public void onContentChunk(String contentChunk) {
									fullContent.append(contentChunk);
									callback.onResponse(contentChunk); // 实时回调每个分片
								}

								@Override
								public void onFunctionCall(String functionName, String arguments) {
									// 预留函数调用处理
									logger.d(TAG, "Function call: " + functionName + ", args: " + arguments);
								}

								@Override
								public void onTokenUpdate(int tokens) {
									// 预留token更新
									logger.d(TAG, "Token update: " + tokens);
								}

								@Override
								public void onError(String error) {
									streamManager.updateMessageState(pendingMessageId, StreamingRequestManager.StreamState.FAILED);
									callback.onError(error);
								}

								@Override
								public void onComplete() {
									streamManager.completeStream(chatId, pendingMessageId);
									callback.onComplete();
									logger.i(TAG, "Stream completed successfully for message: " + pendingMessageId);
								}
							});

					} catch (Exception e) {
						logger.e(TAG, "Failed to process stream response", e);
						streamManager.updateMessageState(pendingMessageId, StreamingRequestManager.StreamState.FAILED);
						callback.onError("流式处理失败: " + e.getMessage());
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
}
