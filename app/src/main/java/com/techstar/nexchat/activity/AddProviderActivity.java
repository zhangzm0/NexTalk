package com.techstar.nexchat.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.techstar.nexchat.R;
import com.techstar.nexchat.database.ApiProviderDao;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.service.ApiClient;
import com.techstar.nexchat.util.FileLogger;
import java.util.List;

public class AddProviderActivity extends AppCompatActivity {
    private static final String TAG = "AddProviderActivity";
    
    private ImageButton btnBack;
    private TextView tvTitle;
    private EditText etProviderName;
    private EditText etApiUrl;
    private EditText etApiKey;
    private Button btnFetchModels;
    private Button btnCancel;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvModelsTitle;
    private LinearLayout modelsLayout;
    
    private ApiProviderDao apiProviderDao;
    private FileLogger logger;
    private ApiClient apiClient;
    
    private ApiProvider editingProvider;
    private List<String> fetchedModels;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_provider);
        
        logger = FileLogger.getInstance(this);
        apiProviderDao = new ApiProviderDao(this);
        apiClient = new ApiClient(this);
        
        logger.i(TAG, "AddProviderActivity created");
        
        initViews();
        loadEditingProvider();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        etProviderName = findViewById(R.id.etProviderName);
        etApiUrl = findViewById(R.id.etApiUrl);
        etApiKey = findViewById(R.id.etApiKey);
        btnFetchModels = findViewById(R.id.btnFetchModels);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tvModelsTitle = findViewById(R.id.tvModelsTitle);
        modelsLayout = findViewById(R.id.modelsLayout);
        
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnFetchModels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchModels();
            }
        });
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProvider();
            }
        });
    }
    
    private void loadEditingProvider() {
        int providerId = getIntent().getIntExtra("provider_id", -1);
        if (providerId != -1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    editingProvider = apiProviderDao.getProviderById(providerId);
                    if (editingProvider != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                populateForm(editingProvider);
                            }
                        });
                    }
                }
            }).start();
        }
    }
    
    private void populateForm(ApiProvider provider) {
        tvTitle.setText("编辑API供应商");
        etProviderName.setText(provider.getName());
        etApiUrl.setText(provider.getApiUrl());
        etApiKey.setText(provider.getApiKey());
        
        if (provider.getModels() != null) {
            fetchedModels = provider.getModels();
            updateModelsDisplay();
        }
    }
    
    private void fetchModels() {
        String providerName = etProviderName.getText().toString().trim();
        String apiUrl = etApiUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        
        if (TextUtils.isEmpty(providerName)) {
            showError("请输入供应商名称");
            return;
        }
        
        if (TextUtils.isEmpty(apiUrl)) {
            showError("请输入API URL");
            return;
        }
        
        if (TextUtils.isEmpty(apiKey)) {
            showError("请输入API Key");
            return;
        }
        
        // 创建临时 provider 用于获取模型
        ApiProvider tempProvider = new ApiProvider(providerName, apiUrl, apiKey);
        
        showProgress(true);
        
        apiClient.fetchModels(tempProvider, new ApiClient.ModelsCallback() {
            @Override
            public void onSuccess(List<String> models) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        fetchedModels = models;
                        updateModelsDisplay();
                        logger.i(TAG, "Fetched " + models.size() + " models");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        showError("获取模型失败: " + error);
                        logger.e(TAG, "Failed to fetch models: " + error);
                    }
                });
            }
        });
    }
    
    private void updateModelsDisplay() {
        if (fetchedModels == null || fetchedModels.isEmpty()) {
            tvModelsTitle.setText("获取到的模型: 0个");
            modelsLayout.removeAllViews();
            return;
        }
        
        tvModelsTitle.setText("获取到的模型: " + fetchedModels.size() + "个");
        modelsLayout.removeAllViews();
        
        for (String model : fetchedModels) {
            TextView modelView = new TextView(this);
            modelView.setText("• " + model);
            modelView.setTextColor(0xFFE0E0E0);
            modelView.setTextSize(14);
            modelView.setPadding(0, 4, 0, 4);
            modelsLayout.addView(modelView);
        }
    }
    
    private void saveProvider() {
		String providerName = etProviderName.getText().toString().trim();
		String apiUrl = etApiUrl.getText().toString().trim();
		String apiKey = etApiKey.getText().toString().trim();

		if (TextUtils.isEmpty(providerName)) {
			showError("请输入供应商名称");
			return;
		}

		if (TextUtils.isEmpty(apiUrl)) {
			showError("请输入API URL");
			return;
		}

		if (TextUtils.isEmpty(apiKey)) {
			showError("请输入API Key");
			return;
		}

		if (fetchedModels == null || fetchedModels.isEmpty()) {
			showError("请先获取模型列表");
			return;
		}

		final ApiProvider provider;
		if (editingProvider != null) {
			provider = editingProvider;
			provider.setName(providerName);
			provider.setApiUrl(apiUrl);
			provider.setApiKey(apiKey);
			provider.setModels(fetchedModels);
		} else {
			provider = new ApiProvider(providerName, apiUrl, apiKey);
			provider.setModels(fetchedModels);
		}

		new Thread(new Runnable() {
				@Override
				public void run() {
					boolean success;
					if (editingProvider != null) {
						success = apiProviderDao.updateProvider(provider);
					} else {
						long id = apiProviderDao.insertProvider(provider);
						success = id != -1;
					}

					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (success) {
									logger.i(TAG, "Saved provider: " + provider.getName());
									// 设置结果并关闭
									setResult(RESULT_OK);
									finish();
								} else {
									showError("保存失败");
								}
							}
						});
				}
			}).start();
	}
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnFetchModels.setEnabled(!show);
    }
    
    private void showError(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
