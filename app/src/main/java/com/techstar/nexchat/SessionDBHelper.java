package com.techstar.nexchat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.techstar.nexchat.SessionContract;

public class SessionDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "nexchat.db";
    private static final int    VERSION = 1;

    public SessionDBHelper(Context c) { super(c, DB_NAME, null, VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SessionContract.TABLE + " ("
				   + SessionContract._ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				   + SessionContract.TITLE    + " TEXT, "
				   + SessionContract.LAST_MSG + " TEXT, "
				   + SessionContract.UPDATED  + " INTEGER)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
}

