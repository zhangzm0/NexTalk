package com.techstar.nexchat;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

        initToolbar();
        initRecyclerView();
        loadProviders();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
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
        // 从SharedPreferences或数据库加载已保存的供应商
        // 这里先添加一些示例数据
        ApiProvider provider1 = new ApiProvider("OpenAI", "https://api.openai.com/v1", "sk-...");
        provider1.getModels().add("gpt-3.5-turbo");
        provider1.getModels().add("gpt-4");
        providers.add(provider1);

        adapter.notifyDataSetChanged();
    }

    private void showAddProviderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加API供应商");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_provider, null);
        builder.setView(dialogView);

        final EditText etName = dialogView.findViewById(R.id.etProviderName);
        final EditText etUrl = dialogView.findViewById(R.id.etApiUrl);
        final EditText etKey = dialogView.findViewById(R.id.etApiKey);
        final Button btnFetch = dialogView.findViewById(R.id.btnFetchModels);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        builder.setPositiveButton("保存", null);
        builder.setNegativeButton("取消", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // 重写保存按钮的点击事件
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String name = etName.getText().toString().trim();
					String url = etUrl.getText().toString().trim();
					String key = etKey.getText().toString().trim();

					if (name.isEmpty() || url.isEmpty() || key.isEmpty()) {
						showError("请填写所有字段");
						return;
					}

					ApiProvider provider = new ApiProvider(name, url, key);
					providers.add(provider);
					adapter.notifyDataSetChanged();
					dialog.dismiss();
					Toast.makeText(ApiProvidersActivity.this, "供应商添加成功", Toast.LENGTH_SHORT).show();
				}
			});

        btnFetch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String url = etUrl.getText().toString().trim();
					String key = etKey.getText().toString().trim();

					if (url.isEmpty() || key.isEmpty()) {
						showError("请先填写API URL和Key");
						return;
					}

					fetchModels(url, key, progressBar, btnFetch);
				}
			});
    }

    private void fetchModels(String apiUrl, String apiKey, final ProgressBar progressBar, final Button btnFetch) {
        progressBar.setVisibility(View.VISIBLE);
        btnFetch.setEnabled(false);
        btnFetch.setText("获取中...");

        new FetchModelsTask(apiUrl, apiKey, progressBar, btnFetch, new FetchModelsTask.FetchModelsCallback() {
				@Override
				public void onSuccess(List<String> models) {
					progressBar.setVisibility(View.GONE);
					btnFetch.setEnabled(true);
					btnFetch.setText("获取模型列表");
					Toast.makeText(ApiProvidersActivity.this, "成功获取 " + models.size() + " 个模型", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onError(String error) {
					progressBar.setVisibility(View.GONE);
					btnFetch.setEnabled(true);
					btnFetch.setText("获取模型列表");
					showErrorDialog("获取模型失败", error);
				}
			}).execute();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog(String title, final String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
			.setMessage(error)
			.setPositiveButton("复制", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// 复制错误信息到剪贴板
					android.content.ClipboardManager clipboard = 
						(android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = 
						android.content.ClipData.newPlainText("错误信息", error);
					clipboard.setPrimaryClip(clip);
					Toast.makeText(ApiProvidersActivity.this, "已复制错误信息", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton("确定", null)
			.show();
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
            if (providers.get(position).getId() != null && 
                providers.get(position).getId().equals("add_new")) {
                return TYPE_ADD_NEW;
            }
            return TYPE_PROVIDER;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ADD_NEW) {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
                return new AddNewViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_api_provider, parent, false);
                return new ProviderViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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
						showAddProviderDialog();
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
        // 实现编辑逻辑
        showErrorDialog("编辑功能", "编辑功能尚未实现");
    }

    private void deleteProvider(final ApiProvider provider) {
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除" + provider.getName() + "吗？")
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    providers.remove(provider);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ApiProvidersActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
