package com.techstar.nexchat.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.techstar.nexchat.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatRepo {
    private static final String DB_NAME = "chat.db";
    private static final int    VERSION = 1;
    private static ChatRepo INSTANCE;

    private final SQLiteDatabase db;

    private ChatRepo(Context ctx) {
        db = new Helper(ctx.getApplicationContext()).getWritableDatabase();
    }
    public static ChatRepo get(Context ctx) {
        if (INSTANCE == null) INSTANCE = new ChatRepo(ctx);
        return INSTANCE;
    }

    /* 增 */
    public void addUser(String text) {
        db.execSQL("INSERT INTO chat(role,content,ts) VALUES(?,?,?)",
                new Object[]{"user", text, System.currentTimeMillis()});
        // TODO 真正调 AI 后再插入 assistant 行
    }

    /* 查 */
    public List<Message> getAll() {
        List<Message> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT role,content FROM chat ORDER BY ts", null);
        while (c.moveToNext()) {
            list.add(new Message(c.getString(0), c.getString(1)));
        }
        c.close();
        return list;
    }

    /* 清 */
    public void clear() {
        db.delete("chat", null, null);
    }

    /* 历史标题（偷懒用 content 前 20 字） */
    public List<String> getSessionTitles() {
        List<String> titles = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT DISTINCT substr(content,1,20) FROM chat WHERE role='user'", null);
        while (c.moveToNext()) titles.add(c.getString(0));
        c.close();
        return titles;
    }

    /* SQLiteOpenHelper */
    private static class Helper extends SQLiteOpenHelper {
        Helper(Context ctx) { super(ctx, DB_NAME, null, VERSION); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE chat(" +
                    "role TEXT, content TEXT, ts LONG)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
    }
}
