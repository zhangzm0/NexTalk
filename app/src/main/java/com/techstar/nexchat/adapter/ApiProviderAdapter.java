package com.techstar.nexchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.util.FileLogger;
import java.util.List;

public class ApiProviderAdapter extends RecyclerView.Adapter<ApiProviderAdapter.ViewHolder> {
    private static final String TAG = "ApiProviderAdapter";
    
    private Context context;
    private List<ApiProvider> providers;
    private FileLogger logger;
    
    private OnProviderActionListener actionListener;
    
    public interface OnProviderActionListener {
        void onAddProvider();
        void onEditProvider(ApiProvider provider);
        void onDeleteProvider(ApiProvider provider);
        void onCheckBalance(ApiProvider provider);
    }
    
    public void setOnProviderActionListener(OnProviderActionListener listener) {
        this.actionListener = listener;
    }
    
    public ApiProviderAdapter(Context context, List<ApiProvider> providers) {
        this.context = context;
        this.providers = providers;
        this.logger = FileLogger.getInstance(context);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_api_provider, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (providers.isEmpty()) {
			// 显示添加按钮
			holder.bindAddButton();
		} else {
			ApiProvider provider = providers.get(position);
			holder.bindProvider(provider);
		}
	}

	@Override
	public int getItemCount() {
		return providers.isEmpty() ? 1 : providers.size();
	}
    
    
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProviderName;
        TextView tvApiUrl;
        TextView tvModels;
        TextView tvBalance;
        Button btnBalance;
        Button btnEdit;
        Button btnDelete;
        
        View addButtonView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvApiUrl = itemView.findViewById(R.id.tvApiUrl);
            tvModels = itemView.findViewById(R.id.tvModels);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            btnBalance = itemView.findViewById(R.id.btnBalance);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            
            addButtonView = itemView;
        }
        
        // 在 ApiProviderAdapter 的 bindAddButton 方法中改进：
		public void bindAddButton() {
			// 隐藏所有 provider 相关的视图
			tvProviderName.setVisibility(View.GONE);
			tvApiUrl.setVisibility(View.GONE);
			tvModels.setVisibility(View.GONE);
			tvBalance.setVisibility(View.GONE);
			btnBalance.setVisibility(View.GONE);
			btnEdit.setVisibility(View.GONE);
			btnDelete.setVisibility(View.GONE);

			// 显示添加提示
			tvProviderName.setVisibility(View.VISIBLE);
			tvProviderName.setText("+ 添加第一个API供应商");
			tvProviderName.setTextColor(0xFF2196F3);
			tvProviderName.setTextSize(18);
			tvProviderName.setGravity(android.view.Gravity.CENTER);

			addButtonView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (actionListener != null) {
							actionListener.onAddProvider();
						}
					}
				});
		}
        
        public void bindProvider(final ApiProvider provider) {
            // 显示所有 provider 相关的视图
            tvProviderName.setVisibility(View.VISIBLE);
            tvApiUrl.setVisibility(View.VISIBLE);
            tvModels.setVisibility(View.VISIBLE);
            tvBalance.setVisibility(View.VISIBLE);
            btnBalance.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            
            tvProviderName.setText(provider.getName());
            tvApiUrl.setText(provider.getApiUrl());
            
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
                    if (actionListener != null) {
                        actionListener.onCheckBalance(provider);
                    }
                }
            });
            
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onEditProvider(provider);
                    }
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onDeleteProvider(provider);
                    }
                }
            });
        }
    }
}
