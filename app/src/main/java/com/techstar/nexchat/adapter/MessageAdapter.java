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
import com.techstar.nexchat.model.StreamingMessage;
import com.techstar.nexchat.service.UIUpdateCoordinator;
import com.techstar.nexchat.util.FileLogger;
import com.techstar.nexchat.view.MarkdownTextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    }
    
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
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
    
    // 在 MessageAdapter.java 中添加以下方法

// 添加成员变量
	private Map<Integer, StreamingMessage> streamingMessages;

// 在 MessageAdapter.java 中添加：
	public MessageAdapter(Context context, List<Message> messages) {
		this(context, messages, -1); // 使用-1作为默认chatId
	}

// 添加新的构造方法支持chatId
	private int chatId;
	// 在 MessageAdapter.java 中修改

	private android.os.Handler handler;

// 修改构造函数
	public MessageAdapter(Context context, List<Message> messages, int chatId) {
		this.context = context;
		this.messages = messages;
		this.logger = FileLogger.getInstance(context);
		this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
		this.chatId = chatId;
		this.handler = new android.os.Handler(android.os.Looper.getMainLooper());

		// 注册到UI协调器
		UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
		uiCoordinator.registerAdapter(chatId, this);
	}

// 修改更新方法
	public void updateMessageContent(int messageId) {
		handler.post(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < messages.size(); i++) {
						if (messages.get(i).getId() == messageId) {
							notifyItemChanged(i);
							logger.d(TAG, "Updated streaming message at position: " + i);
							break;
						}
					}
				}
			});
	}



// 在 onBindViewHolder 中处理流式消息
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Message message = messages.get(position);

		// 检查是否为流式消息
		UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
		StreamingMessage streamingMessage = uiCoordinator.getStreamingMessage(message.getId());

		Message displayMessage = message;
		if (streamingMessage != null) {
			displayMessage = streamingMessage.getMessage();
			// 更新消息内容为流式内容
			displayMessage.setContent(streamingMessage.getContent());
		}

		if (holder.getItemViewType() == TYPE_USER) {
			((UserMessageViewHolder) holder).bind(displayMessage);
		} else {
			((AssistantMessageViewHolder) holder).bind(displayMessage);
		}
	}

// 添加清理方法
	public void cleanup() {
		if (chatId != -1) {
			UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
			uiCoordinator.unregisterAdapter(chatId);
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
        
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
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
        }
    }
    
    private class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvModelName;
        MarkdownTextView tvMessage;
        TextView tvTime;
        TextView tvTokens;
        LinearLayout layoutActions;
        Button btnCopy;
        Button btnRegenerate;
        Button btnDelete;
        
        // 在 AssistantMessageViewHolder 中添加
		

// 在构造函数中初始化
		public AssistantMessageViewHolder(@NonNull View itemView) {
			super(itemView);
			tvModelName = itemView.findViewById(R.id.tvModelName);
			tvMessage = itemView.findViewById(R.id.tvMessage);
			tvTime = itemView.findViewById(R.id.tvTime);
			tvTokens = itemView.findViewById(R.id.tvTokens);
			layoutActions = itemView.findViewById(R.id.layoutActions);
			btnCopy = itemView.findViewById(R.id.btnCopy);
			btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
			btnDelete = itemView.findViewById(R.id.btnDelete);


		}

// 在 bind 方法中更新流式状态
		public void bind(final Message message) {
			// 设置模型名称
			if (!TextUtils.isEmpty(message.getModel())) {
				tvModelName.setText(message.getModel());
				tvModelName.setVisibility(View.VISIBLE);
			} else {
				tvModelName.setVisibility(View.GONE);
			}

			// 检查流式状态
			UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);

			boolean isStreaming = false;
			if (uiCoordinator != null) {
				StreamingMessage streamingMessage = uiCoordinator.getStreamingMessage(message.getId());
				isStreaming = streamingMessage != null && streamingMessage.isStreaming();
			}


			tvMessage.setMarkdownText(message.getContent());
			tvTime.setText(formatTimestamp(message.getTimestamp()));

			// 设置 tokens 信息
			if (message.getTokens() > 0) {
				tvTokens.setText(" • " + message.getTokens() + " tokens");
				tvTokens.setVisibility(View.VISIBLE);
			} else {
				tvTokens.setVisibility(View.GONE);
			}

			// 流式消息时隐藏操作按钮
			if (isStreaming) {
				layoutActions.setVisibility(View.GONE);
			}

			// 设置操作按钮
			setupActionButtons(message);

			// 点击消息显示/隐藏操作按钮（流式时禁用）
			if (!isStreaming) {
				tvMessage.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean isVisible = layoutActions.getVisibility() == View.VISIBLE;
							layoutActions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
						}
					});
			} else {
				tvMessage.setOnClickListener(null);
			}
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
        }
    }
    
    private String formatTimestamp(long timestamp) {
        return timeFormat.format(new Date(timestamp));
    }
	
	
}
