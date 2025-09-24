package com.techstar.nexchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddProviderActivity extends AppCompatActivity {

    private EditText etProviderName, etApiUrl, etApiKey;
    private Button btnFetchModels, btnSave, btnCancel;
    private ProgressBar progressBar;
    private LinearLayout modelsLayout;
    private TextView tvModelsTitle;

    private List<String> fetchedModels = new ArrayList<>();
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_provider);

        client = new OkHttpClient();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etProviderName = findViewById(R.id.etProviderName);
        etApiUrl = findViewById(R.id.etApiUrl);
        etApiKey = findViewById(R.id.etApiKey);
        btnFetchModels = findViewById(R.id.btnFetchModels);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
        modelsLayout = findViewById(R.id.modelsLayout);
        tvModelsTitle = findViewById(R.id.tvModelsTitle);

        // 设置返回按钮
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
    }

    private void setupClickListeners() {
        btnFetchModels.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					fetchModels();
				}
			});

        btnSave.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveProvider();
				}
			});

        btnCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
    }

    private void fetchModels() {
        String url = etApiUrl.getText().toString().trim();
        String key = etApiKey.getText().toString().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "请先填写API URL和Key", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnFetchModels.setEnabled(false);
        btnFetchModels.setText("获取中...");
        clearModelsDisplay();

        // 构建请求
        String apiUrl = url.endsWith("/") ? url + "models" : url + "/models";

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer " + key)
            .addHeader("Content-Type", "application/json")
            .build();

        client.newCall(request).enqueue(new Callback() {
				@Override
				public void onFailure(Call call, final IOException e) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);
								btnFetchModels.setEnabled(true);
								btnFetchModels.setText("获取模型列表");
								showError("网络请求失败: " + e.getMessage());
							}
						});
				}

				@Override
				public void onResponse(Call call, final Response response) throws IOException {
					final String responseBody = response.body().string();
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisibility(View.GONE);
								btnFetchModels.setEnabled(true);
								btnFetchModels.setText("获取模型列表");

								if (response.isSuccessful()) {
									try {
										parseModelsResponse(responseBody);
									} catch (Exception e) {
										showError("解析响应失败: " + e.getMessage());
									}
								} else {
									showError("API请求失败: " + response.code() + " - " + responseBody);
								}
							}
						});
				}
			});
    }

    private void parseModelsResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray data = json.getJSONArray("data");

            fetchedModels.clear();
            for (int i = 0; i < data.length(); i++) {
                JSONObject model = data.getJSONObject(i);
                String modelId = model.getString("id");
                fetchedModels.add(modelId);
            }

            displayModelsList();
            Toast.makeText(this, "成功获取 " + fetchedModels.size() + " 个模型", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            // 尝试其他格式（如Azure OpenAI）
            try {
                JSONArray modelsArray = new JSONArray(responseBody);
                fetchedModels.clear();
                for (int i = 0; i < modelsArray.length(); i++) {
                    JSONObject model = modelsArray.getJSONObject(i);
                    if (model.has("model")) {
                        fetchedModels.add(model.getString("model"));
                    }
                }
                displayModelsList();
                Toast.makeText(this, "成功获取 " + fetchedModels.size() + " 个模型", Toast.LENGTH_SHORT).show();
            } catch (Exception e2) {
                showError("不支持的响应格式: " + e2.getMessage());
            }
        }
    }

    private void displayModelsList() {
        clearModelsDisplay();

        if (fetchedModels.isEmpty()) {
            tvModelsTitle.setText("获取到的模型: 0个");
            return;
        }

        tvModelsTitle.setText("获取到的模型: " + fetchedModels.size() + "个");

        for (String model : fetchedModels) {
            TextView modelView = new TextView(this);
            modelView.setText("• " + model);
            modelView.setTextColor(0xFFCCCCCC);
            modelView.setTextSize(12);
            modelView.setPadding(0, 4, 0, 4);
            modelsLayout.addView(modelView);
        }
    }

    private void clearModelsDisplay() {
        modelsLayout.removeAllViews();
        tvModelsTitle.setText("获取到的模型: 0个");
    }

    private void saveProvider() {
        String name = etProviderName.getText().toString().trim();
        String url = etApiUrl.getText().toString().trim();
        String key = etApiKey.getText().toString().trim();

        if (name.isEmpty()) {
            etProviderName.setError("请输入供应商名称");
            return;
        }

        if (url.isEmpty()) {
            etApiUrl.setError("请输入API URL");
            return;
        }

        if (key.isEmpty()) {
            etApiKey.setError("请输入API Key");
            return;
        }

        if (fetchedModels.isEmpty()) {
            Toast.makeText(this, "请先获取模型列表", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存到SharedPreferences
        saveProviderToPrefs(name, url, key, fetchedModels);

        Toast.makeText(this, "供应商保存成功", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void saveProviderToPrefs(String name, String url, String key, List<String> models) {
        // 生成唯一ID
        String providerId = "provider_" + System.currentTimeMillis();

        // 保存基本信息
        getSharedPreferences("api_providers", MODE_PRIVATE)
            .edit()
            .putString(providerId + "_name", name)
            .putString(providerId + "_url", url)
            .putString(providerId + "_key", key)
            .putInt(providerId + "_model_count", models.size())
            .apply();

        // 保存模型列表
        for (int i = 0; i < models.size(); i++) {
            getSharedPreferences("api_providers", MODE_PRIVATE)
                .edit()
                .putString(providerId + "_model_" + i, models.get(i))
                .apply();
        }

        // 保存供应商ID列表
        String existingIds = getSharedPreferences("api_providers", MODE_PRIVATE)
            .getString("provider_ids", "");
        if (!existingIds.isEmpty()) {
            existingIds += "," + providerId;
        } else {
            existingIds = providerId;
        }
        getSharedPreferences("api_providers", MODE_PRIVATE)
            .edit()
            .putString("provider_ids", existingIds)
            .apply();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
