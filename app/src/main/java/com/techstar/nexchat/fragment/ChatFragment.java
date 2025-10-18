package com.techstar.nexchat.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.R;
import com.techstar.nexchat.adapter.MessageAdapter;
import com.techstar.nexchat.database.ChatHistoryDao;
import com.techstar.nexchat.database.MessageDao;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.model.Message;
import com.techstar.nexchat.model.StreamingMessage;
import com.techstar.nexchat.service.UIUpdateCoordinator;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private FileLogger logger;
    private MessageDao messageDao;
    private ChatHistoryDao chatHistoryDao;
    
    private List<Message> messages;
    private int currentChatId = -1;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        logger = FileLogger.getInstance(getContext());
        messageDao = new MessageDao(getContext());
        chatHistoryDao = new ChatHistoryDao(getContext());
        
        logger.i(TAG, "ChatFragment created");
        
        initViews(view);
        loadDefaultChat();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(getContext(), messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMessages.setAdapter(messageAdapter);
        
        // 设置消息操作监听
        messageAdapter.setOnMessageActionListener(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onCopyMessage(Message message) {
                copyMessage(message);
            }
            
            @Override
            public void onRegenerateMessage(Message message) {
                regenerateMessage(message);
            }
            
            @Override
            public void onDeleteMessage(Message message) {
                deleteMessage(message);
            }
        });
    }
    
    private void loadDefaultChat() {
        // 加载最新的聊天记录
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ChatHistory> chats = chatHistoryDao.getAllChats();
                if (!chats.isEmpty()) {
                    loadChat(chats.get(0).getId());
                } else {
                    // 如果没有聊天记录，创建一个新的
                    createNewChat();
                }
            }
        }).start();
    }
    
    public void loadChat(int chatId) {
		if (chatId == currentChatId) {
			logger.d(TAG, "Same chat ID, skipping load: " + chatId);
			return;
		}

		currentChatId = chatId;

		new Thread(new Runnable() {
				@Override
				public void run() {
					final List<Message> chatMessages = messageDao.getMessagesByChatId(chatId);
					final ChatHistory chat = chatHistoryDao.getChatById(chatId);

					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									messages.clear();
									messages.addAll(chatMessages);
									messageAdapter.notifyDataSetChanged();

									// 滚动到底部
									if (!messages.isEmpty()) {
										recyclerViewMessages.scrollToPosition(messages.size() - 1);
									}

									logger.i(TAG, "Loaded " + messages.size() + " messages for chat: " + chatId);
								}
							});
					}
				}
			}).start();
	}
    
    private void createNewChat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatHistory newChat = new ChatHistory("新对话");
                long chatId = chatHistoryDao.insertChat(newChat);
                
                if (chatId != -1) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadChat((int) chatId);
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    public void addUserMessage(String content) {
        if (currentChatId == -1) {
            logger.w(TAG, "No active chat, cannot add message");
            return;
        }
        
        final Message userMessage = new Message(currentChatId, Message.ROLE_USER, content);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                long messageId = messageDao.insertMessage(userMessage);
                if (messageId != -1) {
                    userMessage.setId((int) messageId);
                    
                    // 更新聊天预览
                    String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                    chatHistoryDao.updateChatPreview(currentChatId, preview);
                    chatHistoryDao.incrementMessageCount(currentChatId);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messages.add(userMessage);
                                messageAdapter.notifyItemInserted(messages.size() - 1);
                                recyclerViewMessages.scrollToPosition(messages.size() - 1);
                                
                                logger.i(TAG, "Added user message: " + content);
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    // 在 ChatFragment.java 中添加流式支持

// 修改 addAssistantMessage 方法，支持流式初始化
	public int addAssistantMessage(String content, String model, boolean isStreaming) {
		if (currentChatId == -1) {
			logger.w(TAG, "No active chat, cannot add message");
			return -1;
		}

		final Message assistantMessage = new Message(currentChatId, Message.ROLE_ASSISTANT, content);
		assistantMessage.setModel(model);

		// 如果是流式消息，先创建空消息
		if (isStreaming) {
			assistantMessage.setContent(""); // 初始为空
		}

		final int[] messageId = {-1};

		new Thread(new Runnable() {
				@Override
				public void run() {
					long id = messageDao.insertMessage(assistantMessage);
					if (id != -1) {
						assistantMessage.setId((int) id);
						messageId[0] = (int) id;

						if (isStreaming) {
							// 注册流式消息
							StreamingMessage streamingMessage = new StreamingMessage(assistantMessage);
							UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
							uiCoordinator.registerStreamingMessage((int) id, streamingMessage);
						}

						if (getActivity() != null) {
							getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										messages.add(assistantMessage);
										messageAdapter.notifyItemInserted(messages.size() - 1);
										recyclerViewMessages.scrollToPosition(messages.size() - 1);

										logger.i(TAG, "Added assistant message with ID: " + id);
									}
								});
						}
					}
				}
			}).start();

		return messageId[0];
	}

// 添加流式更新方法
	/*
	public void updateAssistantMessage(int messageId, String contentChunk) {
		UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
		uiCoordinator.updateMessageContent(currentChatId, messageId, contentChunk);
	}
	*/

// 完成流式消息
	// 在 ChatFragment.java 中修改 completeAssistantMessage 方法
public void completeAssistantMessage(int messageId, String fullContent) {
    // 更新数据库中的完整内容
    new Thread(new Runnable() {
        @Override
        public void run() {
            // 找到对应的消息对象并更新
            for (Message message : messages) {
                if (message.getId() == messageId) {
                    message.setContent(fullContent);
                    messageDao.updateMessage(message);  // 使用现有的 updateMessage 方法
                    break;
                }
            }
            
            // 更新聊天预览
            String preview = fullContent.length() > 50 ? fullContent.substring(0, 50) + "..." : fullContent;
            chatHistoryDao.updateChatPreview(currentChatId, preview);
            chatHistoryDao.incrementMessageCount(currentChatId);
        }
    }).start();
    
    UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
    uiCoordinator.completeStreaming(currentChatId, messageId);
}

// 在 Fragment 销毁时清理
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (messageAdapter != null) {
			messageAdapter.cleanup();
		}
		if (currentChatId != -1) {
			UIUpdateCoordinator uiCoordinator = UIUpdateCoordinator.getInstance(logger);
			uiCoordinator.cleanupChat(currentChatId);
		}
	}
    
    private void copyMessage(Message message) {
        // 复制消息内容到剪贴板
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Message", message.getContent());
        clipboard.setPrimaryClip(clip);
        
        // 显示提示
        android.widget.Toast.makeText(getContext(), "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show();
        logger.i(TAG, "Copied message: " + message.getContent());
    }
    
    private void regenerateMessage(Message message) {
        logger.i(TAG, "Regenerating message: " + message.getId());
        // 重新生成逻辑需要结合 InputFragment 的发送功能
        // 这里先简单记录
        android.widget.Toast.makeText(getContext(), "重新生成功能开发中", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void deleteMessage(Message message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = messageDao.deleteMessage(message.getId());
                if (success) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int position = messages.indexOf(message);
                                if (position != -1) {
                                    messages.remove(position);
                                    messageAdapter.notifyItemRemoved(position);
                                    logger.i(TAG, "Deleted message: " + message.getId());
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    public int getCurrentChatId() {
        return currentChatId;
    }
}
