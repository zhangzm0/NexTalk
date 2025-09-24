package com.techstar.nexchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ApiProvider;
import java.util.ArrayList;
import java.util.List;

public class ApiProvidersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiProviderAdapter adapter;
    private List<ApiProvider> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_providers);

        // 设置返回按钮
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

        // 添加第一个项目作为"添加新供应商"
        providers.add(0, createAddNewItem());
        adapter.notifyDataSetChanged();
    }

    private ApiProvider createAddNewItem() {
        ApiProvider addNew = new ApiProvider();
        addNew.setId("add_new");
        addNew.setName("添加新的API供应商");
        return addNew;
    }

    private void loadProviders() {
        // 从数据库加载已保存的供应商
        // 这里先添加一些示例数据
        ApiProvider provider1 = new ApiProvider("OpenAI", "https://api.openai.com/v1", "sk-...");
        provider1.getModels().add("gpt-3.5-turbo");
        provider1.getModels().add("gpt-4");
        providers.add(provider1);

        adapter.notifyDataSetChanged();
    }

    private void startAddProviderActivity() {
        try {
            Intent intent = new Intent(this, AddProviderActivity.class);
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            Toast.makeText(this, "打开添加页面失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 刷新供应商列表
            loadProviders();
            Toast.makeText(this, "供应商添加成功", Toast.LENGTH_SHORT).show();
        }
    }

    // RecyclerView Adapter
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
            if (provider.getId() != null && provider.getId().equals("add_new")) {
                return TYPE_ADD_NEW;
            }
            return TYPE_PROVIDER;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_ADD_NEW) {
                View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
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
            textView = (TextView) itemView;
            textView.setTextColor(0xFF2196F3);
            textView.setPadding(32, 32, 32, 32);

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
        TextView tvName, tvUrl, tvModels;
        Button btnEdit, btnDelete;

        public ProviderViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProviderName);
            tvUrl = itemView.findViewById(R.id.tvApiUrl);
            tvModels = itemView.findViewById(R.id.tvModels);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(final ApiProvider provider) {
            tvName.setText(provider.getName());
            tvUrl.setText(provider.getApiUrl());
            tvModels.setText("模型: " + provider.getModels().size() + "个");

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
        Toast.makeText(this, "编辑功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void deleteProvider(final ApiProvider provider) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除" + provider.getName() + "吗？")
            .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    providers.remove(provider);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ApiProvidersActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
