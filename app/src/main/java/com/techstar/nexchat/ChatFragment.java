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

    // 核心组件
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private TextView tvChatTitle;
    private ImageButton btnPause;
    
    // 数据
    private ChatConversation currentConversation;
    private List<ChatMessage> messages = new ArrayList<>();
    
    // 网络
    private OkHttpClient client;
    private Call currentCall;
    
    // 状态
    private boolean isStreaming = false;
    private String currentProviderId = "";
    private String currentModel = "";
    
    // 管理器
    private ChatManager chatManager;
    private Markwon markwon;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        AppLogger.d("ChatFragment", "onCreateView - 开始创建视图");
        
        initCoreComponents();
        initViews(view);
        loadConversation();
        
        AppLogger.d("ChatFragment", "onCreateView - 视图创建完成");
        return view;
    }

    /**
     * 初始化核心组件 - 保持简单
     */
    private void initCoreComponents() {
        // HTTP客户端
        client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        
        // Markdown渲染
        markwon = Markwon.builder(getActivity())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build();
            
        // 聊天管理器
        chatManager = ChatManager.getInstance(getActivity());
        
        AppLogger.d("ChatFragment", "核心组件初始化完成");
    }

    /**
     * 初始化视图 - 极简版本
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);
        btnPause = view.findViewById(R.id.btnPause);
        
        AppLogger.d("ChatFragment", "开始初始化视图");

        // 最简单的RecyclerView配置 - 不干预任何滚动行为
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        
        // 禁用所有可能引起自动滚动的功能
        recyclerView.setSaveEnabled(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // 适配器
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);
        
        // 暂停按钮
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePauseClick();
            }
        });
        
        AppLogger.d("ChatFragment", "视图初始化完成");
    }

    /**
     * 加载对话 - 简单直接
     */
    private void loadConversation() {
        AppLogger.d("ChatFragment", "开始加载对话");
        
        currentConversation = chatManager.getCurrentConversation();
        if (currentConversation == null) {
            currentConversation = chatManager.createNewConversation();
            AppLogger.d("ChatFragment", "创建新对话: " + currentConversation.getId());
        } else {
            AppLogger.d("ChatFragment", "加载现有对话: " + currentConversation.getTitle());
        }
        
        // 更新消息列表
        messages.clear();
        messages.addAll(currentConversation.getMessages());
        
        // 更新UI
        if (tvChatTitle != null) {
            tvChatTitle.setText(currentConversation.getTitle());
        }
        
        adapter.notifyDataSetChanged();
        AppLogger.d("ChatFragment", "对话加载完成，消息数: " + messages.size());
    }

    /**
     * 发送消息 - 核心功能
     */
    public void sendMessage(String messageText, String providerId, String model) {
        if (TextUtils.isEmpty(messageText)) return;
        
        AppLogger.d("ChatFragment", "发送消息 - 供应商: " + providerId + ", 模型: " + model);
        
        this.currentProviderId = providerId;
        this.currentModel = model;
        
        // 添加用户消息
        ChatMessage userMessage = new ChatMessage(ChatMessage.TYPE_USER, messageText);
        addMessageToConversation(userMessage);
        
        // 更新标题（如果是第一条消息）
        updateConversationTitle(messageText);
        
        // 添加AI消息占位符
        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        aiMessage.setStreaming(true);
        aiMessage.setModel(model);
        addMessageToConversation(aiMessage);
        
        // 发送API请求
        sendApiRequest(messageText, providerId, model, aiMessage);
    }

    private void addMessageToConversation(ChatMessage message) {
        messages.add(message);
        currentConversation.addMessage(message);
        chatManager.saveConversation(currentConversation);
        adapter.notifyItemInserted(messages.size() - 1);
    }

    private void updateConversationTitle(String firstMessage) {
        if (currentConversation.getMessageCount() == 1 && "新对话".equals(currentConversation.getTitle())) {
            String newTitle = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
            currentConversation.setTitle(newTitle);
            if (tvChatTitle != null) {
                tvChatTitle.setText(newTitle);
            }
        }
    }

    /**
     * API请求处理
     */
    private void sendApiRequest(String message, String providerId, String model, final ChatMessage aiMessage) {
        try {
            showPauseButton();
            
            ApiProvider provider = loadProviderById(providerId);
            if (provider == null) {
                handleError("供应商不存在");
                return;
            }
            
            JSONObject requestBody = buildRequestBody(message, model);
            Request request = buildRequest(provider, requestBody);
            
            currentCall = client.newCall(request);
            isStreaming = true;
            
            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (!call.isCanceled()) {
                        handleError("网络请求失败: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        handleError("API错误: " + response.code());
                        return;
                    }
                    processStreamResponse(response, aiMessage);
                }
            });
            
        } catch (Exception e) {
            handleError("请求构建失败: " + e.getMessage());
        }
    }

    private JSONObject buildRequestBody(String message, String model) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);
        
        JSONArray messagesArray = new JSONArray();
        
        // 添加上下文消息（最近10条）
        if (messages.size() > 1) {
            int startIndex = Math.max(0, messages.size() - 10);
            for (int i = startIndex; i < messages.size() - 1; i++) {
                ChatMessage msg = messages.get(i);
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
                msgObj.put("content", msg.getContent());
                messagesArray.put(msgObj);
            }
        }
        
        // 添加当前消息
        JSONObject currentMsg = new JSONObject();
        currentMsg.put("role", "user");
        currentMsg.put("content", message);
        messagesArray.put(currentMsg);
        
        requestBody.put("messages", messagesArray);
        return requestBody;
    }

    private Request buildRequest(ApiProvider provider, JSONObject requestBody) {
        String baseUrl = provider.getApiUrl().trim();
        String apiUrl = baseUrl.endsWith("/") ? 
            baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            requestBody.toString()
        );
        
        return new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer " + provider.getApiKey().trim())
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();
    }

    private void processStreamResponse(Response response, final ChatMessage aiMessage) {
        try {
            final StringBuilder content = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body().byteStream())
            );
            
            String line;
            final int aiMessageIndex = messages.indexOf(aiMessage);
            
            while ((line = reader.readLine()) != null && isStreaming) {
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    final String jsonStr = line.substring(6);
                    if (!jsonStr.trim().isEmpty()) {
                        processChunk(jsonStr, content, aiMessage, aiMessageIndex);
                    }
                }
            }
            
            // 流结束
            finishStreaming(aiMessage, content.toString());
            
        } catch (Exception e) {
            handleError("响应解析失败: " + e.getMessage());
        }
    }

    private void processChunk(final String jsonStr, final StringBuilder content, final ChatMessage aiMessage, final int aiMessageIndex) {
        try {
            JSONObject data = new JSONObject(jsonStr);
            
            // 处理token统计
            if (data.has("usage")) {
                JSONObject usage = data.getJSONObject("usage");
                aiMessage.setTokensInfo(
                    usage.optInt("prompt_tokens", 0),
                    usage.optInt("completion_tokens", 0),
                    usage.optInt("total_tokens", 0)
                );
            }
            
            // 处理内容
            JSONArray choices = data.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                if (delta.has("content")) {
                    String chunk = delta.getString("content");
                    content.append(chunk);
                    
                    // 更新UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (aiMessageIndex >= 0 && aiMessageIndex < messages.size()) {
                                    aiMessage.setContent(content.toString());
                                    adapter.notifyItemChanged(aiMessageIndex);
                                }
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            // 忽略单个chunk的解析错误
        }
    }

    private void finishStreaming(final ChatMessage aiMessage, final String finalContent) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aiMessage.setContent(finalContent);
                    aiMessage.setStreaming(false);
                    hidePauseButton();
                    adapter.notifyItemChanged(messages.indexOf(aiMessage));
                    chatManager.saveConversation(currentConversation);
                }
            });
        }
    }

    /**
     * 简单的生命周期方法
     */
    // 在 ChatFragment.java 的 onResume 中添加额外检查
	@Override
	public void onResume() {
		super.onResume();
		AppLogger.d("ChatFragment", "onResume - 开始");

		// 记录当前状态
		if (recyclerView != null) {
			AppLogger.d("ChatFragment", "RecyclerView状态: " + 
						"isLayoutRequested=" + recyclerView.isLayoutRequested() +
						", isAttachedToWindow=" + recyclerView.isAttachedToWindow());
		}

		// 只同步模型选择，不刷新对话
		if (getActivity() instanceof MainActivity) {
			MainActivity mainActivity = (MainActivity) getActivity();
			if (mainActivity.inputFragment != null) {
				String providerId = mainActivity.inputFragment.getCurrentProviderId();
				String model = mainActivity.inputFragment.getCurrentModel();
				if (!TextUtils.isEmpty(providerId) && !TextUtils.isEmpty(model)) {
					this.currentProviderId = providerId;
					this.currentModel = model;
					AppLogger.d("ChatFragment", "同步模型选择: " + providerId + ", " + model);
				}
			}
		}

		AppLogger.d("ChatFragment", "onResume - 结束");
	}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AppLogger.d("ChatFragment", "onDestroyView - 清理资源");
        
        isStreaming = false;
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    /**
     * 工具方法
     */
    private ApiProvider loadProviderById(String providerId) {
        try {
            String name = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_name", "");
            String url = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_url", "");
            String key = getActivity().getSharedPreferences("api_providers", Context.MODE_PRIVATE)
                .getString(providerId + "_key", "");
                
            if (!name.isEmpty() && !url.isEmpty()) {
                ApiProvider provider = new ApiProvider(name, url, key);
                provider.setId(providerId);
                return provider;
            }
        } catch (Exception e) {
            AppLogger.e("ChatFragment", "加载供应商失败", e);
        }
        return null;
    }

    private void handlePauseClick() {
        AppLogger.d("ChatFragment", "暂停按钮点击");
        if (isStreaming && currentCall != null) {
            currentCall.cancel();
            isStreaming = false;
            hidePauseButton();
            
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

    private void showPauseButton() {
        isStreaming = true;
        if (btnPause != null) {
            btnPause.setVisibility(View.VISIBLE);
        }
    }

    private void hidePauseButton() {
        isStreaming = false;
        if (btnPause != null) {
            btnPause.setVisibility(View.GONE);
        }
    }

    private void handleError(final String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hidePauseButton();
                    
                    if (!messages.isEmpty()) {
                        ChatMessage lastMessage = messages.get(messages.size() - 1);
                        if (lastMessage.isStreaming()) {
                            lastMessage.setContent("错误: " + error);
                            lastMessage.setStreaming(false);
                            adapter.notifyItemChanged(messages.size() - 1);
                        }
                    }
                    
                    Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * 公共接口方法
     */
    public void refreshConversation() {
        AppLogger.d("ChatFragment", "手动刷新对话");
        loadConversation();
    }

    public void setCurrentProviderAndModel(String providerId, String model) {
        this.currentProviderId = providerId;
        this.currentModel = model;
        AppLogger.d("ChatFragment", "设置供应商和模型: " + providerId + ", " + model);
    }

    /**
     * 极简适配器 - 不包含任何滚动逻辑
     */
    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        
        private static final int TYPE_USER = 0;
        private static final int TYPE_ASSISTANT = 1;

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

    /**
     * ViewHolder类
     */
    private class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage, tvTime;
        private Button btnCopy, btnRegenerate, btnDelete;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            
            setupClickListeners();
        }

        public void bind(ChatMessage message) {
            tvMessage.setText(message.getContent());
            tvTime.setText(message.getFormattedTime());
        }

        private void setupClickListeners() {
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        copyToClipboard(messages.get(position).getContent());
                    }
                }
            });
            
            btnRegenerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        regenerateMessage(messages.get(position));
                    }
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteMessagePair(messages.get(position));
                    }
                }
            });
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
            
            setupClickListeners();
        }

        public void bind(ChatMessage message) {
            // 消息内容
            if (message.isStreaming()) {
                tvMessage.setText(message.getContent() + " ▌");
            } else {
                //markwon.setMarkdown(tvMessage, message.getContent());
				tvMessage.setText(message.getContent());
            }
            
            // 元数据
            tvTime.setText(message.getFormattedTime());
            tvTokens.setText(" • " + message.getTokensText());
            
            if (message.getModel() != null) {
                tvModelName.setText(message.getModel());
            }
            
            // 操作按钮可见性
            if (message.isStreaming()) {
                layoutActions.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.VISIBLE);
            }
        }

        private void setupClickListeners() {
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        copyToClipboard(messages.get(position).getContent());
                    }
                }
            });
            
            btnRegenerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        regenerateMessage(messages.get(position));
                    }
                }
            });
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteMessagePair(messages.get(position));
                    }
                }
            });
        }
    }

    /**
     * 消息操作
     */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI回复", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            AppLogger.e("ChatFragment", "复制失败", e);
        }
    }

    private void regenerateMessage(ChatMessage message) {
        if (TextUtils.isEmpty(currentProviderId) || TextUtils.isEmpty(currentModel)) {
            Toast.makeText(getActivity(), "请先选择供应商和模型", Toast.LENGTH_SHORT).show();
            return;
        }

        String userMessageContent = findUserMessageForRegeneration(message);
        if (userMessageContent == null) {
            Toast.makeText(getActivity(), "无法重新生成此消息", Toast.LENGTH_SHORT).show();
            return;
        }

        removeAIMessageForRegeneration(message);
        sendRegeneratedMessage(userMessageContent);
    }

    private String findUserMessageForRegeneration(ChatMessage message) {
        int index = messages.indexOf(message);
        
        if (message.getType() == ChatMessage.TYPE_ASSISTANT && index > 0) {
            ChatMessage userMessage = messages.get(index - 1);
            if (userMessage.getType() == ChatMessage.TYPE_USER) {
                return userMessage.getContent();
            }
        } else if (message.getType() == ChatMessage.TYPE_USER) {
            return message.getContent();
        }
        
        return null;
    }

    private void removeAIMessageForRegeneration(ChatMessage message) {
        int index = messages.indexOf(message);
        
        if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
            // 直接删除AI消息
            messages.remove(index);
            currentConversation.getMessages().remove(index);
        } else if (message.getType() == ChatMessage.TYPE_USER && index + 1 < messages.size()) {
            // 删除对应的AI消息
            ChatMessage aiMessage = messages.get(index + 1);
            if (aiMessage.getType() == ChatMessage.TYPE_ASSISTANT) {
                messages.remove(index + 1);
                currentConversation.getMessages().remove(index + 1);
            }
        }
        
        chatManager.saveConversation(currentConversation);
        adapter.notifyDataSetChanged();
    }

    private void sendRegeneratedMessage(String userMessageContent) {
        ChatMessage aiMessage = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        aiMessage.setStreaming(true);
        aiMessage.setModel(currentModel);
        addMessageToConversation(aiMessage);
        
        sendApiRequest(userMessageContent, currentProviderId, currentModel, aiMessage);
    }

    private void deleteMessagePair(ChatMessage message) {
        final int index = messages.indexOf(message);
        if (index == -1) return;

        final int userIndex;
        final int aiIndex;

        if (message.getType() == ChatMessage.TYPE_USER) {
            userIndex = index;
            if (index + 1 < messages.size()) {
                ChatMessage nextMessage = messages.get(index + 1);
                if (nextMessage.getType() == ChatMessage.TYPE_ASSISTANT) {
                    aiIndex = index + 1;
                } else {
                    aiIndex = -1;
                }
            } else {
                aiIndex = -1;
            }
        } else if (message.getType() == ChatMessage.TYPE_ASSISTANT) {
            aiIndex = index;
            if (index > 0) {
                ChatMessage prevMessage = messages.get(index - 1);
                if (prevMessage.getType() == ChatMessage.TYPE_USER) {
                    userIndex = index - 1;
                } else {
                    userIndex = -1;
                }
            } else {
                userIndex = -1;
            }
        } else {
            userIndex = -1;
            aiIndex = -1;
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
                    List<ChatMessage> toRemove = new ArrayList<>();
                    if (userIndex != -1) toRemove.add(messages.get(userIndex));
                    if (aiIndex != -1) toRemove.add(messages.get(aiIndex));
                    
                    messages.removeAll(toRemove);
                    currentConversation.getMessages().removeAll(toRemove);
                    chatManager.saveConversation(currentConversation);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getActivity(), "已删除消息对", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
