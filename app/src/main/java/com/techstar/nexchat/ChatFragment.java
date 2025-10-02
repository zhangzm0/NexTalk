package com.techstar.nexchat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
    private ChatManager chatManager;
    private ImageButton btnPause;
    private boolean isStreaming = false;
    private Call currentCall;
    private String currentProviderId = "";
    private String currentModel = "";
    private boolean isInitialized = false;
	
	private int userIndex = -1;
	private int aiIndex = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        initHttpClient();
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
		btnPause = view.findViewById(R.id.btnPause);

		// 关键：禁用 RecyclerView 的自动状态保存
		recyclerView.setSaveEnabled(false);

		// 最简单的LayoutManager，不干预任何滚动行为
		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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

    private void initMarkwon() {
        markwon = Markwon.builder(getActivity())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build();
    }

    private void initHttpClient() {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.readTimeout(60, java.util.concurrent.TimeUnit.SECONDS);
            builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            client = builder.build();
        } catch (Exception e) {
            client = new OkHttpClient();
        }
    }

    @Override
	public void onResume() {
		super.onResume();
		AppLogger.d("ChatFragment", "onResume called");

		// 只同步模型选择，不刷新对话
		if (getActivity() instanceof MainActivity) {
			MainActivity mainActivity = (MainActivity) getActivity();
			if (mainActivity.inputFragment != null) {
				String providerId = mainActivity.inputFragment.getCurrentProviderId();
				String model = mainActivity.inputFragment.getCurrentModel();
				if (!TextUtils.isEmpty(providerId) && !TextUtils.isEmpty(model)) {
					this.currentProviderId = providerId;
					this.currentModel = model;
					AppLogger.d("ChatFragment", "从InputFragment同步选择: " + providerId + ", " + model);
				}
			}
		}

		// 完全移除 checkAndUpdateConversation() 调用
	}

    public void sendMessage(String messageText, String providerId, String model) {
        this.currentProviderId = providerId;
        this.currentModel = model;

        if (!isInitialized) {
            ensureInitialized();
        }

        if (TextUtils.isEmpty(messageText)) return;

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
        adapter.notifyDataSetChanged();

        // 创建AI消息（流式响应）
        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        aiMessage.setStreaming(true);
        aiMessage.setModel(model);
        messages.add(aiMessage);
        currentConversation.addMessage(aiMessage);

        // 立即保存包含AI消息的对话
        chatManager.saveConversation(currentConversation);
        adapter.notifyDataSetChanged();

        // 发送API请求
        sendChatRequest(messageText, providerId, model, aiMessage);
    }

    private void sendRegeneratedMessage(String messageText, String providerId, String model) {
        this.currentProviderId = providerId;
        this.currentModel = model;

        if (TextUtils.isEmpty(messageText)) return;

        // 创建AI消息（流式响应）
        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        aiMessage.setStreaming(true);
        aiMessage.setModel(model);
        messages.add(aiMessage);
        currentConversation.addMessage(aiMessage);

        // 立即保存对话
        chatManager.saveConversation(currentConversation);
        adapter.notifyDataSetChanged();

        // 发送API请求
        sendChatRequest(messageText, providerId, model, aiMessage);
    }

    private void sendChatRequest(String message, String providerId, String model, final ChatMessage aiMessage) {
        try {
            // 显示暂停按钮
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (btnPause != null) {
								btnPause.setVisibility(View.VISIBLE);
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

            currentCall = client.newCall(request);
            isStreaming = true;

            currentCall.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, final IOException e) {
						if (!call.isCanceled()) {
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
						if (!response.isSuccessful()) {
							String errorBody = response.body().string();
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

						processStreamResponse(response, aiMessage);
					}
				});

        } catch (Exception e) {
            handleError("请求构建失败: " + e.getMessage());
            hidePauseButton();
        }
    }

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

                                    final String currentContent = content.toString();
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
												@Override
												public void run() {
													if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
														aiMessage.setContent(currentContent);
														aiMessage.setStreaming(true);
														adapter.notifyItemChanged(aiMessageIndex);
													}
												}
											});
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
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
							if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
								aiMessage.setContent(finalContent);
								aiMessage.setStreaming(false);

								if (finalHasTokens) {
									aiMessage.setTokensInfo(finalPromptTokens, finalCompletionTokens, finalTotalTokens);
								}
								hidePauseButton();
								adapter.notifyItemChanged(aiMessageIndex);
								chatManager.saveConversation(currentConversation);
							}
						}
					});
            }

        } catch (final Exception e) {
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

    private void hidePauseButton() {
        isStreaming = false;
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (btnPause != null) {
							btnPause.setVisibility(View.GONE);
						}
					}
				});
        }
    }

    private void handleError(final String error) {
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
									adapter.notifyItemChanged(messages.size() - 1);
								}
							}

							Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							// 忽略错误处理中的错误
						}
					}
				});
        }
    }

    private ApiProvider loadProviderById(String providerId) {
        if (getActivity() == null) {
            return null;
        }

        try {
            String name = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_name", "");
            String url = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_url", "");
            String key = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_key", "");
            int modelCount = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getInt(providerId + "_model_count", 0);

            if (!name.isEmpty() && !url.isEmpty()) {
                ApiProvider provider = new ApiProvider(name, url, key);
                provider.setId(providerId);

                for (int i = 0; i < modelCount; i++) {
                    String model = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                        .getString(providerId + "_model_" + i, "");
                    if (!model.isEmpty()) {
                        provider.getModels().add(model);
                    }
                }

                return provider;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    private void loadOrCreateConversation() {
        if (chatManager == null) {
            chatManager = ChatManager.getInstance(getActivity());
        }

        currentConversation = chatManager.getCurrentConversation();
        if (currentConversation == null) {
            currentConversation = chatManager.createNewConversation();
        }

        if (messages == null) {
            messages = new ArrayList<>();
        } else {
            messages.clear();
        }
        messages.addAll(currentConversation.getMessages());

        if (adapter == null) {
            adapter = new MessageAdapter(messages);
            if (recyclerView != null) {
                recyclerView.setAdapter(adapter);
            }
        } else {
            adapter.notifyDataSetChanged();
        }

        if (tvChatTitle != null) {
            tvChatTitle.setText(currentConversation.getTitle());
        }
    }

    public void ensureInitialized() {
        if (!isInitialized && getView() != null) {
            initViews(getView());
            initMarkwon();
            loadOrCreateConversation();
            isInitialized = true;
        }
    }

    public void refreshConversation() {
        loadOrCreateConversation();
    }

    public void setCurrentProviderAndModel(String providerId, String model) {
        this.currentProviderId = providerId;
        this.currentModel = model;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isStreaming = false;
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    // 简单的Adapter，不包含任何滚动逻辑
    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_USER = 0;
        private static final int TYPE_ASSISTANT = 1;

        private final List<ChatMessage> messages;

        public MessageAdapter(List<ChatMessage> messages) {
            this.messages = messages != null ? messages : new ArrayList<ChatMessage>();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= messages.size()) {
                return TYPE_ASSISTANT;
            }
            return messages.get(position).getType() == ChatMessage.TYPE_USER ? TYPE_USER : TYPE_ASSISTANT;
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

        @Override
        public int getItemCount() {
            return messages.size();
        }

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
            }

            public void bind(final ChatMessage message) {
                if (tvMessage != null) {
                    tvMessage.setText(message.getContent());
                }
                if (tvTime != null) {
                    tvTime.setText(message.getFormattedTime());
                }

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
            }

            public void bind(final ChatMessage message) {
                if (tvMessage != null) {
                    if (message.isStreaming()) {
                        tvMessage.setText(message.getContent() + " ▌");
                    } else {
                        markwon.setMarkdown(tvMessage, message.getContent());
                    }
                }

                if (tvModelName != null && message.getModel() != null) {
                    tvModelName.setText(message.getModel());
                }

                if (tvTime != null) {
                    tvTime.setText(message.getFormattedTime());
                }
                if (tvTokens != null) {
                    tvTokens.setText(" • " + message.getTokensText());
                }

                if (layoutActions != null) {
                    if (message.isStreaming()) {
                        layoutActions.setVisibility(View.GONE);
                    } else {
                        layoutActions.setVisibility(View.VISIBLE);
                    }
                }

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
    }

    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI回复", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            // 忽略复制错误
        }
    }

    private void regenerateMessage(ChatMessage message) {
        if (TextUtils.isEmpty(currentProviderId) || TextUtils.isEmpty(currentModel)) {
            Toast.makeText(getActivity(), "请先选择供应商和模型", Toast.LENGTH_SHORT).show();
            return;
        }

        String userMessageContent = null;
        int aiMessageIndex = -1;

        if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
            int messageIndex = messages.indexOf(message);
            if (messageIndex > 0) {
                ChatMessage userMessage = messages.get(messageIndex - 1);
                if (userMessage.getType() == ChatMessage.TYPE_USER) {
                    userMessageContent = userMessage.getContent();
                    aiMessageIndex = messageIndex;
                }
            }
        } else if (message.getType() == ChatMessage.TYPE_USER) {
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

        if (aiMessageIndex != -1 && aiMessageIndex < messages.size()) {
            ChatMessage aiMessageToRemove = messages.get(aiMessageIndex);
            messages.remove(aiMessageIndex);
            currentConversation.getMessages().remove(aiMessageToRemove);
            chatManager.saveConversation(currentConversation);
            adapter.notifyDataSetChanged();
        }

        sendRegeneratedMessage(userMessageContent, currentProviderId, currentModel);
    }

    private void deleteMessagePair(ChatMessage message) {
        int messageIndex = messages.indexOf(message);
        if (messageIndex == -1) return;

        userIndex = -1;
        aiIndex = -1;

        if (message.getType() == ChatMessage.TYPE_USER) {
            userIndex = messageIndex;
            if (messageIndex + 1 < messages.size()) {
                ChatMessage nextMessage = messages.get(messageIndex + 1);
                if (nextMessage.getType() == ChatMessage.TYPE_ASSISTANT) {
                    aiIndex = messageIndex + 1;
                }
            }
        } else if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
            aiIndex = messageIndex;
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

        new android.app.AlertDialog.Builder(getActivity())
            .setTitle("确认删除")
            .setMessage("确定要删除这对消息吗？")
            .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    List<ChatMessage> messagesToRemove = new ArrayList<>();

                    if (userIndex != -1 && userIndex < messages.size()) {
                        messagesToRemove.add(messages.get(userIndex));
                    }
                    if (aiIndex != -1 && aiIndex < messages.size()) {
                        messagesToRemove.add(messages.get(aiIndex));
                    }

                    messages.removeAll(messagesToRemove);
                    currentConversation.getMessages().removeAll(messagesToRemove);
                    chatManager.saveConversation(currentConversation);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getActivity(), "已删除消息对", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
