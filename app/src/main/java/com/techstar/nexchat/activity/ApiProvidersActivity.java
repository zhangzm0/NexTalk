package com.techstar.nexchat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.adapter.ApiProviderAdapter;
import com.techstar.nexchat.database.ApiProviderDao;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class ApiProvidersActivity extends AppCompatActivity {
    private static final String TAG = "ApiProvidersActivity";
    
    private ImageButton btnApiBack;
    private RecyclerView recyclerViewProviders;
    private ApiProviderAdapter adapter;
    private ApiProviderDao apiProviderDao;
    private FileLogger logger;
    
    private List<ApiProvider> providers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_providers);
        
        logger = FileLogger.getInstance(this);
        apiProviderDao = new ApiProviderDao(this);
        
        logger.i(TAG, "ApiProvidersActivity created");
        
        initViews();
        loadProviders();
    }
    
    private void initViews() {
        btnApiBack = findViewById(R.id.btnApiBack);
        recyclerViewProviders = findViewById(R.id.recyclerViewProviders);
        
        providers = new ArrayList<>();
        adapter = new ApiProviderAdapter(this, providers);
        recyclerViewProviders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewProviders.setAdapter(adapter);
        
        btnApiBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // 设置适配器监听
        adapter.setOnProviderActionListener(new ApiProviderAdapter.OnProviderActionListener() {
            @Override
            public void onAddProvider() {
                openAddProvider();
            }
            
            @Override
            public void onEditProvider(ApiProvider provider) {
                editProvider(provider);
            }
            
            @Override
            public void onDeleteProvider(ApiProvider provider) {
                deleteProvider(provider);
            }
            
            @Override
            public void onCheckBalance(ApiProvider provider) {
                checkBalance(provider);
            }
        });
    }
    
    private void loadProviders() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ApiProvider> providerList = apiProviderDao.getAllProviders();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        providers.clear();
                        providers.addAll(providerList);
                        adapter.notifyDataSetChanged();
                        logger.i(TAG, "Loaded " + providers.size() + " API providers");
                    }
                });
            }
        }).start();
    }
    
    private void openAddProvider() {
        logger.i(TAG, "Opening add provider");
        Intent intent = new Intent(this, AddProviderActivity.class);
        startActivity(intent);
    }
    
    private void editProvider(ApiProvider provider) {
        logger.i(TAG, "Editing provider: " + provider.getName());
        Intent intent = new Intent(this, AddProviderActivity.class);
        intent.putExtra("provider_id", provider.getId());
        startActivity(intent);
    }
    
    private void deleteProvider(final ApiProvider provider) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("删除供应商")
                .setMessage("确定要删除供应商 \"" + provider.getName() + "\" 吗？")
                .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        performDeleteProvider(provider);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void performDeleteProvider(final ApiProvider provider) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = apiProviderDao.deleteProvider(provider.getId());
                if (success) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int position = providers.indexOf(provider);
                            if (position != -1) {
                                providers.remove(position);
                                adapter.notifyItemRemoved(position);
                                logger.i(TAG, "Deleted provider: " + provider.getName());
                            }
                        }
                    });
                }
            }
        }).start();
    }
    
    private void checkBalance(final ApiProvider provider) {
        logger.i(TAG, "Checking balance for: " + provider.getName());
        // 这里实现查询余额的逻辑
        // 暂时显示提示
        android.widget.Toast.makeText(this, "查询余额功能开发中", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载数据
        loadProviders();
    }
}