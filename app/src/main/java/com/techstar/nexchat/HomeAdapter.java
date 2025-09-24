package com.techstar.nexchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.model.HomeMenuItem;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MENU_ITEM = 0;
    private static final int TYPE_CHAT_HISTORY = 1;
    private static final int TYPE_SEPARATOR = 2;

    private List<Object> items;

    public HomeAdapter(List<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof HomeMenuItem) {
            return TYPE_MENU_ITEM;
        } else if (item instanceof ChatHistory) {
            return TYPE_CHAT_HISTORY;
        } else if (item instanceof String && ((String) item).equals("分隔符")) {
            return TYPE_SEPARATOR;
        }
        return TYPE_MENU_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_MENU_ITEM:
                View menuView = inflater.inflate(R.layout.item_home_menu, parent, false);
                return new MenuItemViewHolder(menuView);

            case TYPE_CHAT_HISTORY:
                View historyView = inflater.inflate(R.layout.item_chat_history, parent, false);
                return new ChatHistoryViewHolder(historyView);

            case TYPE_SEPARATOR:
                View separatorView = inflater.inflate(R.layout.item_separator, parent, false);
                return new SeparatorViewHolder(separatorView);

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_MENU_ITEM:
                ((MenuItemViewHolder) holder).bind((HomeMenuItem) item);
                break;

            case TYPE_CHAT_HISTORY:
                ((ChatHistoryViewHolder) holder).bind((ChatHistory) item);
                break;

            case TYPE_SEPARATOR:
                ((SeparatorViewHolder) holder).bind();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    
    // 菜单项ViewHolder
	private static class MenuItemViewHolder extends RecyclerView.ViewHolder {
		private ImageView icon;
		private TextView title;

		public MenuItemViewHolder(@NonNull View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
		}

		public void bind(final HomeMenuItem menuItem) {
			// 使用app:srcCompat来支持矢量图
			icon.setImageResource(menuItem.getIconRes());
			title.setText(menuItem.getTitle());

			itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (menuItem.getOnClickListener() != null) {
							menuItem.getOnClickListener().onClick(v);
						}
					}
				});
		}
	}

    // 聊天历史ViewHolder
    private static class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvChatTitle, tvPreview, tvTime;

        public ChatHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(final ChatHistory chatHistory) {
            tvChatTitle.setText(chatHistory.getTitle());
            tvPreview.setText(chatHistory.getPreview());
            tvTime.setText(chatHistory.getFormattedTime());

            itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// 加载选中的聊天记录
						// 这里需要实现加载聊天逻辑
					}
				});
        }
    }

    // 分隔符ViewHolder
    private static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        private View separator;

        public SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
            separator = itemView.findViewById(R.id.separator);
        }

        public void bind() {
            // 分隔符不需要特殊处理
        }
    }
}
