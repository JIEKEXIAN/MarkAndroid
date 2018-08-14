package com.intlime.mark.tools.db;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.intlime.mark.activitys.MainActivity;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;

/**
 * 数据库管理
 *
 * @author Administrator
 */
public class DBHelper extends SQLiteOpenHelper {
    public final static String DB_NAME = "mark_db";
    public final static int version = 9;
    private static DBHelper INSTANCE = null;

    public static synchronized DBHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DBHelper(AppEngine.getContext());
        }
        return INSTANCE;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, version);
    }

    private static final String movie_sql = //电影表
            "create table if not exists movie(" +
                    "id integer primary key," +
                    "db_num varchar," +
                    "is_done integer," +
                    "name varchar," +
                    "img_url varchar," +
                    "mark_rating float," +
                    "watch_time integer," +
                    "note varchar," +
                    "pubdate varchar," +
                    "duration varchar," +
                    "genres varchar," +
                    "dbrating float," +
                    "exhibit_pubdate integer," +
                    "update_time integer," +
                    "pinyin varchar," +
                    "group_update_time varchar," +
                    "group_watch_time varchar," +
                    "group_exhibit_time varchar)";
    private static final String movie_single_sql = //影单表
            "create table if not exists movie_single(" +
                    "id integer primary key," +
                    "name varchar," +
                    "update_time integer)";
    private static final String movie_single_access_sql = //影单关联表
            "create table if not exists movie_single_access(" +
                    "single_id integer default 0," +
                    "movie_id integer," +
                    "update_time integer," +
                    "primary key (single_id, movie_id))";

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            db.execSQL(movie_sql);
            db.execSQL(movie_single_sql);
            db.execSQL(movie_single_access_sql);

            db.execSQL("INSERT INTO movie_single VALUES (0, '我喜欢的电影', 2524579200)");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = oldVersion + 1; i <= newVersion; i++) {
            switch (i) {
                case 7:
                    try {
                        String sql1 = "drop table if exists movie";
                        db.execSQL(sql1);
                        onCreate(db);

                        final Activity activity = WWindowManager.getInstance().getCurrentActivity();
                        if (Session.uid <= 0 || !(activity instanceof MainActivity)) break;
                        ThreadManager.getInstance().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SettingManager.getInstance().setNeedUpdateData(true);
                                if (activity != null) {
                                    (((MainActivity) activity)).updateData(false);
                                }
                            }
                        }, 100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                case 8:
                    try {
                        db.beginTransaction();

                        db.execSQL(movie_single_sql);
                        db.execSQL(movie_single_access_sql);
                        db.execSQL("INSERT INTO movie_single VALUES (0, '我喜欢的电影', 2524579200)");
                        db.execSQL("INSERT OR IGNORE INTO movie_single_access (movie_id,update_time) select id,update_time_favorite from movie where is_favorite=1");

                        db.execSQL("alter table movie rename to temp");
                        db.execSQL(movie_sql);
                        db.execSQL("insert OR IGNORE into movie select id,db_num,is_done,name,img_url,mark_rating," +
                                "watch_time,note,pubdate,duration,genres,dbrating,exhibit_pubdate,update_time," +
                                "pinyin,group_update_time,group_watch_time,group_exhibit_time from temp");
                        db.execSQL("drop table temp");

                        db.setTransactionSuccessful();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 9:
                    try {
                        db.beginTransaction();
                        db.execSQL("delete from movie_single_access where single_id < 0 or movie_id <= 0");
                        db.setTransactionSuccessful();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.endTransaction();
                    }
                    break;
            }
        }
    }
}
