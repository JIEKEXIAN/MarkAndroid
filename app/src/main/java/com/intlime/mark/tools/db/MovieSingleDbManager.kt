package com.intlime.mark.tools.db

import android.content.ContentValues
import android.database.Cursor
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.bean.SingleBean
import java.util.*

/**
 * Created by wtuadn on 16/04/26.
 */
object MovieSingleDbManager {
    val table_single = "movie_single"
    val table_access = "movie_single_access"

    fun getSingleCursor(singleId: Int): Cursor? {
        val db = DBHelper.getInstance().readableDatabase
        return db.rawQuery("select t1.* from ${MovieDbManager.TABEL} t1 inner join $table_access t2 " +
                "on t2.single_id = $singleId and t1.id = t2.movie_id order by t2.update_time desc", null)
    }

    fun getItemByCursor(cursor: Cursor): SingleBean {
        val bean = SingleBean()
        bean.id = cursor.getInt(0)
        bean.name = cursor.getString(1)
        return bean
    }

    fun isInSingle(singleId: Int, movieId: Int): Boolean {
        if (singleId < 0 || movieId <= 0) return false
        val db = DBHelper.getInstance().readableDatabase
        val cursor = db.rawQuery("select * from $table_access where single_id=$singleId and movie_id=$movieId", null)
        return cursor.count > 0
    }

    fun getSingleIdList(movieId: Int): List<Int> {
        val list = ArrayList<Int>()
        if (movieId > 0) {
            val db = DBHelper.getInstance().readableDatabase
            val cursor = db.rawQuery("select DISTINCT single_id from $table_access where movie_id=$movieId", null)
            while (cursor.moveToNext()) {
                list.add(cursor.getInt(0))
            }
        }
        return list
    }

    fun insertSingles(bean: SingleBean) {
        if (bean.id < 0) return
        val db = DBHelper.getInstance().writableDatabase
        try {
            val values = getSingleContentValues(bean)
            db.replace(table_single, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insertAccess(bean: SingleAccessBean) {
        if (bean.single_id < 0 || bean.movie_id <= 0) return
        val db = DBHelper.getInstance().writableDatabase
        try {
            val values = getAccessContentValues(bean)
            db.replace(table_access, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insertSingles(list: List<SingleBean>) {
        val db = DBHelper.getInstance().writableDatabase
        try {
            db.beginTransaction()
            for (bean in list) {
                if (bean.id < 0) continue
                val values = getSingleContentValues(bean)
                db.replace(table_single, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAccess(list: List<SingleAccessBean>) {
        val db = DBHelper.getInstance().writableDatabase
        try {
            db.beginTransaction()
            for (bean in list) {
                if (bean.single_id < 0 || bean.movie_id <= 0) continue
                val values = getAccessContentValues(bean)
                db.replace(table_access, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteSingle(bean: SingleBean): Int {
        val db = DBHelper.getInstance().writableDatabase
        var id = -1
        try {
            id = db.delete(table_single, "id=?", arrayOf(Integer.toString(bean.id)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    fun deleteAccess(bean: SingleAccessBean): Int {
        val db = DBHelper.getInstance().writableDatabase
        var id = -1
        try {
            id = db.delete(table_access, "single_id=? and movie_id=?", arrayOf(bean.single_id.toString(), bean.movie_id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    fun deleteAccess(bean: MovieBean): Int {
        val db = DBHelper.getInstance().writableDatabase
        var id = -1
        try {
            id = db.delete(table_access, "movie_id=?", arrayOf(bean.id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    fun deleteAccess(list: List<*>) {
        if (list.isEmpty()) return
        val db = DBHelper.getInstance().writableDatabase
        try {
            db.beginTransaction()
            for (b in list) {
                if (b is SingleAccessBean) {
                    db.delete(table_access, "single_id=? and movie_id=?", arrayOf(b.single_id.toString(), b.movie_id.toString()))
                } else if (b is MovieBean) {
                    db.delete(table_access, "movie_id=?", arrayOf(b.id.toString()))
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    private fun getSingleContentValues(bean: SingleBean): ContentValues {
        val values = ContentValues(3)
        values.put("id", bean.id)
        values.put("name", bean.name)
        values.put("update_time", 0)
        return values
    }

    private fun getAccessContentValues(bean: SingleAccessBean): ContentValues {
        val values = ContentValues(3)
        values.put("single_id", bean.single_id)
        values.put("movie_id", bean.movie_id)
        values.put("update_time", bean.update_time)
        return values
    }

    fun clearSingle() {
        val db = DBHelper.getInstance().writableDatabase
        try {
            db.delete(table_single, "id!=?", arrayOf("0"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAccess() {
        val db = DBHelper.getInstance().writableDatabase
        try {
            db.delete(table_access, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}