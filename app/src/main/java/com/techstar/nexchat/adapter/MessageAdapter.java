package com.techstar.nexchat.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.model.Message;
import com.techstar.nexchat.util.FileLogger;
import com.techstar.nexchat.view.MarkdownTextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MessageAdapter";
    
    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;
    
    private Context context;
    private List<Message> messages;
    private FileLogger logger;
    private SimpleDateFormat timeFormat;
    
    private OnMessageActionListener actionListener;
    
    public interface OnMessageActionListener {
        void onCopyMessage(Message message);
        void onRegenerateMessage(Message message);
        void onDeleteMessage(Message message);
        void onBranchFromMessage(Message message);
    }
    
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }
    
    public MessageAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages;
        this.logger = FileLogger.getInstance(context);
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isUser() ? TYPE_USER : TYPE_ASSISTANT;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder.getItemViewType() == TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((AssistantMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    private class UserMessageViewHolder extends RecyclerView.ViewHolder {
        MarkdownTextView tvMessage;
        TextView tvTime;
        LinearLayout layoutActions;
        Button btnCopy;
        Button btnRegenerate;
        Button btnDelete;
        Button btnBranch;
        
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBranch = itemView.findViewById(R.id.btnBranch);
        }
        
        public void bind(final Message message) {
            tvMessage.setMarkdownText(message.getContent());
            tvTime.setText(formatTimestamp(message.getTimestamp()));
            
            // 设置操作按钮
            setupActionButtons(message);
            
            // 点击消息显示/隐藏操作按钮
            tvMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isVisible = layoutActions.getVisibility() == View.VISIBLE;
                    layoutActions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                }
            });
        }
        
        private void setupActionButtons(final Message message) {
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onCopyMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnRegenerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onRegenerateMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onDeleteMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnBranch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onBranchFromMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
        }
    }
    
    private class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvModelName;
        MarkdownTextView tvMessage;
        TextView tvTime;
        TextView tvTokens;
        LinearLayout layoutActions;
        LinearLayout layoutReasoning;
        MarkdownTextView tvReasoning;
        Button btnCopy;
        Button btnRegenerate;
        Button btnDelete;
        Button btnBranch;
        TextView tvStreamingIndicator;
        
        public AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModelName = itemView.findViewById(R.id.tvModelName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTokens = itemView.findViewById(R.id.tvTokens);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            layoutReasoning = itemView.findViewById(R.id.layoutReasoning);
            tvReasoning = itemView.findViewById(R.id.tvReasoning);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBranch = itemView.findViewById(R.id.btnBranch);
            tvStreamingIndicator = itemView.findViewById(R.id.tvStreamingIndicator);
        }
        
        public void bind(final Message message) {
            // 设置模型名称
            if (!TextUtils.isEmpty(message.getModel())) {
                tvModelName.setText(message.getModel());
                tvModelName.setVisibility(View.VISIBLE);
            } else {
                tvModelName.setVisibility(View.GONE);
            }
            
            // 设置消息内容
            tvMessage.setMarkdownText(message.getContent());
            tvTime.setText(formatTimestamp(message.getTimestamp()));
            
            // 设置 tokens 信息
            if (message.getTokens() > 0) {
                tvTokens.setText(" • " + message.getTokens() + " tokens");
                tvTokens.setVisibility(View.VISIBLE);
            } else {
                tvTokens.setVisibility(View.GONE);
            }
            
            // 设置流式指示器
            if (message.isStreaming()) {
                tvStreamingIndicator.setVisibility(View.VISIBLE);
                tvStreamingIndicator.setText("流式响应中...");
            } else if (message.isSending()) {
                tvStreamingIndicator.setVisibility(View.VISIBLE);
                tvStreamingIndicator.setText("发送中...");
            } else {
                tvStreamingIndicator.setVisibility(View.GONE);
            }
            
            // 设置思考过程
            if (message.hasReasoning() && !TextUtils.isEmpty(message.getReasoningContent())) {
                layoutReasoning.setVisibility(View.VISIBLE);
                tvReasoning.setMarkdownText(message.getReasoningContent());
                
                // 点击思考过程展开/折叠
                tvReasoning.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tvReasoning.getMaxLines() == 3) {
                            tvReasoning.setMaxLines(Integer.MAX_VALUE);
                        } else {
                            tvReasoning.setMaxLines(3);
                        }
                    }
                });
                
                // 默认显示3行
                tvReasoning.setMaxLines(3);
            } else {
                layoutReasoning.setVisibility(View.GONE);
            }
            
            // 设置操作按钮
            setupActionButtons(message);
            
            // 点击消息显示/隐藏操作按钮
            tvMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isVisible = layoutActions.getVisibility() == View.VISIBLE;
                    layoutActions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                }
            });
        }
        
        private void setupActionButtons(final Message message) {
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onCopyMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnRegenerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onRegenerateMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onDeleteMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
            
            btnBranch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionListener != null) {
                        actionListener.onBranchFromMessage(message);
                    }
                    layoutActions.setVisibility(View.GONE);
                }
            });
        }
    }
    
    private String formatTimestamp(long timestamp) {
        return timeFormat.format(new Date(timestamp));
    }
}