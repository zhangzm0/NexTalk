package com.techstar.nexchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddProviderActivity extends AppCompatActivity {

    private EditText etProviderName, etApiUrl, etApiKey;
    private Button btnFetchModels, btnSave, btnCancel;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_provider);

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

        // 模拟API调用
        btnFetchModels.postDelayed(new Runnable() {
				@Override
				public void run() {
					progressBar.setVisibility(View.GONE);
					btnFetchModels.setEnabled(true);
					btnFetchModels.setText("获取模型列表");
					Toast.makeText(AddProviderActivity.this, "成功获取3个模型", Toast.LENGTH_SHORT).show();
				}
			}, 2000);
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

        // 这里应该保存到数据库或SharedPreferences
        Toast.makeText(this, "供应商保存成功", Toast.LENGTH_SHORT).show();

        // 返回结果给上一个Activity
        setResult(RESULT_OK);
        finish();
    }
}
