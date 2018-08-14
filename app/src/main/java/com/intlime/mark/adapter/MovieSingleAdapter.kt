package com.intlime.mark.adapter

import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
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
import com.intlime.mark.activitys.BaseActivity
import com.intlime.mark.activitys.MovieDetailActivity
import com.intlime.mark.activitys.SingleActivity
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.bean.SingleBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.CryptTool
import com.intlime.mark.tools.DensityUtils
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.db.DBHelper
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.tools.glide.MovieCoverKey
import com.intlime.mark.view.MovieSingleView
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.DividerItemDecoration
import com.intlime.mark.view.recyclerview.RecyclerCursorAdapter
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.yRecyclerView
import com.intlime.mark.view.widget.MovieStatusView
import com.wtuadn.pressable.PressableLinearLayout
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import java.util.*

/**
 * Created by wtuadn on 16/04/26.
 */
class MovieSingleAdapter : RecyclerCursorAdapter<SingleBean>() {
    private val cache = ArrayMap<Int, Cursor>()

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

    private fun getSingleId(position: Int): Int {
        mCursor.moveToPosition(position - headerSize)
        return mCursor.getInt(id_position)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        if (viewHolder is Holder) {
            val id = mCursor.getInt(id_position)
            val name = mCursor.getString(name_position)
            viewHolder.name.text = name
            viewHolder.adapter.notifyDataSetChanged(id)
            viewHolder.count.text = viewHolder.adapter.normalItemCount.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var viewHolder = super.onCreateViewHolder(parent, viewType)
        if (viewHolder == null) {
            viewHolder = Holder(LinearLayout(AppEngine.getContext()))
        }
        return viewHolder
    }

    private inner class Holder(view: LinearLayout) : RecyclerView.ViewHolder(view) {
        lateinit var name: TextView
        lateinit var count: TextView
        lateinit var adapter: MyAdapter

        init {
            view.apply {
                orientation = LinearLayout.VERTICAL
                view {
                    val lp = LinearLayout.LayoutParams(matchParent, dip(1))
                    lp.topMargin = dip(10)
                    layoutParams = lp
                    backgroundColor = Color.parseColor("#e1e1e1")
                }
                pressableRelativeLayout {
                    backgroundColor = resources.getColor(R.color.bg)
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(45f)).apply {
                        bottomMargin = dip(1)
                    }
                    name = textView {
                        id = 1
                        textSize = 15f
                        textColor = resources.getColor(R.color.a_main_text_color)
                        gravity = Gravity.CENTER_VERTICAL
                        singleLine = true
                        setLines(1)
                        ellipsize = TextUtils.TruncateAt.END
                        val lp = RelativeLayout.LayoutParams(matchParent, matchParent)
                        lp.addRule(RelativeLayout.LEFT_OF, 2)
                        layoutParams = lp
                        horizontalPadding = dip(15)
                    }
                    count = textView {
                        id = 2
                        textSize = 16f
                        textColor = resources.getColor(R.color.a_main_text_color)
                        gravity = Gravity.CENTER_VERTICAL
                        singleLine = true
                        setLines(1)
                        ellipsize = TextUtils.TruncateAt.END
                        compoundDrawablePadding = dip(16)
                        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.right_arrow, 0)
                        val lp = RelativeLayout.LayoutParams(wrapContent, matchParent)
                        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        layoutParams = lp
                        rightPadding = dip(12)
                    }
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    onClick {
                        val bean = getItem(adapterPosition)
                        if (bean != null) {
                            WWindowManager.getInstance().currentActivity.startActivity<SingleActivity>(BaseActivity.BEAN to bean)
                        }
                    }
                }
                val emptyView = relativeLayout {
                    layoutParams = LinearLayout.LayoutParams(matchParent, dip(200))
                    backgroundColor = resources.getColor(R.color.bg)
                    view {
                        id = 11
                        backgroundResource = R.drawable.movie_empty_icon
                    }.lparams(dip(33.5f), dip(33.5f)) {
                        centerHorizontally()
                        above(12)
                    }
                    textView("暂无电影") {
                        id = 12
                        textSize = 15f
                        textColor = Color.parseColor("#818c91")
                        topPadding = dip(9)
                    }.lparams(wrapContent, wrapContent) {
                        centerInParent()
                    }
                }
                yRecyclerView {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    backgroundColor = resources.getColor(R.color.bg)
                    verticalPadding = dip(12)
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
                    minimumHeight = dip(200)
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    addItemDecoration(DividerItemDecoration(Color.TRANSPARENT, dip(12), LinearLayoutManager.HORIZONTAL))
                    this@Holder.adapter = MyAdapter()
                    adapter = this@Holder.adapter
                    registerEmptyView(emptyView)
                    setRecyclerItemListener(object : RecyclerItemListener() {
                        init {
                            clickable = true
                            longClickable = true
                        }

                        override fun onItemClick(v: View?, position: Int) {
                            val bean = this@Holder.adapter.getItem(position)
                            if (bean != null) {
                                WWindowManager.getInstance().currentActivity.startActivity<MovieDetailActivity>(
                                        BaseActivity.BEAN to bean, "single_id" to getSingleId(adapterPosition))
                            }
                        }

                        override fun onItemLongClick(v: View?, position: Int): Boolean {
                            val bean = this@Holder.adapter.getItem(position)
                            if (bean != null) {
                                val p = layoutPosition
                                MovieStatusView.show(bean, 0, null, object : MovieStatusView.OnDeleteSingleClickListener {
                                    override fun onDeleteSingleClicked(v: View) {
                                        val callback = object : NetRequestCallBack() {
                                            override fun onDefault() {
                                                DialogTool.dismissWaitDialog()
                                            }

                                            override fun onSuccess(result: ArrayMap<*, *>?) {
                                                MovieSingleDbManager.deleteAccess(SingleAccessBean(id, bean.id))
                                                if (id == 0) MovieSingleView.getLikesCursor().close()
                                                cache.remove(id)
                                                ThreadManager.getInstance().post {
                                                    this@Holder.adapter.notifyDataSetChanged(id)
                                                    count.text = this@Holder.adapter.normalItemCount.toString()
                                                }
                                            }
                                        }
                                        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
                                        val id = getSingleId(p)
                                        val params = ArrayList<NameValuePair>()
                                        params.add(BasicNameValuePair("db_num", bean.db_num))
                                        params.add(BasicNameValuePair("is_done", bean.done.toString()))
                                        params.add(BasicNameValuePair("add_ids", CryptTool.encrypt("")))
                                        params.add(BasicNameValuePair("delete_ids", CryptTool.encrypt(id.toString())))
                                        NetManager.getInstance().addSingleAccess(params, callback)
                                    }
                                }, null)
                            }
                            return true
                        }
                    })
                }
                view {
                    val lp = LinearLayout.LayoutParams(matchParent, dip(1))
                    layoutParams = lp
                    backgroundColor = Color.parseColor("#e1e1e1")
                }
            }
        }

