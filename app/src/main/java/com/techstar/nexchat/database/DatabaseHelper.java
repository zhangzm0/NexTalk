package com.techstar.nexchat.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.techstar.nexchat.util.FileLogger;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "nexchat.db";
    private static final int DATABASE_VERSION = 2; // 版本升级
    
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
        
        // 消息表 - 扩展支持树状对话和思考过程
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chat_id INTEGER," +
                "role INTEGER," +
                "content TEXT," +
                "timestamp INTEGER," +
                "tokens INTEGER DEFAULT 0," +
                "model TEXT," +
                "reasoning_content TEXT," +        // 新增：思考过程
                "tool_calls TEXT," +               // 新增：工具调用
                "parent_id INTEGER DEFAULT -1," +  // 新增：父节点ID
                "branch_id TEXT DEFAULT 'main'," + // 新增：分支标识
                "has_reasoning INTEGER DEFAULT 0," + // 新增：是否有思考过程
                "status INTEGER DEFAULT 2," +      // 新增：消息状态
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
        
        if (oldVersion < 2) {
            // 升级到版本2：添加新字段
            logger.i(TAG, "Upgrading to version 2: adding new message fields");
            
            // 添加新字段到消息表
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN reasoning_content TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN tool_calls TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN parent_id INTEGER DEFAULT -1");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN branch_id TEXT DEFAULT 'main'");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN has_reasoning INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN status INTEGER DEFAULT 2");
            
            logger.i(TAG, "Database upgraded to version 2 successfully");
        }
        
        // 未来版本升级逻辑可以在这里添加
        if (oldVersion < 3) {
            // 版本3的升级逻辑
        }
    }
}