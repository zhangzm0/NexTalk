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

	private boolean isInitialized = false;



    public void ensureInitialized() {
        if (!isInitialized && getView() != null) {
            initViews(getView());
            initMarkwon();
            loadOrCreateConversation();
            isInitialized = true;
        }
    }

	private ChatManager chatManager;
	private ImageButton btnPause;
    private boolean isStreaming = false;
    private Call currentCall; // 用于暂停请求

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
			AppLogger.e("ChatFragment", "Stream response processing failed", e);
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
												AppLogger.e("ChatFragment", "Scroll failed", e);
											}
										}
									});
							}
						} catch (Exception e) {
							AppLogger.e("ChatFragment", "Safe scroll failed", e);
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


	// 修复供应商加载方法，添加更多调试信息
	private ApiProvider loadProviderById(String providerId) {
		if (getActivity() == null) {
			AppLogger.e("ChatFragment", "Activity为null，无法加载供应商");
			return null;
		}

		if (providerId == null || providerId.isEmpty()) {
			AppLogger.e("ChatFragment", "供应商ID为空");
			return null;
		}

		try {
			AppLogger.d("ChatFragment", "正在加载供应商: " + providerId);

			// 从SharedPreferences加载供应商数据
			String name = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getString(providerId + "_name", "");
			String url = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getString(providerId + "_url", "");
			String key = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getString(providerId + "_key", "");
			int modelCount = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
				.getInt(providerId + "_model_count", 0);

			AppLogger.d("ChatFragment", "加载到供应商数据 - Name: " + name + ", URL: " + url + ", ModelCount: " + modelCount);

			if (!name.isEmpty() && !url.isEmpty()) {
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

				AppLogger.d("ChatFragment", "成功加载供应商: " + provider.getName() + ", 模型数量: " + provider.getModels().size());
				return provider;
			} else {
				AppLogger.e("ChatFragment", "供应商数据不完整: name=" + name + ", url=" + url);
				return null;
			}
		} catch (Exception e) {
			AppLogger.e("ChatFragment", "加载供应商失败", e);
			return null;
		}
	}

	// 修复错误处理方法
	private void handleError(final String error) {
		AppLogger.e("ChatFragment", "处理错误: " + error);

		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							hidePauseButton();

							if (messages != null && !messages.isEmpty()) {
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
						} catch (Exception e) {
							AppLogger.e("ChatFragment", "处理错误时发生异常", e);
						}
					}
				});
		}
	}


	// 添加一个简单的测试方法，绕过复杂的请求构建
	private void testSimpleRequest() {
		try {
			AppLogger.d("ChatFragment", "开始简单测试请求");

			// 创建一个最简单的请求
			String testUrl = "https://httpbin.org/get"; // 测试用的公开API
			Request request = new Request.Builder()
				.url(testUrl)
				.get()
				.build();

			AppLogger.d("ChatFragment", "简单请求构建完成");

			client.newCall(request).enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						AppLogger.e("ChatFragment", "简单测试请求失败", e);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						AppLogger.d("ChatFragment", "简单测试请求成功: " + response.code());
					}
				});

		} catch (Exception e) {
			AppLogger.e("ChatFragment", "简单测试请求异常", e);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_chat, container, false);

		// 正确初始化OkHttpClient
		initHttpClient();

		chatManager = ChatManager.getInstance(getActivity());
		initViews(view);
		initMarkwon();
		loadOrCreateConversation();
		isInitialized = true;

		return view;
	}

	// 初始化OkHttpClient
	private void initHttpClient() {
		try {
			AppLogger.d("ChatFragment", "开始初始化OkHttpClient");

			// 创建OkHttpClient构建器
			OkHttpClient.Builder builder = new OkHttpClient.Builder();

			// 设置超时时间
			builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
			builder.readTimeout(60, java.util.concurrent.TimeUnit.SECONDS);
			builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS);

			// 创建客户端
			client = builder.build();

			AppLogger.d("ChatFragment", "OkHttpClient初始化成功");

		} catch (Exception e) {
			AppLogger.e("ChatFragment", "OkHttpClient初始化失败", e);
			// 备用方案：使用默认客户端
			client = new OkHttpClient();
			AppLogger.d("ChatFragment", "使用默认OkHttpClient");
		}
	}

	// 添加一个更简单的测试方法
	private void testHttpClient() {
		try {
			AppLogger.d("ChatFragment", "测试HTTP客户端");

			if (client == null) {
				AppLogger.e("ChatFragment", "客户端为null");
				return;
			}

			// 测试一个简单的HTTP请求
			Request testRequest = new Request.Builder()
				.url("https://httpbin.org/get")
				.get()
				.build();

			AppLogger.d("ChatFragment", "测试请求构建完成");

			client.newCall(testRequest).enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						AppLogger.e("ChatFragment", "HTTP客户端测试失败", e);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						AppLogger.d("ChatFragment", "HTTP客户端测试成功: " + response.code());
					}
				});

		} catch (Exception e) {
			AppLogger.e("ChatFragment", "HTTP客户端测试异常", e);
		}
	}

	public void sendMessage(String messageText, String providerId, String model) {
		if (!isInitialized) {
			ensureInitialized();
		}

		if (TextUtils.isEmpty(messageText)) return;

		// 检查是否正在更新
		if (isUpdating) {
			AppLogger.w("ChatFragment", "正在更新，跳过新消息");
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

			// 如果这是第一条消息，用用户的第一句话作为标题
			if (currentConversation.getMessageCount() == 1 && "新对话".equals(currentConversation.getTitle())) {
				String newTitle = messageText.length() > 20 ? messageText.substring(0, 20) + "..." : messageText;
				currentConversation.setTitle(newTitle);
				tvChatTitle.setText(newTitle);
				chatManager.saveConversation(currentConversation);
			}

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

	// 修复暂停键显示逻辑
	private void sendChatRequest(String message, String providerId, String model, final ChatMessage aiMessage) {
		try {
			AppLogger.d("ChatFragment", "开始构建请求");

			// 检查client是否初始化
			if (client == null) {
				AppLogger.e("ChatFragment", "OkHttpClient为null");
				handleError("网络客户端未初始化");
				return;
			}

			// 先显示暂停按钮
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (btnPause != null) {
								btnPause.setVisibility(View.VISIBLE);
								AppLogger.d("ChatFragment", "暂停按钮已显示");
							}
						}
					});
			}

			// 获取供应商信息
			ApiProvider provider = loadProviderById(providerId);
			if (provider == null) {
				handleError("供应商不存在: " + providerId);
				hidePauseButton();
				return;
			}

			// 构建请求体
			JSONObject requestBody = new JSONObject();
			requestBody.put("model", model);

			JSONArray messagesArray = new JSONArray();

			// 添加上下文消息
			if (messages != null && messages.size() > 1) {
				int startIndex = Math.max(0, messages.size() - 10);
				for (int i = startIndex; i < messages.size() - 1; i++) {
					if (i >= 0 && i < messages.size()) {
						ChatMessage msg = messages.get(i);
						JSONObject msgObj = new JSONObject();
						msgObj.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
						msgObj.put("content", msg.getContent());
						messagesArray.put(msgObj);
					}
				}
			}

			// 添加当前消息
			JSONObject currentMsg = new JSONObject();
			currentMsg.put("role", "user");
			currentMsg.put("content", message);
			messagesArray.put(currentMsg);

			requestBody.put("messages", messagesArray);
			requestBody.put("stream", true);
			requestBody.put("temperature", 0.7);

			// 构建API URL
			String baseUrl = provider.getApiUrl().trim();
			String apiUrl = baseUrl.endsWith("/") ? 
				baseUrl + "chat/completions" :
				baseUrl + "/chat/completions";

			AppLogger.d("ChatFragment", "发送请求到: " + apiUrl);

			RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), 
				requestBody.toString()
			);

			Request request = new Request.Builder()
				.url(apiUrl)
				.addHeader("Authorization", "Bearer " + provider.getApiKey().trim())
				.addHeader("Content-Type", "application/json")
				.post(body)
				.build();

			// 创建Call
			currentCall = client.newCall(request);
			isStreaming = true;

			currentCall.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, final IOException e) {
						AppLogger.d("ChatFragment", "请求失败");
						if (!call.isCanceled()) {
							AppLogger.e("ChatFragment", "网络请求失败", e);
							if (getActivity() != null) {
								getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											handleError("网络请求失败: " + e.getMessage());
											hidePauseButton();
										}
									});
							}
						}
					}

					@Override
					public void onResponse(Call call, final Response response) throws IOException {
						AppLogger.d("ChatFragment", "收到响应: " + response.code());
						if (!response.isSuccessful()) {
							String errorBody = response.body().string();
							AppLogger.e("ChatFragment", "API错误: " + response.code());
							if (getActivity() != null) {
								getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											handleError("API错误: " + response.code());
											hidePauseButton();
										}
									});
							}
							return;
						}

						AppLogger.d("ChatFragment", "开始处理流式响应");
						processStreamResponse(response, aiMessage);
					}
				});

		} catch (Exception e) {
			AppLogger.e("ChatFragment", "请求构建异常", e);
			handleError("请求构建失败: " + e.getMessage());
			hidePauseButton();
		}
	}

	// 修复暂停键隐藏逻辑
	private void hidePauseButton() {
		isStreaming = false;
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (btnPause != null) {
							btnPause.setVisibility(View.GONE);
							AppLogger.d("ChatFragment", "暂停按钮已隐藏");
						}
					}
				});
		}
	}
	// ... 其他代码不变

	@Override
	public void onResume() {
		super.onResume();
		AppLogger.d("ChatFragment", "onResume called");

		// 每次页面显示时检查是否需要更新对话
		checkAndUpdateConversation();
	}

	// 检查并更新当前对话
	private void checkAndUpdateConversation() {
		if (chatManager == null) {
			chatManager = ChatManager.getInstance(getActivity());
		}

		ChatConversation currentConv = chatManager.getCurrentConversation();
		if (currentConv != null) {
			// 如果当前对话与显示的不同，更新显示
			if (currentConversation == null || !currentConversation.getId().equals(currentConv.getId())) {
				AppLogger.d("ChatFragment", "切换到新对话: " + currentConv.getTitle());
				loadConversation(currentConv.getId());
			}
		} else {
			// 如果没有当前对话，创建新的
			AppLogger.d("ChatFragment", "没有当前对话，创建新对话");
			loadOrCreateConversation();
		}
	}

	// 加载指定对话
	private void loadConversation(String conversationId) {
		try {
			AppLogger.d("ChatFragment", "加载对话: " + conversationId);

			ChatConversation conversation = chatManager.loadConversation(conversationId);
			if (conversation != null) {
				currentConversation = conversation;

				// 更新消息列表
				if (messages == null) {
					messages = new ArrayList<>();
				} else {
					messages.clear();
				}
				messages.addAll(currentConversation.getMessages());

				// 更新UI
				if (tvChatTitle != null) {
					tvChatTitle.setText(currentConversation.getTitle());
				}

				if (adapter != null) {
					adapter.notifyDataSetChanged();
				} else {
					adapter = new MessageAdapter(messages);
					if (recyclerView != null) {
						recyclerView.setAdapter(adapter);
					}
				}

				safeScrollToBottom();
				AppLogger.d("ChatFragment", "对话加载完成: " + currentConversation.getTitle() + ", 消息数: " + messages.size());
			} else {
				AppLogger.e("ChatFragment", "对话加载失败: " + conversationId);
				loadOrCreateConversation();
			}
		} catch (Exception e) {
			AppLogger.e("ChatFragment", "加载对话异常", e);
			loadOrCreateConversation();
		}
	}

	// 修改原有的loadOrCreateConversation方法
	private void loadOrCreateConversation() {
		AppLogger.d("ChatFragment", "加载或创建对话");

		if (chatManager == null) {
			chatManager = ChatManager.getInstance(getActivity());
		}

		// 获取当前对话
		currentConversation = chatManager.getCurrentConversation();
		if (currentConversation == null) {
			// 如果没有当前对话，创建新的
			currentConversation = chatManager.createNewConversation();
			AppLogger.d("ChatFragment", "创建新对话: " + currentConversation.getId());
		} else {
			AppLogger.d("ChatFragment", "使用现有对话: " + currentConversation.getTitle());
		}

		// 初始化消息列表
		if (messages == null) {
			messages = new ArrayList<>();
		} else {
			messages.clear();
		}
		messages.addAll(currentConversation.getMessages());

		// 初始化适配器
		if (adapter == null) {
			adapter = new MessageAdapter(messages);
			if (recyclerView != null) {
				recyclerView.setAdapter(adapter);
			}
		} else {
			adapter.notifyDataSetChanged();
		}

		// 更新标题
		if (tvChatTitle != null) {
			tvChatTitle.setText(currentConversation.getTitle());
		}

		safeScrollToBottom();
		AppLogger.d("ChatFragment", "对话加载完成: " + currentConversation.getTitle() + ", 消息数: " + messages.size());
	}

	// 添加一个公共方法来强制刷新对话
	public void refreshConversation() {
		AppLogger.d("ChatFragment", "强制刷新对话");
		checkAndUpdateConversation();
	}
}
