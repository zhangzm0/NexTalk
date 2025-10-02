package com.techstar.nexchat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
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
    private TextView tvChatTitle;


    private ChatConversation currentConversation;
    private List<ChatMessage> messages;
    private Markwon markwon;
    private OkHttpClient client;

	private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;

	private int userIndex = -1;
	private int aiIndex = -1;
	
	private int updateCount = 0;

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

// 在initViews方法中修改LayoutManager
	private void initViews(View view) {
		recyclerView = view.findViewById(R.id.recyclerViewMessages);
		tvChatTitle = view.findViewById(R.id.tvChatTitle);
		btnPause = view.findViewById(R.id.btnPause);

		// 创建LinearLayoutManager并设置从底部开始堆叠
		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setStackFromEnd(true); // 关键：从底部开始堆叠
		layoutManager.setReverseLayout(true); // 正常顺序

		recyclerView.setLayoutManager(layoutManager);

		messages = new ArrayList<>();
		adapter = new MessageAdapter(messages);
		recyclerView.setAdapter(adapter);

		// 暂停按钮点击事件
		btnPause.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isStreaming && currentCall != null) {
						currentCall.cancel();
						isStreaming = false;
						btnPause.setVisibility(View.GONE);

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




	private boolean isUpdating = false;

// 在ChatFragment.java中彻底修复滚动逻辑
	// 在ChatFragment.java中添加滚动控制变量和方法
	private void scrollToBottom() {/*
		if (recyclerView != null && adapter != null && getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							if (messages != null && messages.size() > 0) {
								LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
								if (layoutManager != null) {
									// 一键直接到底部 - 这是最关键的代码
									layoutManager.scrollToPositionWithOffset(messages.size() - 1, 0);
								}
							}
						} catch (Exception e) {
							AppLogger.e("ChatFragment", "滚动失败", e);
						}
					}
				});
		}*/
	}


// 修改processStreamResponse方法中的滚动逻辑
	private void processStreamResponse(Response response, final ChatMessage aiMessage) {
		try {
			String line;
			StringBuilder content = new StringBuilder();
			java.io.BufferedReader reader = new java.io.BufferedReader(
				new java.io.InputStreamReader(response.body().byteStream())
			);

			final int aiMessageIndex = messages.indexOf(aiMessage);
			int promptTokens = 0;
			int completionTokens = 0;
			int totalTokens = 0;
			boolean hasTokens = false;
			updateCount = 0;

			while ((line = reader.readLine()) != null && isStreaming) {
				if (line.startsWith("data: ")) {
					if (line.equals("data: [DONE]")) {
						break;
					}

					String jsonStr = line.substring(6);
					if (!jsonStr.trim().isEmpty()) {
						try {
							JSONObject data = new JSONObject(jsonStr);

							if (data.has("usage")) {
								JSONObject usage = data.getJSONObject("usage");
								promptTokens = usage.optInt("prompt_tokens", 0);
								completionTokens = usage.optInt("completion_tokens", 0);
								totalTokens = usage.optInt("total_tokens", 0);
								hasTokens = true;
							}

							JSONArray choices = data.getJSONArray("choices");
							if (choices.length() > 0) {
								JSONObject choice = choices.getJSONObject(0);
								JSONObject delta = choice.getJSONObject("delta");
								if (delta.has("content")) {
									String chunk = delta.getString("content");
									content.append(chunk);
									updateCount++;

									// 每3个chunk更新一次UI
									if (updateCount % 3 == 0) {
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
																// 流式响应时每9个chunk滚动到底部
																if (updateCount % 9 == 0){
																	scrollToBottom();
																}
															}
														}
													}
												});
										}
									}
								}
							}
						} catch (Exception e) {
							AppLogger.e("ChatFragment", "解析流数据失败", e);
						}
					}
				}
			}

			// 流结束时的处理
			final int finalPromptTokens = promptTokens;
			final int finalCompletionTokens = completionTokens;
			final int finalTotalTokens = totalTokens;
			final boolean finalHasTokens = hasTokens;
			final String finalContent = content.toString();

			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (ChatFragment.this) {
								if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
									aiMessage.setContent(finalContent);
									aiMessage.setStreaming(false);

									if (finalHasTokens) {
										aiMessage.setTokensInfo(finalPromptTokens, finalCompletionTokens, finalTotalTokens);
									}
									hidePauseButton();
									if (adapter != null) {
										adapter.safeNotifyItemChanged(aiMessageIndex);
									}

									// 流结束时滚动到底部
									scrollToBottom();

									// 保存对话
									chatManager.saveConversation(currentConversation);
								}
							}
						}
					});
			}

		} catch (final Exception e) {
			AppLogger.e("ChatFragment", "流式响应处理失败", e);
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



	// 修改sendMessage方法，保存providerId
	private String currentProviderId = "";
	private String currentModel = "";

	// 修改sendMessage方法
	public void sendMessage(String messageText, String providerId, String model) {
		this.currentProviderId = providerId;
		this.currentModel = model;

		if (!isInitialized) {
			ensureInitialized();
		}

		if (TextUtils.isEmpty(messageText)) return;

		if (isUpdating) {
			AppLogger.w("ChatFragment", "正在更新，跳过新消息");
			return;
		}

		isUpdating = true;

		try {
			// 创建用户消息
			ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, messageText);
			messages.add(userMessage);
			currentConversation.addMessage(userMessage);

			// 如果这是第一条消息，用用户的第一句话作为标题
			if (currentConversation.getMessageCount() == 1 && "新对话".equals(currentConversation.getTitle())) {
				String newTitle = messageText.length() > 20 ? messageText.substring(0, 20) + "..." : messageText;
				currentConversation.setTitle(newTitle);
				tvChatTitle.setText(newTitle);
			}

			// 立即保存对话
			chatManager.saveConversation(currentConversation);

			if (adapter != null) {
				adapter.safeNotifyDataSetChanged();
			}
			scrollToBottom(); // 发送消息后滚动到底部

			// 创建AI消息（流式响应）
			ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
			aiMessage.setStreaming(true);
			aiMessage.setModel(model);
			messages.add(aiMessage);
			currentConversation.addMessage(aiMessage);

			// 立即保存包含AI消息的对话
			chatManager.saveConversation(currentConversation);

			if (adapter != null) {
				adapter.safeNotifyDataSetChanged();
			}
			scrollToBottom(); // AI消息创建后滚动到底部

			// 发送API请求
			sendChatRequest(messageText, providerId, model, aiMessage);

		} finally {
			isUpdating = false;
		}
	}

