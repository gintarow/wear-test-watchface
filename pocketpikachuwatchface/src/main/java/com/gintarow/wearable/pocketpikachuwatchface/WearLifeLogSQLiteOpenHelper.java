package com.gintarow.wearable.pocketpikachuwatchface;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 設定情報を保存するDBの作成
 * @author ISPC R&D
 */
public class WearLifeLogSQLiteOpenHelper extends SQLiteOpenHelper {

    static final String DB = "wear_life_log.db";
	public static final String TABLE_NAME = "stepCounter";
	static final int DB_VERSION = 1;
	/** CREATE TABLEのクエリ */
    static final String CREATE_TABLE = "create table "+TABLE_NAME
			+" ( date text not null, step_count integer default 0 );";
	/** DROP TABLEのクエリ */
    static final String DROP_TABLE = "drop table "+TABLE_NAME+";";

    public WearLifeLogSQLiteOpenHelper(Context context){
        super(context, DB, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SQLite", "create DB");
        db.execSQL(CREATE_TABLE);
//        db.execSQL("insert into startAppList(name,pkg_name,conf) values ('アプリ１', 'com.pioneer...', 1);");
//        db.execSQL("insert into startAppList(name,pkg_name,conf) values ('アプリ２', 'com.pioneer...', 10);");
//        db.execSQL("insert into startAppList(name,pkg_name,conf) values ('アプリ３', 'com.pioneer...', 11);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
}
