package com.techstar.nexchat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.database.ApiProviderDao;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class ApiProvidersActivity extends AppCompatActivity {
    private static final String TAG = "ApiProvidersActivity";
    
    private RecyclerView recyclerView;
    private ApiProviderAdapter adapter;
    private List<ApiProvider> providers;
    private ApiProviderDao apiProviderDao;
    private FileLogger logger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_providers);
        
        logger = FileLogger.getInstance(this);
        apiProviderDao = new ApiProviderDao(this);
        
        findViewById(R.id.btnApiBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        initRecyclerView();
        loadProviders();
    }
    
    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewProviders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        providers = new ArrayList<ApiProvider>();
        adapter = new ApiProviderAdapter(providers);
        recyclerView.setAdapter(adapter);
        
        // 添加"新建"项
        providers.add(0, createAddNewItem());
        adapter.notifyDataSetChanged();
    }
    
    private ApiProvider createAddNewItem() {
        ApiProvider addNew = new ApiProvider();
        addNew.setId(-1); // 特殊ID表示添加按钮
        addNew.setName("添加新的API供应商");
        return addNew;
    }
    
    private void loadProviders() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ApiProvider> savedProviders = apiProviderDao.getAllProviders();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        providers.clear();
                        providers.add(createAddNewItem());
                        providers.addAll(savedProviders);
                        adapter.notifyDataSetChanged();
                        logger.i(TAG, "Loaded " + savedProviders.size() + " providers");
                    }
                });
            }
        }).start();
    }
    
    private void startAddProviderActivity() {
        try {
            Intent intent = new Intent(this, AddProviderActivity.class);
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "打开添加页面失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            logger.e(TAG, "Failed to open AddProviderActivity", e);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadProviders();
            android.widget.Toast.makeText(this, "供应商添加成功", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private class ApiProviderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_ADD_NEW = 0;
        private static final int TYPE_PROVIDER = 1;
        private List<ApiProvider> providers;
        
        public ApiProviderAdapter(List<ApiProvider> providers) {
            this.providers = providers;
        }
        
        @Override
        public int getItemViewType(int position) {
            ApiProvider provider = providers.get(position);
            if (provider.getId() == -1) {
                return TYPE_ADD_NEW;
            }
            return TYPE_PROVIDER;
        }
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_ADD_NEW) {
                View view = inflater.inflate(R.layout.item_add_provider, parent, false);
                return new AddNewViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_api_provider, parent, false);
                return new ProviderViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_ADD_NEW) {
                ((AddNewViewHolder) holder).bind();
            } else {
                ((ProviderViewHolder) holder).bind(providers.get(position));
            }
        }
        
        @Override
        public int getItemCount() {
            return providers.size();
        }
    }
    
    private class AddNewViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        
        public AddNewViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvAddNew);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAddProviderActivity();
                }
            });
        }
        
        public void bind() {
            textView.setText("+ 添加新的API供应商");
        }
    }
    
    private class ProviderViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUrl, tvModels, tvBalance;
        Button btnEdit, btnDelete, btnBalance;
        
        public ProviderViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProviderName);
            tvUrl = itemView.findViewById(R.id.tvApiUrl);
            tvModels = itemView.findViewById(R.id.tvModels);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBalance = itemView.findViewById(R.id.btnBalance);
        }
        
        public void bind(final ApiProvider provider) {
            tvName.setText(provider.getName());
            tvUrl.setText(provider.getApiUrl());
            
            if (provider.getModels() != null) {
                tvModels.setText("模型: " + provider.getModels().size() + "个");
            } else {
                tvModels.setText("模型: 0个");
            }
            
            if (provider.getBalance() != null) {
                tvBalance.setText("余额: " + provider.getBalance());
            } else {
                tvBalance.setText("余额: 未查询");
            }
            
            btnBalance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchBalance(provider);
                }
            });
            
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editProvider(provider);
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteProvider(provider);
                }
            });
        }
    }
    
    private void editProvider(ApiProvider provider) {
        Intent intent = new Intent(this, AddProviderActivity.class);
        intent.putExtra("provider_id", provider.getId());
        startActivityForResult(intent, 1);
    }
    
    private void fetchBalance(final ApiProvider provider) {
        // 余额查询功能
        provider.setBalance("查询中...");
        adapter.notifyDataSetChanged();
        
        android.widget.Toast.makeText(this, "余额查询功能开发中", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void deleteProvider(final ApiProvider provider) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除" + provider.getName() + "吗？")
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
                            providers.remove(provider);
                            adapter.notifyDataSetChanged();
                            android.widget.Toast.makeText(ApiProvidersActivity.this, "已删除", android.widget.Toast.LENGTH_SHORT).show();
                            logger.i(TAG, "Deleted provider: " + provider.getName());
                        }
                    });
                }
            }
        }).start();
    }
}