// 修改重新生成专用的发送方法
	private void sendRegeneratedMessage(String messageText, String providerId, String model) {
		this.currentProviderId = providerId;
		this.currentModel = model;

		if (TextUtils.isEmpty(messageText)) return;

		if (isUpdating) {
			AppLogger.w("ChatFragment", "正在更新，跳过重新生成");
			return;
		}

		isUpdating = true;

		try {
			// 创建AI消息（流式响应）
			ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
			aiMessage.setStreaming(true);
			aiMessage.setModel(model);
			messages.add(aiMessage);
			currentConversation.addMessage(aiMessage);

			// 立即保存对话
			chatManager.saveConversation(currentConversation);

			if (adapter != null) {
				adapter.safeNotifyDataSetChanged();
			}
			scrollToBottom(); // 重新生成时滚动到底部

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


	// 在ChatFragment.java中添加初始化方法
	@Override
	public void onResume() {
		super.onResume();
		AppLogger.d("ChatFragment", "onResume called");

		// 从InputFragment获取当前选择的模型
		if (getActivity() instanceof MainActivity) {
			MainActivity mainActivity = (MainActivity) getActivity();
			if (mainActivity.inputFragment != null) {
				// 通过MainActivity同步模型选择
				String providerId = mainActivity.inputFragment.getCurrentProviderId();
				String model = mainActivity.inputFragment.getCurrentModel();
				if (!TextUtils.isEmpty(providerId) && !TextUtils.isEmpty(model)) {
					this.currentProviderId = providerId;
					this.currentModel = model;
					AppLogger.d("ChatFragment", "从InputFragment同步选择: " + providerId + ", " + model);
				}
			}
		}

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
	// 在加载对话时验证tokens是否正确加载
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

				// 验证tokens是否正确加载
				for (ChatMessage msg : messages) {
					if (msg.getType() == ChatMessage.TYPE_ASSISTANT && msg.getTotalTokens() > 0) {
						AppLogger.d("ChatFragment", "加载到tokens: " + msg.getTokensText());
					}
				}

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

				scrollToBottom();
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

// 修改loadOrCreateConversation方法
	private void loadOrCreateConversation() {
		AppLogger.d("ChatFragment", "加载或创建对话");

		if (chatManager == null) {
			chatManager = ChatManager.getInstance(getActivity());
		}

		// 获取当前对话
		currentConversation = chatManager.getCurrentConversation();
		if (currentConversation == null) {
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

		// 打开对话时延迟滚动到底部，确保布局完成
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						scrollToBottom();
					}
				});
		}

		AppLogger.d("ChatFragment", "对话加载完成: " + currentConversation.getTitle() + ", 消息数: " + messages.size());
	}

	// 添加一个公共方法来强制刷新对话
	public void refreshConversation() {
		AppLogger.d("ChatFragment", "强制刷新对话");
		checkAndUpdateConversation();
	}


