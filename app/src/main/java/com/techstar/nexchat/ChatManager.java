package com.techstar.nexchat;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import com.techstar.nexchat.model.ChatConversation;
import com.techstar.nexchat.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatManager {

    private static final String PREF_NAME = "chat_conversations";
    private static final String KEY_CONVERSATION_IDS = "conversation_ids";
    private static final String KEY_CURRENT_CONVERSATION = "current_conversation_id";

    private static ChatManager instance;
    private Context context;

    public static ChatManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatManager(context);
        }
        return instance;
    }

    private ChatManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // 保存对话
    public void saveConversation(ChatConversation conversation) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            JSONObject conversationJson = conversationToJson(conversation);
            prefs.edit().putString(conversation.getId(), conversationJson.toString()).apply();

            // 添加到对话ID列表
            addConversationId(conversation.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 加载对话
    public ChatConversation loadConversation(String conversationId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(conversationId, "");
            if (!jsonStr.isEmpty()) {
                return jsonToConversation(new JSONObject(jsonStr));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 删除对话
    public void deleteConversation(String conversationId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            // 从ID列表中移除
            removeConversationId(conversationId);

            // 删除对话数据
            prefs.edit().remove(conversationId).apply();

            // 如果删除的是当前对话，清除当前对话设置
            if (conversationId.equals(getCurrentConversationId())) {
                setCurrentConversationId("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 置顶/取消置顶对话
    public void togglePinConversation(String conversationId) {
        ChatConversation conversation = loadConversation(conversationId);
        if (conversation != null) {
            conversation.setPinned(!conversation.isPinned());
            saveConversation(conversation);
        }
    }

    // 清空所有对话
    public void clearAllConversations() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String conversationIds = prefs.getString(KEY_CONVERSATION_IDS, "");

            if (!conversationIds.isEmpty()) {
                String[] ids = conversationIds.split(",");
                for (String id : ids) {
                    prefs.edit().remove(id).apply();
                }
            }

            prefs.edit().remove(KEY_CONVERSATION_IDS).remove(KEY_CURRENT_CONVERSATION).apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 私有方法：对话转JSON
    private JSONObject conversationToJson(ChatConversation conversation) throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", conversation.getId());
        json.put("title", conversation.getTitle());
        json.put("createTime", conversation.getCreateTime());
        json.put("updateTime", conversation.getUpdateTime());
        json.put("providerId", conversation.getProviderId());
        json.put("model", conversation.getModel());
        json.put("messageCount", conversation.getMessageCount());
        json.put("isPinned", conversation.isPinned());

        // 保存消息列表
        JSONArray messagesArray = new JSONArray();
        for (ChatMessage message : conversation.getMessages()) {
            JSONObject messageJson = new JSONObject();
            messageJson.put("id", message.getId());
            messageJson.put("type", message.getType());
            messageJson.put("content", message.getContent());
            messageJson.put("timestamp", message.getTimestamp());
            messageJson.put("isStreaming", message.isStreaming());
            messageJson.put("model", message.getModel());
            messagesArray.put(messageJson);
        }
        json.put("messages", messagesArray);

        return json;
    }

    // 私有方法：JSON转对话
    private ChatConversation jsonToConversation(JSONObject json) throws Exception {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(json.getString("id"));
        conversation.setTitle(json.getString("title"));
        conversation.setCreateTime(json.getLong("createTime"));
        conversation.setUpdateTime(json.getLong("updateTime"));
        conversation.setProviderId(json.optString("providerId"));
        conversation.setModel(json.optString("model"));
        conversation.setMessageCount(json.optInt("messageCount"));
        conversation.setPinned(json.optBoolean("isPinned"));

        // 加载消息列表
        JSONArray messagesArray = json.getJSONArray("messages");
        for (int i = 0; i < messagesArray.length(); i++) {
            JSONObject messageJson = messagesArray.getJSONObject(i);
            ChatMessage message = new ChatMessage();
            message.setId(messageJson.getString("id"));
            message.setType(messageJson.getInt("type"));
            message.setContent(messageJson.getString("content"));
            message.setTimestamp(messageJson.getLong("timestamp"));
            message.setStreaming(messageJson.optBoolean("isStreaming"));
            message.setModel(messageJson.optString("model"));
            conversation.addMessage(message);
        }

        return conversation;
    }

    // 私有方法：管理对话ID列表
    private void addConversationId(String conversationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existingIds = prefs.getString(KEY_CONVERSATION_IDS, "");

        if (!existingIds.contains(conversationId)) {
            if (!existingIds.isEmpty()) {
                existingIds = conversationId + "," + existingIds;
            } else {
                existingIds = conversationId;
            }
            prefs.edit().putString(KEY_CONVERSATION_IDS, existingIds).apply();
        }
    }

    private void removeConversationId(String conversationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existingIds = prefs.getString(KEY_CONVERSATION_IDS, "");

        if (!existingIds.isEmpty()) {
            List<String> idList = new ArrayList<>();
            Collections.addAll(idList, existingIds.split(","));
            idList.remove(conversationId);
            String newIds = android.text.TextUtils.join(",", idList);
            prefs.edit().putString(KEY_CONVERSATION_IDS, newIds).apply();
        }
    }

    private String getCurrentConversationId() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENT_CONVERSATION, "");
    }

    private void setCurrentConversationId(String conversationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_CONVERSATION, conversationId).apply();
    }

	// 加载所有对话（按更新时间倒序，去掉置顶逻辑）
	public List<ChatConversation> loadAllConversations() {
		List<ChatConversation> conversations = new ArrayList<>();
		try {
			SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
			String conversationIds = prefs.getString(KEY_CONVERSATION_IDS, "");

			if (!conversationIds.isEmpty()) {
				String[] ids = conversationIds.split(",");
				for (String id : ids) {
					ChatConversation conversation = loadConversation(id);
					if (conversation != null) {
						conversations.add(conversation);
					}
				}
			}

			// 按更新时间倒序排序（最新的在前面）
			Collections.sort(conversations, new Comparator<ChatConversation>() {
					@Override
					public int compare(ChatConversation c1, ChatConversation c2) {
						return Long.compare(c2.getUpdateTime(), c1.getUpdateTime());
					}
				});

		} catch (Exception e) {
			e.printStackTrace();
		}
		return conversations;
	}

	// 获取当前对话
	public ChatConversation getCurrentConversation() {
		String currentId = getCurrentConversationId();
		if (!currentId.isEmpty()) {
			return loadConversation(currentId);
		}
		return null;
	}
	

	// 创建新对话
	public ChatConversation createNewConversation() {
		ChatConversation conversation = new ChatConversation("新对话");
		saveConversation(conversation);
		setCurrentConversationId(conversation.getId());
		AppLogger.d("ChatManager", "创建新对话: " + conversation.getId());
		return conversation;
	}

	// 设置当前对话
	public void setCurrentConversation(String conversationId) {
		if (conversationId != null && !conversationId.isEmpty()) {
			setCurrentConversationId(conversationId);
			AppLogger.d("ChatManager", "设置当前对话: " + conversationId);
		}
	}
}
