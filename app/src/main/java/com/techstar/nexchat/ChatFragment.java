package com.techstar.nexchat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import com.techstar.nexchat.model.ChatConversation;
import com.techstar.nexchat.model.ChatMessage;
import org.json.JSONArray;
import org.json.JSONObject;
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
import com.techstar.nexchat.model.ApiProvider;
import android.util.Log;
import android.content.Context;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvChatTitle;

    private ChatConversation currentConversation;
    private List<ChatMessage> messages;
    private Markwon markwon;
    private OkHttpClient client;

    

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        progressBar = view.findViewById(R.id.progressBar);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        recyclerView.setAdapter(adapter);

        client = new OkHttpClient();
    }

    private void initMarkwon() {
        markwon = Markwon.builder(getActivity())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build();
    }

    

    

    private void processStreamResponse(Response response, final ChatMessage aiMessage) {
        try {
            String line;
            final StringBuilder content = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body().byteStream())
            );

            while ((line = reader.readLine()) != null) {
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

                                // 更新UI
                                final String currentContent = content.toString();
                                getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											aiMessage.setContent(currentContent);
											aiMessage.setStreaming(true);
											adapter.notifyItemChanged(messages.size() - 1);
											scrollToBottom();
										}
									});
                            }
                        }
                    }
                }
            }

            // 流式响应结束
            getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						aiMessage.setStreaming(false);
						progressBar.setVisibility(View.GONE);
						adapter.notifyItemChanged(messages.size() - 1);

						// 保存对话到ChatManager
						chatManager.saveConversation(currentConversation);

						// 更新对话标题（如果还是默认标题）
						if ("新对话".equals(currentConversation.getTitle()) && content.length() > 10) {
							String newTitle = content.toString().substring(0, Math.min(20, content.length()));
							currentConversation.setTitle(newTitle);
							tvChatTitle.setText(newTitle);
							chatManager.saveConversation(currentConversation);
						}
					}
				});

        } catch (final Exception e) {
            getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						handleError("响应解析失败: " + e.getMessage());
					}
				});
        }
    }

    

    private void scrollToBottom() {
        recyclerView.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (messages.size() > 0) {
						recyclerView.smoothScrollToPosition(messages.size() - 1);
					}
				}
			}, 100);
    }

    

    private void saveConversation(ChatConversation conversation) {
        // 保存对话到SharedPreferences
    }

    

    // MessageAdapter
    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_USER = 0;
        private static final int TYPE_ASSISTANT = 1;

        private List<ChatMessage> messages;

        public MessageAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemViewType(int position) {
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

	

	public void sendMessage(String messageText, String providerId, String model) {
		
		if (!isInitialized) {
            ensureInitialized();
        }

        if (TextUtils.isEmpty(messageText)) return;

        // 确保数据初始化
        if (messages == null) messages = new ArrayList<>();
        if (currentConversation == null) loadOrCreateConversation();
		
		
		// 创建用户消息
		ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, messageText);
		messages.add(userMessage);
		currentConversation.addMessage(userMessage);

		if (adapter != null) {
			adapter.notifyItemInserted(messages.size() - 1);
		}
		scrollToBottom();

		// 创建AI消息（流式响应）
		ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
		aiMessage.setStreaming(true);
		aiMessage.setModel(model);
		messages.add(aiMessage);
		currentConversation.addMessage(aiMessage);

		if (adapter != null) {
			adapter.notifyItemInserted(messages.size() - 1);
		}
		scrollToBottom();

		// 显示加载
		if (progressBar != null) {
			progressBar.setVisibility(View.VISIBLE);
		}

		// 发送API请求
		sendChatRequest(messageText, providerId, model, aiMessage);
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
            // 获取供应商信息 - 修复这里的加载逻辑
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
                
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleError("网络请求失败: " + e.getMessage());
                        }
                    });
                }
                
                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        final String errorBody = response.body().string();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleError("API错误: " + response.code() + " - " + errorBody);
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
    
    // 修复错误处理方法，提供更详细的错误信息
    private void handleError(final String error) {
        Log.e("ChatFragment", "错误: " + error);
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.GONE);
                    
                    if (!messages.isEmpty() && messages.get(messages.size() - 1).isStreaming()) {
                        ChatMessage lastMessage = messages.get(messages.size() - 1);
                        lastMessage.setContent("错误: " + error);
                        lastMessage.setStreaming(false);
                        if (adapter != null) {
                            adapter.notifyItemChanged(messages.size() - 1);
                        }
                    }
                    
                    Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
	
	private ChatManager chatManager;

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
	
	
}



