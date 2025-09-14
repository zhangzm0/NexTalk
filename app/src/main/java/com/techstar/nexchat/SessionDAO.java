package com.techstar.nexchat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.techstar.nexchat.SessionContract;

public class SessionDAO {
    // 新增会话，返回 id
    public static long insert(Context c, String title, String lastMsg) {
        ContentValues v = new ContentValues();
        v.put(SessionContract.TITLE,    title);
        v.put(SessionContract.LAST_MSG, lastMsg);
        v.put(SessionContract.UPDATED,  System.currentTimeMillis());
        return new SessionDBHelper(c).getWritableDatabase()
			.insert(SessionContract.TABLE, null, v);
    }

    // 查询全部（按时间倒序）
    public static Cursor queryAll(Context c) {
        return new SessionDBHelper(c).getReadableDatabase()
			.query(SessionContract.TABLE, null, null, null,
				   null, null, SessionContract.UPDATED + " DESC");
    }

    // 删除
    public static void delete(Context c, long id) {
        new SessionDBHelper(c).getWritableDatabase()
			.delete(SessionContract.TABLE, SessionContract._ID + "=?",
					new String[]{String.valueOf(id)});
    }
}

