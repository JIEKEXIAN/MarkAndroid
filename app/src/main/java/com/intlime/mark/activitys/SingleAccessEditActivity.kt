package com.intlime.mark.activitys

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.bean.SingleBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.CryptTool
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.db.DBHelper
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.view.MovieDoneView
import com.intlime.mark.view.MovieTodoView
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.RecyclerCursorAdapter
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.yRecyclerView
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableLinearLayout
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import java.util.*

/**
 * Created by wtuadn on 16/04/27.
 */
class SingleAccessEditActivity : BaseActivity() {
    private val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
    private val error = EmptyDrawable(25f, 25f)
    private val cache = ArrayMap<Int, Cursor>()
    private var bean: MovieBean? = null
    private var type = 0 //0为本地有的,1为本地没有的
    private var done = 0
    private lateinit var myAdapter: MyAdapter
    private val singlesList = ArrayList<Int>() // 该电影的所有影单id
    private val checkedList = ArrayList<Int>() // 选中的影单id
    private val toAddList = ArrayList<Int>() //要添加到的影单id
    private val toDeleteList = ArrayList<Int>() //要从中删除的影单id

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (RELOAD_SINGLE_ACTION == action) {
                    myAdapter.newCursor()
                    myAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bean = intent.getParcelableExtra(BEAN)
        if (bean == null) {
            finish()
            return
        }
        if (bean!!.done < 0) bean!!.done = 0
        type = intent.getIntExtra("type", 0)
        done = bean!!.done
        singlesList.addAll(MovieSingleDbManager.getSingleIdList(bean!!.id))
        checkedList.addAll(singlesList)
        applyUI()

        val filter = IntentFilter()
        filter.addAction(RELOAD_SINGLE_ACTION)
        registerReceiver(broadcastReceiver, filter)
    }

    override fun finish() {
        super.finish()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch(e: Exception) {
        }
    }