        inner class MyAdapter : RecyclerCursorAdapter<MovieBean>() {
            private val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
            private val error = EmptyDrawable(50f, 50f)
            private val name_position = 3
            private val img_url_position = 4

            override fun getItem(position: Int): MovieBean? {
                if (mCursor == null) return null
                val bool = mCursor.moveToPosition(position)
                if (!bool) return null
                return MovieDbManager.getInstance().getItemByCursor(mCursor)
            }

            override fun getNormalItemCount(): Int {
                return if (mCursor != null) mCursor.count else 0
            }

            fun notifyDataSetChanged(id: Int) {
                var c = cache[id]
                if (c == null) {
                    if (id == 0) {
                        c = MovieSingleView.getLikesCursor()
                    } else {
                        c = MovieSingleDbManager.getSingleCursor(id)
                        cache.put(id, c)
                    }
                }
                mCursor = c;
                notifyDataSetChanged()
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
                super.onBindViewHolder(viewHolder, position)
                if (viewHolder is MyHolder) {
                    val name = mCursor.getString(name_position)
                    val imgUrl = mCursor.getString(img_url_position)
                    viewHolder.textView.text = name
                    Glide.with(viewHolder.imgView.context)
                            .load(imgUrl)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .signature(MovieCoverKey(imgUrl))
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .placeholder(placeholder)
                            .error(error)
                            .into(viewHolder.imgView)
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
                var viewHolder = super.onCreateViewHolder(parent, viewType)
                if (viewHolder == null) {
                    viewHolder = MyHolder(PressableLinearLayout(AppEngine.getContext()))
                }
                return viewHolder
            }

            private inner class MyHolder(view: PressableLinearLayout) : RecyclerView.ViewHolder(view) {
                lateinit var imgView: ImageView
                lateinit var textView: TextView

                init {
                    view.apply {
                        orientation = LinearLayout.VERTICAL
                        PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                        imgView = imageView {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            val width = (WWindowManager.getInstance().width - DensityUtils.dp2px(AppEngine.getContext(), 48f)) / 3
                            layoutParams = LinearLayout.LayoutParams(width, (width * 1.467).toInt())
                        }
                        textView = textView {
                            textSize = 12f
                            textColor = resources.getColor(R.color.main_text_color)
                            gravity = Gravity.CENTER
                            singleLine = true
                            setLines(1)
                            ellipsize = TextUtils.TruncateAt.END
                            backgroundColor = Color.WHITE
                            horizontalPadding = dip(4)
                            layoutParams = LinearLayout.LayoutParams(matchParent, dip(30))
                        }
                    }
                }
            }
        }
    }
}