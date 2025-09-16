package com.techstar.nexchat.api;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

public final class HttpUtil {

    public static String post(String targetUrl,
                              String json,
                              String authHead,
                              int timeoutMs) throws IOException {

        HttpsURLConnection conn = null;
        OutputStream out = null;
        InputStream  in  = null;
        try {
            conn = (HttpsURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (authHead != null) {
                conn.setRequestProperty("Authorization", authHead);
            }
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);

            /* 写 body */
            out = conn.getOutputStream();
            out.write(json.getBytes("utf-8"));
            out.flush();

            /* 读响应 */
            int code = conn.getResponseCode();
            in = (code >= 200 && code < 300) ? conn.getInputStream()
                                             : conn.getErrorStream();
            return streamToString(in);
        } finally {
            if (out != null) try { out.close(); } catch (IOException ignore) {}
            if (in  != null) try { in.close();  } catch (IOException ignore) {}
            if (conn != null) conn.disconnect();
        }
    }

    private static String streamToString(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        return sb.toString().trim();
    }
}
