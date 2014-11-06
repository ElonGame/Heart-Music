package com.musicplayer.heartmusic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dongju on 14. 10. 9..
 */
public class dbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "heartmusic.db";
    private static final int DATABASE_VERSION = 1;

    public dbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE BPM (id INTEGER, title TEXT, artist TEXT, path TEXT, bpm INTEGER);");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXITS BPM");
        onCreate(db);
    }
}