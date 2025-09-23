package com.techstar.nexchat;

import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ProgressBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FetchModelsTask extends AsyncTask<Void, Void, FetchModelsTask.Result> {
    
    public interface FetchModelsCallback {
        void onSuccess(List<String> models);
        void onError(String error);
    }
    
    public static class Result {
        public boolean success;
        public List<String> models;
        public String error;
        
        public Result(boolean success, List<String> models, String error) {
            this.success = success;
            this.models = models;
            this.error = error;
        }
    }
    
    private String apiUrl;
    private String apiKey;
    private ProgressBar progressBar;
    private Button btnFetch;
    private FetchModelsCallback callback;
    
    public FetchModelsTask(String apiUrl, String apiKey, ProgressBar progressBar, Button btnFetch, FetchModelsCallback callback) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.progressBar = progressBar;
        this.btnFetch = btnFetch;
        this.callback = callback;
    }
    
    @Override
    protected Result doInBackground(Void... voids) {
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
                List<String> models = new ArrayList<>();
                
                for (int i = 0; i < data.length(); i++) {
                    JSONObject model = data.getJSONObject(i);
                    String modelId = model.getString("id");
                    models.add(modelId);
                }
                return new Result(true, models, null);
            } else {
                return new Result(false, null, "HTTP " + response.code() + ": " + responseBody);
            }
            
        } catch (Exception e) {
            return new Result(false, null, "异常: " + e.getMessage());
        }
    }
    
    @Override
    protected void onPostExecute(Result result) {
        if (result.success) {
            if (callback != null) {
                callback.onSuccess(result.models);
            }
        } else {
            if (callback != null) {
                callback.onError(result.error);
            }
        }
    }
}
