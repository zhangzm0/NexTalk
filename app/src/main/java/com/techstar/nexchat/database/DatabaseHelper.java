package com.techstar.nexchat.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.techstar.nexchat.util.FileLogger;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "nexchat.db";
    private static final int DATABASE_VERSION = 1;
    
    private FileLogger logger;
    
    // 表名
    public static final String TABLE_API_PROVIDERS = "api_providers";
    public static final String TABLE_CHAT_HISTORY = "chat_history";
    public static final String TABLE_MESSAGES = "messages";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        logger = FileLogger.getInstance(context);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        logger.i(TAG, "Creating database tables");
        
        // API供应商表
        String createApiProvidersTable = "CREATE TABLE " + TABLE_API_PROVIDERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "api_url TEXT NOT NULL," +
                "api_key TEXT NOT NULL," +
                "models TEXT," +
                "balance TEXT," +
                "created_at INTEGER" +
                ");";
        
        // 聊天历史表
        String createChatHistoryTable = "CREATE TABLE " + TABLE_CHAT_HISTORY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "preview TEXT," +
                "timestamp INTEGER," +
                "message_count INTEGER DEFAULT 0" +
                ");";
        
        // 消息表
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chat_id INTEGER," +
                "role INTEGER," +
                "content TEXT," +
                "timestamp INTEGER," +
                "tokens INTEGER DEFAULT 0," +
                "model TEXT," +
                "FOREIGN KEY(chat_id) REFERENCES " + TABLE_CHAT_HISTORY + "(id)" +
                ");";
        
        db.execSQL(createApiProvidersTable);
        db.execSQL(createChatHistoryTable);
        db.execSQL(createMessagesTable);
        
        logger.i(TAG, "Database tables created successfully");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logger.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_API_PROVIDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        
        onCreate(db);
    }
}