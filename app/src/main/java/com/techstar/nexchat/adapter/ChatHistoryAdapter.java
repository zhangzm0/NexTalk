package com.techstar.nexchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.util.FileLogger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private static final String TAG = "ChatHistoryAdapter";
    
    private Context context;
    private List<ChatHistory> chatHistories;
    private FileLogger logger;
    private SimpleDateFormat dateFormat;
    
    private OnItemClickListener itemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(ChatHistory chatHistory);
        void onItemLongClick(ChatHistory chatHistory);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public ChatHistoryAdapter(Context context, List<ChatHistory> chatHistories) {
        this.context = context;
        this.chatHistories = chatHistories;
        this.logger = FileLogger.getInstance(context);
        this.dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatHistory chatHistory = chatHistories.get(position);
        
        holder.tvChatTitle.setText(chatHistory.getTitle());
        holder.tvPreview.setText(chatHistory.getPreview());
        holder.tvTime.setText(formatTimestamp(chatHistory.getTimestamp()));
        holder.tvMessageCount.setText(chatHistory.getMessageCount() + "条");
        
        // 设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(chatHistory);
                }
            }
        });
        
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemLongClick(chatHistory);
                }
                return true;
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return chatHistories.size();
    }
    
    private String formatTimestamp(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatTitle;
        TextView tvPreview;
        TextView tvTime;
        TextView tvMessageCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvMessageCount = itemView.findViewById(R.id.tvMessageCount);
        }
    }
}