package com.intlime.mark.activitys

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
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
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.tools.glide.MovieCoverKey
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.RecyclerCursorAdapter
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.yRecyclerView
import com.intlime.mark.view.widget.MovieStatusView
import com.intlime.mark.view.widget.PercentRatingBar
import com.intlime.mark.view.widget.percentRatingBar
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import java.util.*

/**
 * Created by wtuadn on 16/04/27.
 */
class SingleActivity : BaseActivity() {
    private var bean: SingleBean? = null
    private lateinit var myAdapter: MyAdapter
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (SingleEditActivity.EDIT_NAME_ACTION == action) {
                    val bean = intent.getParcelableExtra<SingleBean>(BEAN)
                    if (bean != null) {
                        toolbar.title = bean.name
                    }
                } else {
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
        applyUI()

        val filter = IntentFilter()
        filter.addAction(SingleEditActivity.EDIT_NAME_ACTION)
        filter.addAction(RELOAD_SINGLE_ACTION)
        registerReceiver(broadcastReceiver, filter)
//        ZhugeTool.track("进入自建影单详情")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch(e: Exception) {
        }
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = bean!!.name
                if (bean!!.id != 0) {
                    menu.add(0, 0, 0, "编辑").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    menu.add(0, 1, 0, "删除").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    onMenuItemClick { item ->
                        if (item != null) {
                            if (item.itemId == 0) {
                                startActivity<SingleEditActivity>(BEAN to bean!!, "type" to 2)
                            } else {
                                val dialog = DialogTool.getConfirmDialog("确定要删除这个影单吗？", null, null)
                                dialog.confirm_agree.setOnClickListener {
                                    dialog.dismiss()
                                    val callback = object : NetRequestCallBack() {
                                        override fun onDefault() {
                                            DialogTool.dismissWaitDialog()
                                        }

                                        override fun onSuccess(result: ArrayMap<*, *>?) {
                                            MovieSingleDbManager.deleteSingle(bean!!)
                                            sendBroadcast(Intent(RELOAD_SINGLE_ACTION))
                                            finish()
                                        }
                                    }
                                    DialogTool.showWaitDialog("删除中", DialogTool.CANCEL_ON_BACK, callback)
                                    NetManager.getInstance().deleteSingle(bean!!.id, callback)
                                }
                                dialog.show()
                            }
                        }
                        return@onMenuItemClick true
                    }
                }
            }.lparams(matchParent, dip(49))
            val emptyView = relativeLayout {
                backgroundColor = resources.getColor(R.color.bg)
                view {
                    id = 11
                    backgroundResource = R.drawable.movie_empty_icon
                }.lparams(dip(45.5f), dip(45.5f)) {
                    centerHorizontally()
                    above(12)
                }
                textView("暂无电影") {
                    id = 12
                    textSize = 15f
                    textColor = Color.parseColor("#818c91")
                    topPadding = dip(9)
                    bottomPadding = dip(80)
                }.lparams(wrapContent, wrapContent) {
                    centerInParent()
                }
            }.lparams(matchParent, matchParent)
            yRecyclerView {
                backgroundColor = resources.getColor(R.color.bg)
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                myAdapter = MyAdapter()
                adapter = myAdapter
                setRecyclerItemListener(MyRecyclerItemListener())
                registerEmptyView(emptyView)
            }.lparams(matchParent, matchParent)
        }
    }

    private inner class MyRecyclerItemListener : RecyclerItemListener() {
        init {
            clickable = true
            longClickable = true
        }

        override fun onItemClick(v: View?, position: Int) {
            val movieBean = myAdapter.getItem(position)
            if (movieBean != null) {
                startActivity<MovieDetailActivity>(BEAN to movieBean, "single_id" to bean!!.id)
            }
        }

        override fun onItemLongClick(v: View?, position: Int): Boolean {
            val bean = myAdapter.getItem(position) ?: return true
            MovieStatusView.show(bean, 0, null, object : MovieStatusView.OnDeleteSingleClickListener {
                override fun onDeleteSingleClicked(v: View) {
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<*, *>?) {
                            MovieSingleDbManager.deleteAccess(SingleAccessBean(this@SingleActivity.bean!!.id, bean.id))
                            sendBroadcast(Intent(RELOAD_SINGLE_ACTION))
                        }
                    }
                    DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("db_num", bean.db_num))
                    params.add(BasicNameValuePair("is_done", bean.done.toString()))
                    params.add(BasicNameValuePair("add_ids", CryptTool.encrypt("")))
                    params.add(BasicNameValuePair("delete_ids", CryptTool.encrypt(this@SingleActivity.bean!!.id.toString())))
                    NetManager.getInstance().addSingleAccess(params, callback)
                }
            }, null)
            return true
        }
    }

    private inner class MyAdapter() : RecyclerCursorAdapter<MovieBean>() {
        private val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
        private val error = EmptyDrawable(50f, 50f)

        private val name_position = 3
        private val img_url_position = 4
        private val pubdate_position = 8
        private val duration_position = 9
        private val genres_position = 10
        private val dbrating_position = 11

        init {
            newCursor()
        }

        fun newCursor() {
            val c = MovieSingleDbManager.getSingleCursor(bean!!.id)
            changeCursor(c)
        }

        override fun getItem(position: Int): MovieBean? {
            val bool = mCursor.moveToPosition(position - headerSize)
            if (!bool) return null
            return MovieDbManager.getInstance().getItemByCursor(mCursor)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                val name = mCursor.getString(name_position)
                val imgUrl = mCursor.getString(img_url_position)
                val pubdate = mCursor.getString(pubdate_position)
                val duration = mCursor.getString(duration_position)
                val movieType = mCursor.getString(genres_position)
                val db_rating = mCursor.getFloat(dbrating_position)

                viewHolder.nameText.text = if (name.isNullOrEmpty()) "-" else name
                viewHolder.pubdate.text = if (pubdate.isNullOrEmpty()) "-" else pubdate
                viewHolder.duration_movieType.text = ""

                val sb = StringBuilder()
                val movieTypes = movieType!!.split("/")
                if (movieTypes.size != 0) {
                    for (i in movieTypes.indices) {
                        val tag = movieTypes[i]
                        if (!TextUtils.isEmpty(tag)) {
                            if (i != 0) {
                                sb.append(" | ")
                            }
                            sb.append(tag)
                        }
                    }
                }
                if (duration.isNullOrEmpty() && sb.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = "-"
                } else if (duration.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = sb
                } else if (sb.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = duration
                } else {
                    viewHolder.duration_movieType.text = "${duration} - $sb"
                }

                viewHolder.rating.text = "${db_rating}分(豆瓣)"

                Glide.with(viewHolder.imageView.context)
                        .load(imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .signature(MovieCoverKey(imgUrl))
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .placeholder(placeholder)
                        .error(error)
                        .into(viewHolder.imageView)

                viewHolder.ratingBar.setRating(db_rating)
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
            lateinit var imageView: ImageView
            lateinit var nameText: TextView
            lateinit var pubdate: TextView
            lateinit var duration_movieType: TextView
            lateinit var ratingBar: PercentRatingBar
            lateinit var rating: TextView
            lateinit var line: View

            init {
                view.apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent)
                    pressableRelativeLayout {
                        PressableUtils.setPressableDrawable(this, MResource.getColor(R.color.black_pressed_color))
                        backgroundColor = Color.WHITE
                        layoutParams = LinearLayout.LayoutParams(matchParent, dip(133))
                        imageView = imageView {
                            id = 2
                            val lp = RelativeLayout.LayoutParams(dip(74.5f), dip(109))
                            lp.centerVertically()
                            lp.leftMargin = dip(15)
                            lp.rightMargin = dip(19)
                            layoutParams = lp
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        nameText = textView {
                            id = 3
                            textSize = 16f
                            textColor = resources.getColor(R.color.a_main_text_color)
                            singleLine = true
                            ellipsize = TextUtils.TruncateAt.END
                            rightPadding = dip(18)
                            val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                            lp.topMargin = dip(20)
                            lp.rightOf(imageView)
                            layoutParams = lp
                        }
                        pubdate = textView {
                            id = 4
                            textSize = 12f
                            textColor = Color.parseColor("#818c91")
                            singleLine = true
                            ellipsize = TextUtils.TruncateAt.END
                            rightPadding = dip(18)
                            val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                            lp.topMargin = dip(12)
                            lp.rightOf(imageView)
                            lp.below(nameText)
                            layoutParams = lp
                        }
                        duration_movieType = textView {
                            id = 5
                            textSize = 12f
                            textColor = Color.parseColor("#818c91")
                            singleLine = true
                            ellipsize = TextUtils.TruncateAt.END
                            rightPadding = dip(18)
                            val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                            lp.topMargin = dip(2)
                            lp.rightOf(imageView)
                            lp.below(pubdate)
                            layoutParams = lp
                        }
                        ratingBar = percentRatingBar {
                            id = 6
                            val lp = RelativeLayout.LayoutParams(dip(96.9f), dip(13.7f))
                            lp.topMargin = dip(8)
                            lp.rightOf(imageView)
                            lp.below(duration_movieType)
                            layoutParams = lp
                        }
                        rating = textView {
                            id = 7
                            textSize = 12f
                            textColor = Color.parseColor("#818c91")
                            singleLine = true
                            ellipsize = TextUtils.TruncateAt.END
                            rightPadding = dip(18)
                            val lp = RelativeLayout.LayoutParams(matchParent, wrapContent)
                            lp.topMargin = dip(7.5f)
                            lp.leftMargin = dip(9)
                            lp.rightOf(ratingBar)
                            lp.below(duration_movieType)
                            layoutParams = lp
                        }
                        line = view {
                            id = 8
                            val lp = RelativeLayout.LayoutParams(matchParent, dip(1))
                            lp.alignParentBottom()
                            lp.leftMargin = dip(12.2f)
                            layoutParams = lp
                            backgroundColor = Color.parseColor("#e1e1e1")
                        }
                    }
                }
            }
        }
    }
}