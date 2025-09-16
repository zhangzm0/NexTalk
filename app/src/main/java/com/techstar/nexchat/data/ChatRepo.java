package com.techstar.nexchat.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;   // ← 新增

import com.techstar.nexchat.model.Message;
import com.techstar.nexchat.model.Session;

import java.util.ArrayList;
import java.util.List;

public class ChatRepo {
    private static final String DB_NAME = "chat.db";
    private static final int    VERSION = 2;   // 升版，加 session 表
    private static ChatRepo INSTANCE;

    private final SQLiteDatabase db;
    private final SharedPreferences sp;        // ← 新增

    private ChatRepo(Context ctx) {
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        db = new Helper(ctx.getApplicationContext()).getWritableDatabase();
    }
    public static ChatRepo get(Context ctx) {
        if (INSTANCE == null) INSTANCE = new ChatRepo(ctx);
        return INSTANCE;
    }

    /* ======== 会话相关 ======== */
    public long createSession(String title) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("updated", System.currentTimeMillis());
        long id = db.insert("session", null, cv);
        sp.edit().putLong("current_session", id).apply();
        return id;
    }
    public void switchSession(long sessionId) {
        sp.edit().putLong("current_session", sessionId).apply();
    }
    public long currentSession() {
        return sp.getLong("current_session", -1);
    }
    public List<Session> getSessions() {
        List<Session> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT _id,title FROM session ORDER BY updated DESC", null);
        while (c.moveToNext()) list.add(new Session(c.getLong(0), c.getString(1)));
        c.close();
        return list;
    }
	/* 用于 HomeFragment 的会话标题列表 */
	public List<String> getSessionTitles() {
		List<String> list = new ArrayList<>();
		Cursor c = db.rawQuery(
			"SELECT title FROM session ORDER BY updated DESC", null);
		while (c.moveToNext()) {
			list.add(c.getString(0));
		}
		c.close();
		return list;
	}
	
	
    private void touchSession() {
        long id = currentSession();
        if (id != -1) {
            ContentValues cv = new ContentValues();
            cv.put("updated", System.currentTimeMillis());
            db.update("session", cv, "_id=?", new String[]{String.valueOf(id)});
        }
    }

    /* ======== 消息相关 ======== */
    public void addUser(String text) {
        ensureSession();
        db.execSQL("INSERT INTO chat(session_id,role,content,ts) VALUES(?,?,?,?)",
                new Object[]{currentSession(), "user", text, System.currentTimeMillis()});
        touchSession();
    }
    public void addAssistant(String text) {
        db.execSQL("INSERT INTO chat(session_id,role,content,ts) VALUES(?,?,?,?)",
                new Object[]{currentSession(), "assistant", text, System.currentTimeMillis()});
        touchSession();
    }
    public List<Message> getAll() {
        List<Message> list = new ArrayList<>();
        long sid = currentSession();
        if (sid == -1) return list;
        Cursor c = db.rawQuery("SELECT role,content FROM chat WHERE session_id=? ORDER BY ts",
                new String[]{String.valueOf(sid)});
        while (c.moveToNext()) list.add(new Message(c.getString(0), c.getString(1)));
        c.close();
        return list;
    }
    public void clear() {
        long sid = currentSession();
        if (sid != -1) {
            db.delete("chat", "session_id=?", new String[]{String.valueOf(sid)});
            touchSession();
        }
    }

    /* 如果没有会话就新建一个 */
    private void ensureSession() {
        if (currentSession() == -1) createSession("新对话");
    }

    /* SQLiteOpenHelper */
    private static class Helper extends SQLiteOpenHelper {
        Helper(Context ctx) { super(ctx, DB_NAME, null, VERSION); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE session(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,updated LONG)");
            db.execSQL("CREATE TABLE chat(session_id LONG,role TEXT,content TEXT,ts LONG)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            // 简单暴力：删表重建
            db.execSQL("DROP TABLE IF EXISTS session");
            db.execSQL("DROP TABLE IF EXISTS chat");
            onCreate(db);
        }
    }
}

