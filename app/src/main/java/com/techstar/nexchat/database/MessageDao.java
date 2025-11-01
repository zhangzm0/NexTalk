package com.techstar.nexchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.techstar.nexchat.model.Message;
import com.techstar.nexchat.util.FileLogger;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    private static final String TAG = "MessageDao";
    private DatabaseHelper dbHelper;
    private FileLogger logger;
    
    public MessageDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.logger = FileLogger.getInstance(context);
    }
    
    public long insertMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        
        try {
            ContentValues values = new ContentValues();
            values.put("chat_id", message.getChatId());
            values.put("role", message.getRole());
            values.put("content", message.getContent());
            values.put("timestamp", message.getTimestamp());
            values.put("tokens", message.getTokens());
            values.put("model", message.getModel());
            
            // 新增字段
            values.put("reasoning_content", message.getReasoningContent());
            values.put("tool_calls", message.getToolCalls());
            values.put("parent_id", message.getParentId());
            values.put("branch_id", message.getBranchId());
            values.put("has_reasoning", message.hasReasoning() ? 1 : 0);
            values.put("status", message.getStatus());
            
            id = db.insert(DatabaseHelper.TABLE_MESSAGES, null, values);
            logger.i(TAG, "Inserted message for chat ID: " + message.getChatId() + 
                    ", role: " + message.getRole() + ", ID: " + id);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to insert message: " + e.getMessage());
        } finally {
            db.close();
        }
        
        return id;
    }
    
    public List<Message> getMessagesByChatId(int chatId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_MESSAGES, 
                    null, "chat_id = ?", new String[]{String.valueOf(chatId)}, 
                    null, null, "timestamp ASC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = cursorToMessage(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            
            logger.i(TAG, "Retrieved " + messages.size() + " messages for chat ID: " + chatId);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to retrieve messages for chat ID: " + chatId + ", error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return messages;
    }
    
    // 获取指定分支的消息
    public List<Message> getMessagesByBranch(int chatId, String branchId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_MESSAGES, 
                    null, "chat_id = ? AND branch_id = ?", 
                    new String[]{String.valueOf(chatId), branchId}, 
                    null, null, "timestamp ASC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = cursorToMessage(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            
            logger.i(TAG, "Retrieved " + messages.size() + " messages for branch: " + branchId);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to retrieve messages for branch: " + branchId + ", error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return messages;
    }
    
    // 获取指定父节点的子消息
    public List<Message> getChildMessages(int parentId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_MESSAGES, 
                    null, "parent_id = ?", new String[]{String.valueOf(parentId)}, 
                    null, null, "timestamp ASC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = cursorToMessage(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            
            logger.i(TAG, "Retrieved " + messages.size() + " child messages for parent: " + parentId);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to retrieve child messages for parent: " + parentId + ", error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return messages;
    }
    
    public Message getLastMessageByChatId(int chatId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        Message message = null;
        
        try {
            cursor = db.query(DatabaseHelper.TABLE_MESSAGES, 
                    null, "chat_id = ?", new String[]{String.valueOf(chatId)}, 
                    null, null, "timestamp DESC", "1");
            
            if (cursor != null && cursor.moveToFirst()) {
                message = cursorToMessage(cursor);
            }
            
        } catch (Exception e) {
logger.e(TAG, "Failed to get last message for chat ID: " + chatId + ", error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return message;
    }
    
    public boolean updateMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            ContentValues values = new ContentValues();
            values.put("content", message.getContent());
            values.put("tokens", message.getTokens());
            values.put("model", message.getModel());
            
            // 新增字段
            values.put("reasoning_content", message.getReasoningContent());
            values.put("tool_calls", message.getToolCalls());
            values.put("parent_id", message.getParentId());
            values.put("branch_id", message.getBranchId());
            values.put("has_reasoning", message.hasReasoning() ? 1 : 0);
            values.put("status", message.getStatus());
            
            int rowsAffected = db.update(DatabaseHelper.TABLE_MESSAGES, 
                    values, "id = ?", new String[]{String.valueOf(message.getId())});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Updated message ID: " + message.getId() + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to update message: " + e.getMessage());
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public boolean deleteMessage(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            int rowsAffected = db.delete(DatabaseHelper.TABLE_MESSAGES, 
                    "id = ?", new String[]{String.valueOf(id)});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Deleted message with ID: " + id + ", rows affected: " + rowsAffected);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to delete message with ID: " + id + ", error: " + e.getMessage());
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public boolean deleteMessagesByChatId(int chatId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;
        
        try {
            int rowsAffected = db.delete(DatabaseHelper.TABLE_MESSAGES, 
                    "chat_id = ?", new String[]{String.valueOf(chatId)});
            
            success = rowsAffected > 0;
            logger.i(TAG, "Deleted " + rowsAffected + " messages for chat ID: " + chatId);
            
        } catch (Exception e) {
logger.e(TAG, "Failed to delete messages for chat ID: " + chatId + ", error: " + e.getMessage());
        } finally {
            db.close();
        }
        
        return success;
    }
    
    public int getMessageCountByChatId(int chatId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;
        
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_MESSAGES + 
                    " WHERE chat_id = ?", new String[]{String.valueOf(chatId)});
            
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            
        } catch (Exception e) {
logger.e(TAG, "Failed to get message count for chat ID: " + chatId + ", error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        
        return count;
    }
    
    private Message cursorToMessage(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        message.setChatId(cursor.getInt(cursor.getColumnIndexOrThrow("chat_id")));
        message.setRole(cursor.getInt(cursor.getColumnIndexOrThrow("role")));
        message.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        message.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
        message.setTokens(cursor.getInt(cursor.getColumnIndexOrThrow("tokens")));
        message.setModel(cursor.getString(cursor.getColumnIndexOrThrow("model")));
        
        // 新增字段
        message.setReasoningContent(cursor.getString(cursor.getColumnIndexOrThrow("reasoning_content")));
        message.setToolCalls(cursor.getString(cursor.getColumnIndexOrThrow("tool_calls")));
        message.setParentId(cursor.getInt(cursor.getColumnIndexOrThrow("parent_id")));
        message.setBranchId(cursor.getString(cursor.getColumnIndexOrThrow("branch_id")));
        message.setHasReasoning(cursor.getInt(cursor.getColumnIndexOrThrow("has_reasoning")) == 1);
        message.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
        
        return message;
    }
}