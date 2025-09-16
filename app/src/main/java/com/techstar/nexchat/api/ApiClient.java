package com.techstar.nexchat.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int TIMEOUT = 8000; // 8s

    private final SharedPreferences sp;

    public ApiClient(Context ctx) {
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    /* 同步阻塞版本 —— 先跑通，后面再包一层 AsyncTask */
    public String send(String userText) {
        try {
            // 读取设置（默认 openai）
            String url  = sp.getString("custom_url",  "https://api.moonshot.cn/v1/chat/completions");
            String key  = sp.getString("custom_key",  "sk-TZZzEeuEGYZbnbhQVofJlP1CPuzqOTfTJoUO5qTQmIHMriE3");

            // 拼请求
            ChatRequest req = new ChatRequest();
            req.messages.add(new ChatRequest.Message("user", userText));

            String jsonBody = new com.google.gson.Gson().toJson(req);
            String auth     = key.isEmpty() ? null : "Bearer " + key;

            // 发请求
            String respJson = HttpUtil.post(url, jsonBody, auth, TIMEOUT);
            Log.d(TAG, "resp=" + respJson);

            // 解析
            ChatResponse resp = new com.google.gson.Gson()
                                      .fromJson(respJson, ChatResponse.class);
            return resp.getReply();

        } catch (Exception e) {
            Log.e(TAG, "request error", e);
            return "网络错误: " + e.getMessage();
        }
    }
}
