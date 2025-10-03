package com.techstar.nexchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.techstar.nexchat.model.ChatHistory;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryDao {
    private static final String TAG = "ChatHistoryDao";
    private DatabaseHelper dbHelper;
    private FileLogger logger;
    
    public ChatHistoryDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.logger = FileLogger.getInstance(context);
    }
    
    public long insertChat(ChatHistory chat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        
        try {
            ContentValues values = new ContentValues();
            values.put("title", chat.getTitle());
            values.put("preview", chat.getPreview());
            values.put("timestamp", chat.getTimestamp());
            values.put("message_count", chat.getMessageCount());
            
            id = db.insert(DatabaseHelper.TABLE_CHAT_HISTORY, null, values);
            logger.i(TAG, "Inserted chat: " + chat.getTitle() + " with ID: " + id);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to insert chat", e);
        } finally {
            db.close();
        }
        
        return id;
    }
    
    public List<ChatHistory> getAllChats() {
        List<ChatHistory> chats = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    null, null, null, null, null, "timestamp DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ChatHistory chat = cursorToChat(cursor);
                    chats.add(chat);
                } while (cursor.moveToNext());
            }
            
            logger.i(TAG, "Retrieved " + chats.size() + " chats");
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to retrieve chats", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return chats;
    }
    
    public ChatHistory getChatById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        ChatHistory chat = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    null, "id = ?", new String[]{String.valueOf(id)}, 
                    null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                chat = cursorToChat(cursor);
            }
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to get chat by ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return chat;
    }
    
    public boolean updateChat(ChatHistory chat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            ContentValues values = new ContentValues();
            values.put("title", chat.getTitle());
            values.put("preview", chat.getPreview());
            values.put("timestamp", chat.getTimestamp());
            values.put("message_count", chat.getMessageCount());
            
            int rowsAffected = db.update(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    values, "id = ?", new String[]{String.valueOf(chat.getId())});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Updated chat: " + chat.getTitle() + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to update chat", e);
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public boolean deleteChat(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            // 先删除该聊天相关的所有消息
            db.delete(DatabaseHelper.TABLE_MESSAGES, "chat_id = ?", 
                    new String[]{String.valueOf(id)});
            
            // 再删除聊天记录
            int rowsAffected = db.delete(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    "id = ?", new String[]{String.valueOf(id)});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Deleted chat with ID: " + id + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to delete chat with ID: " + id, e);
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public boolean updateChatPreview(int chatId, String preview) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            ContentValues values = new ContentValues();
            values.put("preview", preview);
            values.put("timestamp", System.currentTimeMillis());
            
            int rowsAffected = db.update(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    values, "id = ?", new String[]{String.valueOf(chatId)});
            
            success = rowsAffected > 0;
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to update chat preview", e);
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public void incrementMessageCount(int chatId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            // 获取当前消息数量
            Cursor cursor = db.query(DatabaseHelper.TABLE_CHAT_HISTORY, 
                    new String[]{"message_count"}, "id = ?", 
                    new String[]{String.valueOf(chatId)}, null, null, null);
            
            int currentCount = 0;
            if (cursor != null && cursor.moveToFirst()) {
                currentCount = cursor.getInt(cursor.getColumnIndexOrThrow("message_count"));
                cursor.close();
            }
            
            // 更新消息数量
            ContentValues values = new ContentValues();
            values.put("message_count", currentCount + 1);
            values.put("timestamp", System.currentTimeMillis());
            
            db.update(DatabaseHelper.TABLE_CHAT_HISTORY, values, "id = ?", 
                    new String[]{String.valueOf(chatId)});
            
        } catch (Exception e) {
            logger.e(TAG, "Failed to increment message count", e);
        } finally {
            db.close();
        }
    }
    
    private ChatHistory cursorToChat(Cursor cursor) {
        ChatHistory chat = new ChatHistory();
        chat.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        chat.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        chat.setPreview(cursor.getString(cursor.getColumnIndexOrThrow("preview")));
        chat.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
        chat.setMessageCount(cursor.getInt(cursor.getColumnIndexOrThrow("message_count")));
        return chat;
    }
}