    private fun applyUI() {
        rootView = verticalLayout {
            backgroundColor = Color.parseColor("#ebebeb")
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "加入我的影单"
                menu.add("完成").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<*, *>) {
                            val time = result["time"] as Long
                            if (type == 1) {
                                val temp = result["bean"] as MovieBean
                                bean!!.id = temp.id
                                bean!!.done = done
                                bean!!.db_rating = temp.db_rating
                                bean!!.pubdate = temp.pubdate
                                bean!!.duration = temp.duration
                                bean!!.movieType = temp.movieType
                                bean!!.update_time = temp.update_time
                                bean!!.pubdateTimestamp = temp.pubdateTimestamp
                                if (bean!!.done == 1) {
                                    bean!!.watchTime = bean!!.update_time
                                } else {
                                    bean!!.watchTime = 0
                                }
                                MovieDbManager.getInstance().insert(bean)
                                val intent = Intent(RELOAD_ALL_ACTION)
                                intent.putExtra(BEAN, bean!!)
                                intent.putExtra("type", 0)
                                sendBroadcast(intent)
                            } else if (done != bean!!.done) {
                                bean!!.done = done
                                bean!!.update_time = time
                                if (bean!!.done == 1) {
                                    bean!!.watchTime = bean!!.update_time
                                } else {
                                    bean!!.watchTime = 0
                                }
                                MovieDbManager.getInstance().update(bean)
                                val intent = Intent(RELOAD_ALL_ACTION)
                                intent.putExtra(BEAN, bean!!)
                                sendBroadcast(intent)
                            }
                            val list = ArrayList<SingleAccessBean>()
                            for (id in toDeleteList) {
                                list.add(SingleAccessBean(id, bean!!.id, time))
                            }
                            MovieSingleDbManager.deleteAccess(list)
                            list.clear()
                            for (id in toAddList) {
                                list.add(SingleAccessBean(id, bean!!.id, time))
                            }
                            MovieSingleDbManager.insertAccess(list)
                            finish()
                            val intent = Intent(RELOAD_SINGLE_ACTION)
                            intent.putExtra("is_done", done)
                            sendBroadcast(intent)
                        }
                    }
                    DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("db_num", bean!!.db_num))
                    params.add(BasicNameValuePair("is_done", done.toString()))
                    var add_ids = ""
                    for (i in toAddList.indices) {
                        if (i != 0) {
                            add_ids += ","
                        }
                        add_ids += toAddList[i]
                    }
                    var delete_ids = ""
                    for (i in toDeleteList.indices) {
                        if (i != 0) {
                            delete_ids += ","
                        }
                        delete_ids += toDeleteList[i]
                    }
                    params.add(BasicNameValuePair("add_ids", CryptTool.encrypt(add_ids)))
                    params.add(BasicNameValuePair("delete_ids", CryptTool.encrypt(delete_ids)))
                    NetManager.getInstance().addSingleAccess(params, callback)
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            yRecyclerView {
                backgroundColor = resources.getColor(R.color.bg)
                layoutManager = LinearLayoutManager(context)
                myAdapter = MyAdapter()
                adapter = myAdapter
                myAdapter.addHeaderView(HeaderView(context), false)
                setRecyclerItemListener(object : RecyclerItemListener() {
                    init {
                        clickable = true
                    }

                    override fun onItemClick(v: View, position: Int) {
                        val holder = getChildViewHolder(v) as Holder
                        myAdapter.getmCursor().moveToPosition(position - myAdapter.headerSize)
                        val id = myAdapter.getmCursor().getInt(0)
                        toAddList.remove(id)
                        toDeleteList.remove(id)
                        if (id in checkedList) {
                            holder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                            if (id in singlesList) {
                                toDeleteList.add(id)
                            }
                            checkedList.remove(id)
                        } else {
                            holder.checkbox.imageResource = R.drawable.single_access_multi_checked_icon
                            if (id !in singlesList) {
                                toAddList.add(id)
                            }
                            checkedList.add(id)
                        }
                    }
                })
            }.lparams(matchParent, wrapContent)
            view {
                backgroundColor = Color.parseColor("#e1e1e1")
            }.lparams(matchParent, dip(1))
        }
    }

    private inner class MyAdapter : RecyclerCursorAdapter<SingleBean>() {
        private val id_position = 0
        private val name_position = 1

        init {
            newCursor()
        }

        fun newCursor() {
            val db = DBHelper.getInstance().readableDatabase
            val cursor = db.rawQuery("select * from ${MovieSingleDbManager.table_single} order by update_time desc, id desc", null)
            changeCursor(cursor)

            for (c in cache) {
                c.value.close()
            }
            cache.clear()
            cache.put(-1, mCursor)
        }

        override fun getItem(position: Int): SingleBean? {
            mCursor.moveToPosition(position - headerSize)
            return MovieSingleDbManager.getItemByCursor(mCursor)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                val id = mCursor.getInt(id_position)
                val name = mCursor.getString(name_position)
                viewHolder.name.text = name
                var cursor = cache[id]
                if (cursor == null) {
                    cursor = MovieSingleDbManager.getSingleCursor(id)
                    cache.put(id, cursor)
                }
                viewHolder.count.text = "${cursor!!.count}部"

                val imageView = viewHolder.imgView
                setImage(cursor, imageView)

                if (id in checkedList) {
                    viewHolder.checkbox.imageResource = R.drawable.single_access_multi_checked_icon
                } else {
                    viewHolder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            var viewHolder = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                viewHolder = Holder(RelativeLayout(AppEngine.getContext()))
            }
            return viewHolder
        }
    }

    private fun setImage(cursor: Cursor, imageView: ImageView) {
        var imgUrl = ""
        if (cursor.moveToFirst()) {
            imgUrl = cursor.getString(MovieDbManager.IMAGE_P) ?: ""
        }
        Glide.with(imageView.context)
                .load(imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .placeholder(placeholder)
                .error(error)
                .into(imageView)
    }

    private inner class Holder(view: RelativeLayout) : RecyclerView.ViewHolder(view) {
        lateinit var imgView: ImageView
        lateinit var name: TextView
        lateinit var count: TextView
        lateinit var checkbox: ImageView
        lateinit var line: View

        init {
            view.apply {
                backgroundColor = resources.getColor(R.color.bg)
                layoutParams = RecyclerView.LayoutParams(matchParent, dip(81.5f))
                line = view {
                    backgroundColor = Color.parseColor("#e1e1e1")
                    layoutParams = RelativeLayout.LayoutParams(matchParent, dip(1)).apply {
                        leftMargin = dip(12)
                    }
                }
                view {
                    backgroundColor = Color.parseColor("#e1e1e1")
                    layoutParams = RelativeLayout.LayoutParams(dip(47.5f), dip(65.5f)).apply {
                        leftMargin = dip(15)
                        centerVertically()
                    }
                }
                imageView {
                    imageResource = R.drawable.movie_empty_icon
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    layoutParams = RelativeLayout.LayoutParams(dip(24.9f), dip(24.9f)).apply {
                        leftMargin = dip(26.5f)
                        centerVertically()
                    }
                }
                imgView = imageView {
                    id = 1
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = RelativeLayout.LayoutParams(dip(47.5f), dip(65.5f)).apply {
                        leftMargin = dip(15)
                        centerVertically()
                    }
                }
                name = textView {
                    id = 2
                    textSize = 15f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    singleLine = true
                    setLines(1)
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = RelativeLayout.LayoutParams(matchParent, wrapContent).apply {
                        topMargin = dip(23.4f)
                        leftMargin = dip(13)
                        rightMargin = dip(15)
                        rightOf(1)
                        leftOf(4)
                    }
                }
                count = textView {
                    id = 3
                    textSize = 13f
                    textColor = Color.parseColor("#818c91")
                    singleLine = true
                    setLines(1)
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = RelativeLayout.LayoutParams(matchParent, wrapContent).apply {
                        topMargin = dip(2)
                        leftMargin = dip(13)
                        rightMargin = dip(15)
                        rightOf(1)
                        leftOf(4)
                        below(2)
                    }
                }
                checkbox = imageView {
                    id = 4
                    layoutParams = RelativeLayout.LayoutParams(wrapContent, wrapContent).apply {
                        rightMargin = dip(15)
                        alignParentRight()
                        centerVertically()
                    }
                }
            }
        }
    }

    private inner class HeaderView(context: Context) : LinearLayout(context) {
        init {
            orientation = VERTICAL
            apply {
                val todoHolder = Holder(RelativeLayout(context))
                todoHolder.line.visibility = GONE
                todoHolder.name.text = "想看的电影"
                todoHolder.count.text = "${MovieTodoView.getCount()}部"
                setImage(MovieTodoView.getMyCursor(), todoHolder.imgView)
                addView(todoHolder.itemView)
                val doneHolder = Holder(RelativeLayout(context))
                doneHolder.name.text = "已看的电影"
                doneHolder.count.text = "${MovieDoneView.getCount()}部"
                setImage(MovieDoneView.getMyCursor(), doneHolder.imgView)
                addView(doneHolder.itemView)
                if (done == 0) {
                    todoHolder.checkbox.imageResource = R.drawable.single_access_checked_icon
                    doneHolder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                } else {
                    todoHolder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                    doneHolder.checkbox.imageResource = R.drawable.single_access_checked_icon
                }
                val l = View.OnClickListener {
                    if (done == 0) {
                        todoHolder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                        doneHolder.checkbox.imageResource = R.drawable.single_access_checked_icon
                    } else {
                        todoHolder.checkbox.imageResource = R.drawable.single_access_checked_icon
                        doneHolder.checkbox.imageResource = R.drawable.single_access_unchecked_icon
                    }
                    done = 1 - done
                }
                todoHolder.itemView.setOnClickListener(l)
                doneHolder.itemView.setOnClickListener(l)

                view {
                    backgroundColor = Color.parseColor("#e1e1e1")
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(1))
                }
                view {
                    backgroundColor = Color.parseColor("#e1e1e1")
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(1)).apply {
                        topMargin = dip(14.5f)
                    }
                }
                textView("加入我的影单(可以多选)") {
                    backgroundColor = resources.getColor(R.color.bg)
                    textSize = 15f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(47.5f)).apply {
                        leftMargin = dip(15)
                        bottomMargin = dip(1)
                    }
                }
                pressableLinearLayout {
                    backgroundColor = resources.getColor(R.color.bg)
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(81.5f))
                    onClick {
                        startActivity<SingleEditActivity>("type" to 1)
                    }
                    imageView {
                        imageResource = R.drawable.single_access_new_icon
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        layoutParams = LinearLayout.LayoutParams(dip(47.5f), dip(47.5f)).apply {
                            leftMargin = dip(15)
                            rightMargin = dip(13)
                        }
                    }
                    textView("新建影单") {
                        textSize = 15f
                        textColor = Color.parseColor("#818c91")
                    }
                }
            }
        }
    }
}
