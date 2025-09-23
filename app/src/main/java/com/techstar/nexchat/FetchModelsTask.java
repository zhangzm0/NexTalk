package com.techstar.nexchat;

import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FetchModelsTask extends AsyncTask<Void, Void, String> {

    private String apiUrl;
    private String apiKey;
    private ProgressBar progressBar;
    private Button btnFetch;
    private List<String> models;
    private boolean success;

    public FetchModelsTask(String apiUrl, String apiKey, ProgressBar progressBar, Button btnFetch) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.progressBar = progressBar;
        this.btnFetch = btnFetch;
        this.models = new ArrayList<String>();
        this.success = false;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(apiUrl + "/models")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();

            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray data = json.getJSONArray("data");

                for (int i = 0; i < data.length(); i++) {
                    JSONObject model = data.getJSONObject(i);
                    String modelId = model.getString("id");
                    models.add(modelId);
                }
                success = true;
                return "成功获取 " + models.size() + " 个模型";
            } else {
                return "错误: " + response.code() + " - " + responseBody;
            }

        } catch (Exception e) {
            return "异常: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        progressBar.setVisibility(View.GONE);
        btnFetch.setEnabled(true);
        btnFetch.setText("获取模型列表");

        if (success) {
            Toast.makeText(btnFetch.getContext(), result, Toast.LENGTH_LONG).show();
            // 保存模型列表到当前正在添加的供应商
        } else {
            // 显示错误对话框
            showErrorDialog(result);
        }
    }

    private void showErrorDialog(String error) {
        // 实现错误对话框显示
    }
}
