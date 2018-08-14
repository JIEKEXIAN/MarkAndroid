package com.intlime.mark.tools.db;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.tools.HanziToPinyin;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by root on 15-11-3.
 */
public class MovieDbManager implements DBInterface<MovieBean> {
    public static final String TABEL = "movie";
    private static MovieDbManager INSTANCE = null;

    public static final int ID_P = 0;
    public static final int DBNUM_P = 1;
    public static final int DONE_P = 2;
    public static final int NAME_P = 3;
    public static final int IMAGE_P = 4;
    public static final int M_RATING_P = 5;
    public static final int WATCHTIME_P = 6;
    public static final int NOTE_P = 7;
    public static final int PUBDATE_P = 8;
    public static final int DURATION_P = 9;
    public static final int MOVIE_TYPE_P = 10;
    public static final int DB_RATING_P = 11;
    public static final int PUBDATE_TIMESTAMP_P = 12;
    public static final int UPDATE_TIME_P = 13;
    public static final int PINYIN_P = 14;
    public static final int GROUP_UPDATE_TIME_P = 15;
    public static final int GROUP_WATCH_TIME_P = 16;
    public static final int GROUP_PUBDATE_P = 17;

    public static MovieDbManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MovieDbManager();
        }
        return INSTANCE;
    }

    @Override
    public MovieBean get(int id) {
        SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABEL + " where id=" + id, null);
        if (cursor.moveToNext()) {
            return getItemByCursor(cursor);
        }
        return null;
    }

    public MovieBean get(String dbNum) {
        SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABEL + " where db_num=" + dbNum, null);
        if (cursor.moveToNext()) {
            return getItemByCursor(cursor);
        }
        return null;
    }

    public MovieBean getItemByCursor(Cursor cursor) {
        MovieBean bean = new MovieBean();
        bean.setId(cursor.getInt(ID_P));
        bean.setDb_num(cursor.getString(DBNUM_P));
        bean.setDone(cursor.getInt(DONE_P));
        bean.setName(cursor.getString(NAME_P));
        bean.setImage(cursor.getString(IMAGE_P));
        bean.setMark_rating(cursor.getFloat(M_RATING_P));
        bean.setWatchTime(cursor.getLong(WATCHTIME_P));
        bean.setNote(cursor.getString(NOTE_P));
        bean.setPubdate(cursor.getString(PUBDATE_P));
        bean.setDuration(cursor.getString(DURATION_P));
        bean.setMovieType(cursor.getString(MOVIE_TYPE_P));
        bean.setDb_rating(cursor.getFloat(DB_RATING_P));
        bean.setPubdateTimestamp(cursor.getLong(PUBDATE_TIMESTAMP_P));
        bean.setUpdate_time(cursor.getLong(UPDATE_TIME_P));
        bean.setPinyin(cursor.getString(PINYIN_P));
        bean.setGroupUpdateTime(cursor.getString(GROUP_UPDATE_TIME_P));
        bean.setGroupWatchTime(cursor.getString(GROUP_WATCH_TIME_P));
        bean.setGroupPubdate(cursor.getString(GROUP_PUBDATE_P));
        return bean;
    }

    @Override
    public int delete(MovieBean bean) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int id = -1;
        try {
            id = db.delete(TABEL, "id=?", new String[]{Integer.toString(bean.getId())});
        } catch (Exception e) {
            e.printStackTrace();
        }
        MovieSingleDbManager.INSTANCE.deleteAccess(bean);
        AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.RELOAD_SINGLE_ACTION));
        return id;
    }

    public void delete(List<MovieBean> list) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        try {
            db.beginTransaction();
            for (int i = 0; i < list.size(); i++) {
                MovieBean bean = list.get(i);
                db.delete(TABEL, "id=?", new String[]{Integer.toString(bean.getId())});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        MovieSingleDbManager.INSTANCE.deleteAccess(list);
        AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.RELOAD_SINGLE_ACTION));
    }

    @Override
    public int insert(MovieBean bean) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int id = -1;
        try {
            ContentValues values = getContentValues(bean);
            id = (int) db.replace(TABEL, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public void insert(List<MovieBean> list) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        try {
            db.beginTransaction();
            for (int i = 0; i < list.size(); i++) {
                MovieBean bean = list.get(i);
                ContentValues values = getContentValues(bean);
                db.replace(TABEL, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int update(MovieBean bean) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int id = -1;
        try {
            ContentValues values = getContentValues(bean);
            id = db.update(TABEL, values, "id=?", new String[]{Integer.toString(bean.getId())});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public void update(List<MovieBean> list) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        try {
            db.beginTransaction();
            for (int i = 0; i < list.size(); i++) {
                MovieBean bean = list.get(i);
                ContentValues values = getContentValues(bean);
                db.update(TABEL, values, "id=?", new String[]{Integer.toString(bean.getId())});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    private ContentValues getContentValues(MovieBean bean) {
        ContentValues values = new ContentValues(20);
        values.put("id", bean.getId());
        values.put("db_num", bean.getDb_num());
        values.put("is_done", bean.getDone());
        values.put("name", bean.getName());
        values.put("img_url", bean.getImage());
        values.put("mark_rating", bean.getMark_rating());
        values.put("watch_time", bean.getWatchTime());
        values.put("note", bean.getNote());
        values.put("pubdate", bean.getPubdate());
        values.put("duration", bean.getDuration());
        values.put("genres", bean.getMovieType());
        values.put("dbrating", bean.getDb_rating());
        values.put("exhibit_pubdate", bean.getPubdateTimestamp());
        values.put("update_time", bean.getUpdate_time());
        String fStr = HanziToPinyin.getPinYin(bean.getName());
        if (TextUtils.isEmpty(fStr) || !abcChars.contains(fStr.charAt(0))) {
            bean.setPinyin("~");
        } else {
            bean.setPinyin(fStr);
        }
        values.put("pinyin", bean.getPinyin());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月", Locale.CHINA);
        values.put("group_update_time", sdf.format(new Date(bean.getUpdate_time() * 1000L)));
        if (bean.getWatchTime() == 0L) {
            values.put("group_watch_time", "暂无日期");
        } else {
            values.put("group_watch_time", sdf.format(new Date(bean.getWatchTime() * 1000L)));
        }
        if (bean.getPubdateTimestamp() < -2398320000L) {
            values.put("group_exhibit_time", "暂无日期");
        } else {
            values.put("group_exhibit_time", sdf.format(new Date(bean.getPubdateTimestamp() * 1000L)));
        }
        return values;
    }

    @Override
    public void clear() {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        try {
            db.delete(TABEL, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<Character> abcChars = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z');
}
