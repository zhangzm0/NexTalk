package com.techstar.nexchat.api;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;

public final class StreamingApiClient {

    private static final String BOUNDARY = "data: ";
    private final Context ctx;
    private final String url;
    private final String auth;

    public StreamingApiClient(Context ctx, String url, String key) {
        this.ctx = ctx;
        this.url = url;
        this.auth = key.isEmpty() ? null : "Bearer " + key;
    }

    /* 同步阻塞，外部包 AsyncTask */
    public void stream(String userText, final DeltaListener listener) throws IOException {
        HttpsURLConnection conn = null;
        BufferedReader reader = null;
        try {
            /* 1. 拼请求体 */
            String body = buildBody(userText);

            conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/event-stream");
            if (auth != null) conn.setRequestProperty("Authorization", auth);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(0);          // 长连接
            conn.setDoOutput(true);

            /* 2. 写 body */
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes("utf-8"));
            out.flush();

            /* 3. 读流 */
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line;
            StringBuilder deltaBuf = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(BOUNDARY)) {
                    String json = line.substring(BOUNDARY.length()).trim();
                    if ("[DONE]".equals(json)) break;
                    String delta = extractDelta(json);
                    if (delta != null) {
                        deltaBuf.append(delta);
                        listener.onDelta(deltaBuf.toString()); // 全量追加
                    }
                }
            }
            listener.onDone(deltaBuf.toString());
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignore) {}
            if (conn != null) conn.disconnect();
        }
    }

    /* 纯字符串截取，不引 Gson */
    private String extractDelta(String json) {
        int idx = json.indexOf("\"content\":\"");
        if (idx == -1) return null;
        int start = idx + 11;
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private String buildBody(String text) {
        return "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"" +
               escape(text) + "\"}],\"stream\":true}";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    /* 回调接口 */
    public interface DeltaListener {
        void onDelta(String current); // 当前已拼好的全文
        void onDone(String full);
    }
}