// 修改MessageAdapter类
	private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final List<ChatMessage> messages;
		private final Object lock = new Object();

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

// 在MessageAdapter类中添加安全通知方法
		public void safeNotifyItemInserted(final int position) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (lock) {
								if (position >= 0 && position <= messages.size()) {
									try {
										notifyItemInserted(position);
									} catch (Exception e) {
										AppLogger.e("MessageAdapter", "notifyItemInserted失败", e);
										notifyDataSetChanged();
									}
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
									try {
										notifyItemChanged(position);
									} catch (Exception e) {
										AppLogger.e("MessageAdapter", "notifyItemChanged失败", e);
										// 不调用notifyDataSetChanged()，避免循环
									}
								}
							}
						}
					});
			}
		}
		public void safeNotifyItemRemoved(final int position) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (lock) {
								if (position >= 0 && position < messages.size()) {
									try {
										notifyItemRemoved(position);
									} catch (Exception e) {
										AppLogger.e("MessageAdapter", "notifyItemRemoved失败", e);
										notifyDataSetChanged();
									}
								}
							}
						}
					});
			}
		}

		public void safeNotifyDataSetChanged() {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (lock) {
								try {
									notifyDataSetChanged();
								} catch (Exception e) {
									AppLogger.e("MessageAdapter", "notifyDataSetChanged失败", e);
								}
							}
						}
					});
			}
		}


		// 在MessageAdapter的ViewHolder中添加删除按钮
		private class UserMessageViewHolder extends RecyclerView.ViewHolder {
			private TextView tvMessage, tvTime;
			private LinearLayout layoutActions;
			private Button btnCopy, btnRegenerate, btnDelete;

			public UserMessageViewHolder(@NonNull View itemView) {
				super(itemView);
				tvMessage = itemView.findViewById(R.id.tvMessage);
				tvTime = itemView.findViewById(R.id.tvTime);
				layoutActions = itemView.findViewById(R.id.layoutActions);
				btnCopy = itemView.findViewById(R.id.btnCopy);
				btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
				btnDelete = itemView.findViewById(R.id.btnDelete);

				if (tvMessage != null) {
					tvMessage.setOnLongClickListener(null);
					tvMessage.setLongClickable(true);
				}
			}

			public void bind(final ChatMessage message) {
				if (tvMessage != null) {
					tvMessage.setText(message.getContent());
				}

				if (tvTime != null) {
					tvTime.setText(message.getFormattedTime());
				}

				// 设置操作按钮
				if (btnCopy != null) {
					btnCopy.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								copyToClipboard(message.getContent());
								Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
							}
						});
				}

				if (btnRegenerate != null) {
					btnRegenerate.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								regenerateMessage(message);
							}
						});
				}

				if (btnDelete != null) {
					btnDelete.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								deleteMessagePair(message);
							}
						});
				}
			}
		}

		private class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
			private TextView tvMessage, tvTime, tvTokens, tvModelName;
			private LinearLayout layoutActions;
			private Button btnCopy, btnRegenerate, btnDelete;

			public AssistantMessageViewHolder(@NonNull View itemView) {
				super(itemView);
				tvMessage = itemView.findViewById(R.id.tvMessage);
				tvTime = itemView.findViewById(R.id.tvTime);
				tvTokens = itemView.findViewById(R.id.tvTokens);
				tvModelName = itemView.findViewById(R.id.tvModelName);
				layoutActions = itemView.findViewById(R.id.layoutActions);
				btnCopy = itemView.findViewById(R.id.btnCopy);
				btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
				btnDelete = itemView.findViewById(R.id.btnDelete);

				if (tvMessage != null) {
					tvMessage.setOnLongClickListener(null);
					tvMessage.setLongClickable(true);
				}
			}

			public void bind(final ChatMessage message) {
				// 设置消息内容
				if (tvMessage != null) {
					if (message.isStreaming()) {
						tvMessage.setText(message.getContent() + " ▌");
					} else {
						markwon.setMarkdown(tvMessage, message.getContent());
					}
				}

				// 设置模型名称
				if (tvModelName != null && message.getModel() != null) {
					tvModelName.setText(message.getModel());
				}

				// 设置时间和tokens
				if (tvTime != null) {
					tvTime.setText(message.getFormattedTime());
				}
				if (tvTokens != null) {
					tvTokens.setText(" • " + message.getTokensText());
				}

				// 设置操作按钮可见性
				if (layoutActions != null) {
					if (message.isStreaming()) {
						layoutActions.setVisibility(View.GONE);
					} else {
						layoutActions.setVisibility(View.VISIBLE);
					}
				}

				// 复制按钮
				if (btnCopy != null) {
					btnCopy.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								copyToClipboard(message.getContent());
								Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
							}
						});
				}

				// 重新生成按钮
				if (btnRegenerate != null) {
					btnRegenerate.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								regenerateMessage(message);
							}
						});
				}

				// 删除按钮
				if (btnDelete != null) {
					btnDelete.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								deleteMessagePair(message);
							}
						});
				}
			}
		}

