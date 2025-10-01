package com.techstar.nexchat;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.techstar.nexchat.model.ApiProvider;
import com.techstar.nexchat.model.ChatConversation;
import com.techstar.nexchat.model.ChatMessage;
import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvChatTitle;

    private ChatConversation currentConversation;
    private List<ChatMessage> messages;
    private Markwon markwon;
    private OkHttpClient client;


    private void initMarkwon() {
        markwon = Markwon.builder(getActivity())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build();
    }







    private void saveConversation(ChatConversation conversation) {
        // 保存对话到SharedPreferences
    }


    private class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        public UserMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        public void bind(ChatMessage message) {
            tvMessage.setText(message.getContent());
        }
    }

    private class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        public AssistantMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        public void bind(ChatMessage message) {
            if (message.isStreaming()) {
                tvMessage.setText(message.getContent() + " ▌");
            } else {
                // 使用Markwon渲染Markdown
                markwon.setMarkdown(tvMessage, message.getContent());
            }
        }
    }



	@Override
	public void onResume() {
		super.onResume();
		// 确保数据正确加载
		loadOrCreateConversation();
	}


	// 修复数据持久化方法
	private ChatConversation loadCurrentConversation() {
		try {
			String conversationId = getActivity().getSharedPreferences("chat", android.content.Context.MODE_PRIVATE)
				.getString("current_conversation_id", "");

			if (!conversationId.isEmpty()) {
				String conversationJson = getActivity().getSharedPreferences("chat", android.content.Context.MODE_PRIVATE)
					.getString(conversationId, "");

				if (!conversationJson.isEmpty()) {
					JSONObject json = new JSONObject(conversationJson);
					ChatConversation conversation = new ChatConversation();
					conversation.setId(json.getString("id"));
					conversation.setTitle(json.getString("title"));
					// ... 加载其他字段和消息
					return conversation;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isInitialized = false;



    public void ensureInitialized() {
        if (!isInitialized && getView() != null) {
            initViews(getView());
            initMarkwon();
            loadOrCreateConversation();
            isInitialized = true;
        }
    }



    private void sendChatRequest(String message, String providerId, String model, final ChatMessage aiMessage) {
		try {
			// 获取供应商信息
			ApiProvider provider = loadProviderById(providerId);
			if (provider == null) {
				handleError("供应商不存在，请检查供应商配置。Provider ID: " + providerId);
				return;
			}

			// 验证API密钥
			if (provider.getApiKey() == null || provider.getApiKey().isEmpty() || provider.getApiKey().equals("sk-...")) {
				handleError("API密钥未配置或无效，请检查供应商设置");
				return;
			}

			// 构建请求体
			JSONObject requestBody = new JSONObject();
			requestBody.put("model", model);

			JSONArray messagesArray = new JSONArray();

			// 添加上下文消息（最后5轮对话）
			int startIndex = Math.max(0, messages.size() - 10);
			for (int i = startIndex; i < messages.size() - 1; i++) {
				ChatMessage msg = messages.get(i);
				JSONObject msgObj = new JSONObject();
				msgObj.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
				msgObj.put("content", msg.getContent());
				messagesArray.put(msgObj);
			}

			// 添加当前消息
			JSONObject currentMsg = new JSONObject();
			currentMsg.put("role", "user");
			currentMsg.put("content", message);
			messagesArray.put(currentMsg);

			requestBody.put("messages", messagesArray);
			requestBody.put("stream", true);
			requestBody.put("temperature", 0.7);

			String apiUrl = provider.getApiUrl().endsWith("/") ? 
				provider.getApiUrl() + "chat/completions" :
				provider.getApiUrl() + "/chat/completions";

			// 日志输出用于调试
			Log.d("ChatFragment", "API URL: " + apiUrl);
			Log.d("ChatFragment", "Model: " + model);
			Log.d("ChatFragment", "Provider: " + provider.getName());

			RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), 
				requestBody.toString()
			);

			Request request = new Request.Builder()
				.url(apiUrl)
				.addHeader("Authorization", "Bearer " + provider.getApiKey())
				.addHeader("Content-Type", "application/json")
				.post(body)
				.build();

			// 保存当前请求以便暂停
			currentCall = client.newCall(request);
			isStreaming = true;

			// 显示暂停按钮
			if (btnPause != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnPause.setVisibility(View.VISIBLE);
						}
					});
			}

			currentCall.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, final IOException e) {
						if (!call.isCanceled()) { // 如果不是用户取消的
							getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										handleError("网络请求失败: " + e.getMessage());
										hidePauseButton();
									}
								});
						}
					}

					@Override
					public void onResponse(Call call, final Response response) throws IOException {
						if (!response.isSuccessful()) {
							final String errorBody = response.body().string();
							getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										handleError("API错误: " + response.code() + " - " + errorBody);
										hidePauseButton();
									}
								});
							return;
						}

						// 处理流式响应
						processStreamResponse(response, aiMessage);
					}
				});

		} catch (Exception e) {
			handleError("请求构建失败: " + e.getMessage());
			hidePauseButton();
			e.printStackTrace();
		}
	}


    // 修复供应商加载方法
    private ApiProvider loadProviderById(String providerId) {
        if (getActivity() == null) return null;

        try {
            Log.d("ChatFragment", "正在加载供应商: " + providerId);

            // 从SharedPreferences加载供应商数据
            String name = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_name", "");
            String url = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_url", "");
            String key = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_key", "");
            int modelCount = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getInt(providerId + "_model_count", 0);

            Log.d("ChatFragment", "加载到供应商数据 - Name: " + name + ", URL: " + url);

            if (!name.isEmpty()) {
                ApiProvider provider = new ApiProvider(name, url, key);
                provider.setId(providerId);

                // 加载模型列表
                for (int i = 0; i < modelCount; i++) {
                    String model = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                        .getString(providerId + "_model_" + i, "");
                    if (!model.isEmpty()) {
                        provider.getModels().add(model);
                    }
                }

                Log.d("ChatFragment", "成功加载供应商: " + provider.getName() + ", 模型数量: " + provider.getModels().size());
                return provider;
            } else {
                Log.d("ChatFragment", "供应商名称为空，可能不存在");
            }
        } catch (Exception e) {
            Log.e("ChatFragment", "加载供应商失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }



	private ChatManager chatManager;


    private void loadOrCreateConversation() {
        // 使用ChatManager加载当前对话
        currentConversation = chatManager.getCurrentConversation();
        if (currentConversation == null) {
            currentConversation = chatManager.createNewConversation();
        }

        messages.clear();
        messages.addAll(currentConversation.getMessages());
        adapter.notifyDataSetChanged();

        tvChatTitle.setText(currentConversation.getTitle());
        scrollToBottom();
    }

	private ImageButton btnPause;
    private boolean isStreaming = false;
    private Call currentCall; // 用于暂停请求

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatManager = ChatManager.getInstance(getActivity());
        initViews(view);
        initMarkwon();
        loadOrCreateConversation();
        isInitialized = true;

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);
        btnPause = view.findViewById(R.id.btnPause); // 新增暂停按钮

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        recyclerView.setAdapter(adapter);

        // 暂停按钮点击事件
        btnPause.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isStreaming && currentCall != null) {
						// 取消当前请求
						currentCall.cancel();
						isStreaming = false;
						btnPause.setVisibility(View.GONE);

						// 更新最后一条消息状态
						if (!messages.isEmpty()) {
							ChatMessage lastMessage = messages.get(messages.size() - 1);
							if (lastMessage.isStreaming()) {
								lastMessage.setStreaming(false);
								lastMessage.setContent(lastMessage.getContent() + "\n\n[已停止]");
								adapter.notifyItemChanged(messages.size() - 1);
								chatManager.saveConversation(currentConversation);
							}
						}

						Toast.makeText(getActivity(), "已停止生成", Toast.LENGTH_SHORT).show();
					}
				}
			});
    }

	

    private void hidePauseButton() {
        isStreaming = false;
        if (btnPause != null) {
            btnPause.setVisibility(View.GONE);
        }
    }



	

	// 修复滚动方法
	private void scrollToBottom() {
		if (recyclerView != null && adapter != null) {
			recyclerView.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (messages != null && messages.size() > 0) {
							try {
								recyclerView.smoothScrollToPosition(messages.size() - 1);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}, 100);
		}
	}
	// ... 其他代码不变

	private boolean isUpdating = false;

	// 修复Adapter，添加同步控制
	private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final List<ChatMessage> messages;
		private final Object lock = new Object();

		private static final int TYPE_ASSISTANT = 1;

		private static final int TYPE_USER = 0;

		public MessageAdapter(List<ChatMessage> messages) {
			this.messages = messages != null ? messages : new ArrayList<ChatMessage>();
		}

		@Override
		public int getItemViewType(int position) {
			synchronized (lock) {
				if (position < 0 || position >= messages.size()) {
					return TYPE_ASSISTANT;
				}
				return messages.get(position).getType() == ChatMessage.TYPE_USER ? TYPE_USER : TYPE_ASSISTANT;
			}
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			// 这个方法在主线程调用，不需要同步
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
			synchronized (lock) {
				if (position < 0 || position >= messages.size()) {
					return;
				}

				ChatMessage message = messages.get(position);
				if (holder instanceof UserMessageViewHolder) {
					((UserMessageViewHolder) holder).bind(message);
				} else if (holder instanceof AssistantMessageViewHolder) {
					((AssistantMessageViewHolder) holder).bind(message);
				}
			}
		}

		@Override
		public int getItemCount() {
			synchronized (lock) {
				return messages.size();
			}
		}

		// 安全地添加消息
		public void safeAddMessage(ChatMessage message) {
			synchronized (lock) {
				messages.add(message);
			}
		}

		// 安全地更新消息
		public void safeUpdateMessage(int position, ChatMessage message) {
			synchronized (lock) {
				if (position >= 0 && position < messages.size()) {
					messages.set(position, message);
				}
			}
		}

		// 安全地通知数据更新
		public void safeNotifyItemInserted(final int position) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (lock) {
								if (position >= 0 && position <= messages.size()) {
									notifyItemInserted(position);
								}
							}
						}
					});
			}
		}

		public void safeNotifyItemChanged(final int position) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (lock) {
								if (position >= 0 && position < messages.size()) {
									notifyItemChanged(position);
								}
							}
						}
					});
			}
		}
	}

	// 修复发送消息方法
	public void sendMessage(String messageText, String providerId, String model) {
		if (!isInitialized) {
			ensureInitialized();
		}

		if (TextUtils.isEmpty(messageText)) return;

		// 检查是否正在更新
		if (isUpdating) {
			log.e("ChatFragment", "Already updating, skip new message");
			return;
		}

		isUpdating = true;

		try {
			// 确保数据初始化
			if (messages == null) {
				messages = new ArrayList<>();
			}
			if (currentConversation == null) {
				loadOrCreateConversation();
			}
			if (adapter == null) {
				adapter = new MessageAdapter(messages);
				recyclerView.setAdapter(adapter);
			}

			// 创建用户消息
			ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, messageText);
			messages.add(userMessage);
			currentConversation.addMessage(userMessage);

			// 使用安全的通知方法
			if (adapter != null) {
				adapter.safeNotifyItemInserted(messages.size() - 1);
			}
			safeScrollToBottom();

			// 创建AI消息（流式响应）
			ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
			aiMessage.setStreaming(true);
			aiMessage.setModel(model);
			messages.add(aiMessage);
			currentConversation.addMessage(aiMessage);

			if (adapter != null) {
				adapter.safeNotifyItemInserted(messages.size() - 1);
			}
			safeScrollToBottom();

			// 发送API请求
			sendChatRequest(messageText, providerId, model, aiMessage);

		} finally {
			isUpdating = false;
		}
	}

	// 修复流式响应处理
	private void processStreamResponse(Response response, final ChatMessage aiMessage) {
		try {
			String line;
			final StringBuilder content = new StringBuilder();
			java.io.BufferedReader reader = new java.io.BufferedReader(
				new java.io.InputStreamReader(response.body().byteStream())
			);

			final int aiMessageIndex = messages.indexOf(aiMessage);

			while ((line = reader.readLine()) != null && isStreaming) {
				if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
					String jsonStr = line.substring(6);
					if (!jsonStr.trim().isEmpty()) {
						JSONObject data = new JSONObject(jsonStr);
						JSONArray choices = data.getJSONArray("choices");
						if (choices.length() > 0) {
							JSONObject choice = choices.getJSONObject(0);
							JSONObject delta = choice.getJSONObject("delta");
							if (delta.has("content")) {
								String chunk = delta.getString("content");
								content.append(chunk);

								// 安全地更新UI
								final String currentContent = content.toString();
								if (getActivity() != null) {
									getActivity().runOnUiThread(new Runnable() {
											@Override
											public void run() {
												synchronized (ChatFragment.this) {
													if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
														aiMessage.setContent(currentContent);
														aiMessage.setStreaming(true);
														if (adapter != null) {
															adapter.safeNotifyItemChanged(aiMessageIndex);
														}
														safeScrollToBottom();
													}
												}
											}
										});
								}
							}
						}
					}
				}
			}

			// 流式响应结束
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (ChatFragment.this) {
								if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
									aiMessage.setStreaming(false);
									hidePauseButton();
									if (adapter != null) {
										adapter.safeNotifyItemChanged(aiMessageIndex);
									}

									// 保存对话到ChatManager
									chatManager.saveConversation(currentConversation);

									// 更新对话标题
									if ("新对话".equals(currentConversation.getTitle()) && content.length() > 10) {
										String newTitle = content.toString().substring(0, Math.min(20, content.length()));
										currentConversation.setTitle(newTitle);
										tvChatTitle.setText(newTitle);
										chatManager.saveConversation(currentConversation);
									}
								}
							}
						}
					});
			}

		} catch (final Exception e) {
			log.e("ChatFragment", "Stream response processing failed", e);
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (isStreaming) {
								handleError("响应解析失败: " + e.getMessage());
							}
							hidePauseButton();
						}
					});
			}
		}
	}

	// 修复滚动方法
	private void safeScrollToBottom() {
		if (recyclerView != null && adapter != null && getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							if (messages != null && messages.size() > 0) {
								// 使用post确保RecyclerView已经完成布局
								recyclerView.post(new Runnable() {
										@Override
										public void run() {
											try {
												recyclerView.smoothScrollToPosition(messages.size() - 1);
											} catch (Exception e) {
												log.e("ChatFragment", "Scroll failed", e);
											}
										}
									});
							}
						} catch (Exception e) {
							log.e("ChatFragment", "Safe scroll failed", e);
						}
					}
				});
		}
	}

	// 修复错误处理方法
	private void handleError(final String error) {
		log.e("ChatFragment", "Error: " + error);

		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						synchronized (ChatFragment.this) {
							hidePauseButton();

							if (!messages.isEmpty()) {
								ChatMessage lastMessage = messages.get(messages.size() - 1);
								if (lastMessage.isStreaming()) {
									lastMessage.setContent("错误: " + error);
									lastMessage.setStreaming(false);
									if (adapter != null) {
										adapter.safeNotifyItemChanged(messages.size() - 1);
									}
								}
							}

							Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
						}
					}
				});
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// 清理资源
		isUpdating = false;
		isStreaming = false;
		if (currentCall != null) {
			currentCall.cancel();
		}
	}
}
