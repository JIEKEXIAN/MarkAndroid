package com.intlime.mark.view

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import com.intlime.mark.application.SettingManager
import com.intlime.mark.tools.ZhugeTool
import com.intlime.mark.tools.db.DBHelper
import com.intlime.mark.tools.db.MovieDbManager
import org.jetbrains.anko.matchParent

/**
 * Created by root on 16-3-12.
 */
class MovieTodoView(context: Context, val checkBox: CheckBox) : FrameLayout(context), CompoundButton.OnCheckedChangeListener {
    companion object {
        private var cursor: Cursor? = null

        fun getMyCursor(): Cursor {
            if (cursor == null || cursor!!.isClosed) {
                cursor = newCursor()
            }
            return cursor!!
        }

        fun getCount(): Int {
            return getMyCursor().count
        }

        fun newCursor(type: Int = 1): Cursor {
            when (type) {
                1 -> {
                    if (cursor != null && !cursor!!.isClosed) cursor!!.close()
                    cursor = DBHelper.getInstance().readableDatabase.rawQuery("select * from " + MovieDbManager.TABEL +
                            " where is_done = 0 order by update_time desc", null)
                }
                2 -> {
                    if (cursor != null && !cursor!!.isClosed) cursor!!.close()
                    cursor = DBHelper.getInstance().readableDatabase.rawQuery("select * from " + MovieDbManager.TABEL +
                            " where is_done = 0 order by exhibit_pubdate desc, update_time desc", null)
                }
                3 -> {
                    if (cursor != null && !cursor!!.isClosed) cursor!!.close()
                    cursor = DBHelper.getInstance().readableDatabase.rawQuery("select * from " + MovieDbManager.TABEL +
                            " where is_done = 0 order by dbrating desc, update_time desc", null)
                }
                4 -> {
                    if (cursor != null && !cursor!!.isClosed) cursor!!.close()
                    cursor = DBHelper.getInstance().readableDatabase.rawQuery("select * from " + MovieDbManager.TABEL +
                            " where is_done = 0 order by pinyin, update_time desc", null)
                }
            }
            return cursor!!
        }
    }

    var sortType = 1
    private var isListMovieViewInited = false

    val gridTodoView by lazy {
        val v = GridTodoView(context)
        v.onSortChanged(sortType, getMyCursor(), true)
        addView(v, LayoutParams(matchParent, matchParent))
        return@lazy v
    }
    val listMovieView by lazy {
        isListMovieViewInited = true
        val v = ListMovieView(context, 1)
        v.onSortChanged(sortType, getMyCursor())
        addView(v, LayoutParams(matchParent, matchParent))

        gridTodoView.recyclerView.addDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                v.myAdapter.onDataChanged()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                v.myAdapter.onDataChanged()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                v.myAdapter.onDataChanged()
            }
        })
        return@lazy v
    }

    init {
        val mode = SettingManager.getInstance().todoViewMode
        if (mode == 0) {
            gridTodoView.visibility = VISIBLE
            listMovieView.visibility = GONE
            checkBox.isChecked = false
        } else {
            gridTodoView.visibility = GONE
            listMovieView.visibility = VISIBLE
            checkBox.isChecked = true
        }
        checkBox.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            gridTodoView.visibility = GONE
            listMovieView.visibility = VISIBLE
            SettingManager.getInstance().todoViewMode = 1
            ZhugeTool.track("列表模式")
        } else {
            gridTodoView.visibility = VISIBLE
            listMovieView.visibility = GONE
            SettingManager.getInstance().todoViewMode = 0
            ZhugeTool.track("大图模式")
        }
    }

    fun onSortChanged(type: Int, scrollToTop:Boolean = true) {
        sortType = type
        newCursor(type)
        gridTodoView.onSortChanged(type, getMyCursor(), scrollToTop)
        if (isListMovieViewInited) {
            listMovieView.onSortChanged(type, getMyCursor(), scrollToTop)
        }
    }
}