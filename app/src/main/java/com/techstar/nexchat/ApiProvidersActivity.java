package com.techstar.nexchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ApiProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

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
        providers.clear();

        // 添加"添加新供应商"项
        providers.add(createAddNewItem());

        // 从SharedPreferences加载已保存的供应商
        List<ApiProvider> savedProviders = loadProvidersFromPrefs();
        providers.addAll(savedProviders);

        adapter.notifyDataSetChanged();
    }

    private List<ApiProvider> loadProvidersFromPrefs() {
        List<ApiProvider> providerList = new ArrayList<>();

        try {
            String providerIds = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getString("provider_ids", "");

            if (!providerIds.isEmpty()) {
                String[] ids = providerIds.split(",");
                for (String id : ids) {
                    ApiProvider provider = loadProviderById(id);
                    if (provider != null) {
                        providerList.add(provider);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return providerList;
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
            tvModels.setText("模型: " + provider.getModels().size() + "个");
            tvBalance.setText("余额: " + provider.getBalance());
            
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
        // 显示加载中
        provider.setBalance("查询中...");
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        // 构建余额查询请求（OpenAI格式）
        String balanceUrl = provider.getApiUrl().endsWith("/") ? 
            provider.getApiUrl() + "dashboard/billing/credit_grants" :
            provider.getApiUrl() + "/dashboard/billing/credit_grants";
            
        Request request = new Request.Builder()
            .url(balanceUrl)
            .addHeader("Authorization", "Bearer " + provider.getApiKey())
            .build();
            
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        provider.setBalance("查询失败");
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        Toast.makeText(ApiProvidersActivity.this, "余额查询失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (response.isSuccessful()) {
                                JSONObject json = new JSONObject(responseBody);
                                if (json.has("total_available")) {
                                    double balance = json.getDouble("total_available");
                                    provider.setBalance(String.format("$%.2f", balance));
                                } else if (json.has("total_granted") && json.has("total_used")) {
                                    double granted = json.getDouble("total_granted");
                                    double used = json.getDouble("total_used");
                                    double available = granted - used;
                                    provider.setBalance(String.format("$%.2f", available));
                                } else {
                                    provider.setBalance("格式不支持");
                                }
                            } else {
                                // 尝试其他API格式
                                try {
                                    JSONObject json = new JSONObject(responseBody);
                                    if (json.has("available")) {
                                        double balance = json.getDouble("available");
                                        provider.setBalance(String.format("$%.2f", balance));
                                    } else {
                                        provider.setBalance("API错误: " + response.code());
                                    }
                                } catch (Exception e) {
                                    provider.setBalance("查询失败: " + response.code());
                                }
                            }
                        } catch (Exception e) {
                            provider.setBalance("解析失败");
                        }
                        
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        
                        // 保存余额信息
                        saveProviderBalance(provider.getId(), provider.getBalance());
                    }
                });
            }
        });
    }
    
    private void saveProviderBalance(String providerId, String balance) {
        getSharedPreferences("api_providers", MODE_PRIVATE)
            .edit()
            .putString(providerId + "_balance", balance)
            .apply();
    }
    
    // 修改加载方法，加载保存的余额信息
    private ApiProvider loadProviderById(String providerId) {
        try {
            String name = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getString(providerId + "_name", "");
            String url = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getString(providerId + "_url", "");
            String key = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getString(providerId + "_key", "");
            String balance = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getString(providerId + "_balance", "未查询");
            int modelCount = getSharedPreferences("api_providers", MODE_PRIVATE)
                .getInt(providerId + "_model_count", 0);
                
            if (!name.isEmpty()) {
                ApiProvider provider = new ApiProvider(name, url, key);
                provider.setId(providerId);
                provider.setBalance(balance);
                
                // 加载模型列表
                for (int i = 0; i < modelCount; i++) {
                    String model = getSharedPreferences("api_providers", MODE_PRIVATE)
                        .getString(providerId + "_model_" + i, "");
                    if (!model.isEmpty()) {
                        provider.getModels().add(model);
                    }
                }
                
                return provider;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	

		private void deleteProvider(final ApiProvider provider) {
			new AlertDialog.Builder(this)
				.setTitle("确认删除")
				.setMessage("确定要删除" + provider.getName() + "吗？")
				.setPositiveButton("删除", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 从列表中移除
						providers.remove(provider);

						// 从SharedPreferences中删除数据
						deleteProviderFromPrefs(provider.getId());

						// 更新适配器
						adapter.notifyDataSetChanged();

						// 发送广播通知其他页面更新
						sendProviderUpdateBroadcast();

						Toast.makeText(ApiProvidersActivity.this, "已删除", Toast.LENGTH_SHORT).show();

						// 检查是否删除了当前选择的供应商
						checkCurrentModelSelection(provider.getId());
					}
				})
				.setNegativeButton("取消", null)
				.show();
		}

		// 从SharedPreferences中彻底删除供应商数据
		private void deleteProviderFromPrefs(String providerId) {
			try {
				// 删除基本信息
				getSharedPreferences("api_providers", MODE_PRIVATE)
					.edit()
					.remove(providerId + "_name")
					.remove(providerId + "_url")
					.remove(providerId + "_key")
					.remove(providerId + "_balance")
					.remove(providerId + "_model_count")
					.apply();

				// 删除所有模型数据
				int modelCount = getSharedPreferences("api_providers", MODE_PRIVATE)
					.getInt(providerId + "_model_count", 0);
				for (int i = 0; i < modelCount; i++) {
					getSharedPreferences("api_providers", MODE_PRIVATE)
						.edit()
						.remove(providerId + "_model_" + i)
						.apply();
				}

				// 从供应商ID列表中移除
				String existingIds = getSharedPreferences("api_providers", MODE_PRIVATE)
					.getString("provider_ids", "");
				if (!existingIds.isEmpty()) {
					List<String> idList = new ArrayList<>(Arrays.asList(existingIds.split(",")));
					idList.remove(providerId);
					String newIds = TextUtils.join(",", idList);
					getSharedPreferences("api_providers", MODE_PRIVATE)
						.edit()
						.putString("provider_ids", newIds)
						.apply();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 发送广播通知其他页面更新
		private void sendProviderUpdateBroadcast() {
			Intent intent = new Intent("com.techstar.nexchat.PROVIDERS_UPDATED");
			sendBroadcast(intent);
		}

		// 检查并更新当前模型选择
		private void checkCurrentModelSelection(String deletedProviderId) {
			try {
				String currentProviderId = getSharedPreferences("app_settings", MODE_PRIVATE)
					.getString("last_provider_id", "");
				String currentModel = getSharedPreferences("app_settings", MODE_PRIVATE)
					.getString("last_model", "");

				// 如果删除的正是当前选择的供应商，清除选择
				if (deletedProviderId.equals(currentProviderId)) {
					getSharedPreferences("app_settings", MODE_PRIVATE)
						.edit()
						.remove("last_provider_id")
						.remove("last_model")
						.apply();

					Toast.makeText(this, "已删除当前选择的供应商，请重新选择模型", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