// 添加删除消息对的方法
		// 在删除消息对的方法中确保保存
		private void deleteMessagePair(ChatMessage message) {
			int messageIndex = messages.indexOf(message);
			if (messageIndex == -1) return;

			// 找到要删除的消息对
			userIndex = -1;
			aiIndex = -1;

			if (message.getType() == ChatMessage.TYPE_USER) {
				userIndex = messageIndex;
				// 找对应的AI消息
				if (messageIndex + 1 < messages.size()) {
					ChatMessage nextMessage = messages.get(messageIndex + 1);
					if (nextMessage.getType() == ChatMessage.TYPE_ASSISTANT) {
						aiIndex = messageIndex + 1;
					}
				}
			} else if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
				aiIndex = messageIndex;
				// 找对应的用户消息
				if (messageIndex > 0) {
					ChatMessage prevMessage = messages.get(messageIndex - 1);
					if (prevMessage.getType() == ChatMessage.TYPE_USER) {
						userIndex = messageIndex - 1;
					}
				}
			}

			if (userIndex == -1 && aiIndex == -1) {
				Toast.makeText(getActivity(), "无法删除此消息", Toast.LENGTH_SHORT).show();
				return;
			}

			// 确认删除
			new android.app.AlertDialog.Builder(getActivity())
				.setTitle("确认删除")
				.setMessage("确定要删除这对消息吗？")
				.setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(android.content.DialogInterface dialog, int which) {
						// 安全删除消息对
						List<ChatMessage> messagesToRemove = new ArrayList<>();

						if (userIndex != -1 && userIndex < messages.size()) {
							messagesToRemove.add(messages.get(userIndex));
						}
						if (aiIndex != -1 && aiIndex < messages.size()) {
							messagesToRemove.add(messages.get(aiIndex));
						}

						// 从列表中移除
						messages.removeAll(messagesToRemove);
						currentConversation.getMessages().removeAll(messagesToRemove);

						// 关键：立即持久化（通过ChatManager的JSON序列化）
						chatManager.saveConversation(currentConversation);

						// 更新UI
						if (adapter != null) {
							adapter.safeNotifyDataSetChanged();
						}

						Toast.makeText(getActivity(), "已删除消息对", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("取消", null)
				.show();
		}


	}

	// 复制到剪贴板
	private void copyToClipboard(String text) {
		try {
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("AI回复", text);
			if (clipboard != null) {
				clipboard.setPrimaryClip(clip);
			}
		} catch (Exception e) {
			AppLogger.e("ChatFragment", "复制失败", e);
		}
	}

