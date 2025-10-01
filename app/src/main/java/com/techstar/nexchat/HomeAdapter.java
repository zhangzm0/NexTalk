package com.techstar.nexchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ChatConversation;
import com.techstar.nexchat.model.HomeMenuItem;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MENU_ITEM = 0;
    private static final int TYPE_CHAT_HISTORY = 1;
    private static final int TYPE_SEPARATOR = 2;

    private List<Object> items;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
        void onConversationLongClick(ChatConversation conversation);
    }

    public HomeAdapter(List<Object> items, OnConversationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof HomeMenuItem) {
            return TYPE_MENU_ITEM;
        } else if (item instanceof ChatConversation) {
            return TYPE_CHAT_HISTORY;
        } else if (item instanceof String && "分隔符".equals(item)) {
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
                View defaultView = inflater.inflate(R.layout.item_home_menu, parent, false);
                return new MenuItemViewHolder(defaultView);
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
                ((ChatHistoryViewHolder) holder).bind((ChatConversation) item);
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
    private class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvChatTitle, tvPreview, tvTime, tvMessageCount;
        

        public ChatHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvMessageCount = itemView.findViewById(R.id.tvMessageCount);
            
        }

        public void bind(final ChatConversation conversation) {
            tvChatTitle.setText(conversation.getTitle());
            tvPreview.setText(conversation.getPreview());
            tvTime.setText(conversation.getFormattedTime());
            tvMessageCount.setText(conversation.getMessageCount() + "条");

            

            // 点击进入对话
            itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onConversationClick(conversation);
						}
					}
				});

            // 长按显示操作菜单
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						if (listener != null) {
							listener.onConversationLongClick(conversation);
						}
						return true;
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