// 在ChatFragment.java中修改重新生成方法
	private void regenerateMessage(ChatMessage message) {
		AppLogger.d("ChatFragment", "重新生成消息，类型: " + message.getType());

		// 检查供应商和模型选择
		if (TextUtils.isEmpty(currentProviderId) || TextUtils.isEmpty(currentModel)) {
			Toast.makeText(getActivity(), "请先选择供应商和模型", Toast.LENGTH_SHORT).show();
			return;
		}

		// 找到对应的用户消息
		String userMessageContent = null;
		int aiMessageIndex = -1;

		if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
			// AI消息：找前一条用户消息
			int messageIndex = messages.indexOf(message);
			if (messageIndex > 0) {
				ChatMessage userMessage = messages.get(messageIndex - 1);
				if (userMessage.getType() == ChatMessage.TYPE_USER) {
					userMessageContent = userMessage.getContent();
					aiMessageIndex = messageIndex;
				}
			}
		} else if (message.getType() == ChatMessage.TYPE_USER) {
			// 用户消息：直接使用当前消息内容，找后面的AI消息
			userMessageContent = message.getContent();
			int messageIndex = messages.indexOf(message);
			if (messageIndex + 1 < messages.size()) {
				ChatMessage aiMessage = messages.get(messageIndex + 1);
				if (aiMessage.getType() == ChatMessage.TYPE_ASSISTANT) {
					aiMessageIndex = messageIndex + 1;
				}
			}
		}

		if (userMessageContent == null) {
			Toast.makeText(getActivity(), "无法重新生成此消息", Toast.LENGTH_SHORT).show();
			return;
		}

		// 安全删除AI消息
		if (aiMessageIndex != -1 && aiMessageIndex < messages.size()) {
			ChatMessage aiMessageToRemove = messages.get(aiMessageIndex);
			messages.remove(aiMessageIndex);
			currentConversation.getMessages().remove(aiMessageToRemove);

			// 立即保存对话
			chatManager.saveConversation(currentConversation);

			// 使用安全方式更新UI
			if (adapter != null) {
				adapter.safeNotifyDataSetChanged();
			}

			AppLogger.d("ChatFragment", "删除AI消息，位置: " + aiMessageIndex);
		}

		// 重新发送用户消息 - 不创建新的用户消息
		sendRegeneratedMessage(userMessageContent, currentProviderId, currentModel);
	}


	// 在ChatFragment类中添加
	public void setCurrentProviderAndModel(String providerId, String model) {
		this.currentProviderId = providerId;
		this.currentModel = model;
		AppLogger.d("ChatFragment", "设置供应商和模型: " + providerId + ", " + model);
	}





}
