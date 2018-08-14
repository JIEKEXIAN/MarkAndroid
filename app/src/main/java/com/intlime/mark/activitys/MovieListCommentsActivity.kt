package com.intlime.mark.activitys

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.adapter.MovieListCommentsAdapter
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.bean.CommentBean
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.StringTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.LoadRecyclerView
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.widget.CommentInputView
import com.wtuadn.pressable.PressableLinearLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource

class MovieListCommentsActivity : BaseActivity(), CommentInputView.OnCommentListener, LoadRecyclerView.OnLoadListener {
    private val limit = 20
    private lateinit var bean: MovieListBean
    private var activityStackSortChanged: Boolean = false
    private lateinit var recyclerView: LoadRecyclerView
    private lateinit var commentInputView: CommentInputView
    private lateinit var imgView: ImageView
    private lateinit var name: TextView
    private lateinit var myAdapter: MovieListCommentsAdapter
    private var toReplyBean: CommentBean? = null
    private var currentCommentId = 0
    private val emptyView by lazy {
        return@lazy TextView(this).apply {
            text = "还没有评论"
            textSize = 15f
            textColor = Color.parseColor("#818c91")
            gravity = Gravity.CENTER_HORIZONTAL
            topPadding = dip(120)
            bottomPadding = dip(80)
            compoundDrawablePadding = dip(10)
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.comment_empty_icon, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bean: MovieListBean? = intent.getParcelableExtra(BEAN)
        if (bean == null) {
            finish()
            return
        }
        this.bean = bean
        currentCommentId = intent.getIntExtra("comment_id", 0)
        applyUI()
        applyData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        overridePendingTransition(R.anim.activity_right_in, R.anim.activity_full_left_out)
        activityStackSortChanged = true
    }

    override fun finish() {
        super.finish()
        if (activityStackSortChanged) {
            overridePendingTransition(R.anim.activity_full_left_in, R.anim.activity_right_out)
        }
    }

    private fun applyUI() {
        rootView = relativeLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "评论(0)"
            }.lparams(matchParent, dip(49))
            recyclerView = LoadRecyclerView(context).apply {
                backgroundColor = resources.getColor(R.color.bg)
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                myAdapter = MovieListCommentsAdapter(arrayListOf())
                adapter = myAdapter
                setRecyclerItemListener(MyRecyclerItemListener())
                setLoadListener(this@MovieListCommentsActivity)
                val headerView = PressableLinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    verticalPadding = dip(15)
                    leftPadding = dip(14.5f)
                    rightPadding = dip(12)
                    gravity = Gravity.CENTER_VERTICAL
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    onClick {
                        val intent = Intent(this@MovieListCommentsActivity, MovieListDetailActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        intent.putExtra(BEAN, bean)
                        startActivity(intent)
                    }
                    imgView = imageView {
                        layoutParams = ViewGroup.LayoutParams(dip(118.5f), dip(64))
                    }
                    name = textView {
                        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                        text = bean.name
                        textSize = 16f
                        textColor = resources.getColor(R.color.dark_blue)
                        setLineSpacing(0f, 1.2f)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        leftPadding = dip(15.3f)
                        compoundDrawablePadding = dip(5)
                        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.right_arrow, 0)
                    }
                }.lparams(matchParent, dip(94))
                myAdapter.addHeaderView(headerView)
            }.lparams(matchParent, matchParent) {
                below(toolbar)
                above(3)
            }
            addView(recyclerView)
            commentInputView = CommentInputView(context, this)
            commentInputView.id = 3
            commentInputView.onCommentListener = this@MovieListCommentsActivity
            addView(commentInputView)
        }
    }

    private fun applyData() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val movieListBean = result["MovieListBean"] as MovieListBean
                bean.name = movieListBean.name
                bean.img_url = movieListBean.img_url
                bean.comments = movieListBean.comments
                toolbar.title = "评论(${bean.comments})"
                name.text = bean.name
                Glide.with(imgView.context)
                        .load(StringTool.getQiniuScaledImgUrl(bean.img_url, imgView.layoutParams.width, imgView.layoutParams.height))
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .placeholder(ColorDrawable(Color.parseColor("#e1e1e1")))
                        .error(EmptyDrawable(40f, 40f))
                        .centerCrop()
                        .into(imgView)
                val list = result["list"] as? MutableList<CommentBean>
                if (list != null) {
                    if (list.size < limit) {
                        recyclerView.isCanLoad = false
                    } else {
                        recyclerView.isCanLoad = true
                    }
                    myAdapter.totalCommentCount = bean.comments
                    if (list.size == 1 && list[0].localType == 1) {
                        myAdapter.addFooterView(emptyView)
                    }
                    if (currentCommentId > 0) {
                        val bean = list.firstOrNull()
                        val typeName = bean?.name
                        if (!"我的评论".equals(typeName)) {
                            var cb = CommentBean()
                            cb.name = "我的评论"
                            cb.localType = 1
                            list.add(0, cb)
                            cb = CommentBean()
                            cb.localType = 3
                            list.add(1, cb)
                        }
                    }
                    myAdapter.lists.addAll(list)
                    myAdapter.notifyDataSetChanged()
                }
                val intent = Intent(RELOAD_SINGLE_ACTION)
                intent.putExtra(BEAN, bean)
                sendBroadcast(intent)
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getMovieListComment(bean.id, currentCommentId, 0, callback)
    }

    override fun onLoad() {
        NetManager.getInstance().getMovieListComment(bean.id, currentCommentId, myAdapter.lists.last().timestamp, object : NetRequestCallBack() {
            override fun onDefault() {
                recyclerView.loadFinish()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["list"] as List<CommentBean>
                if (list.size < limit) {
                    recyclerView.isCanLoad = false
                } else {
                    recyclerView.isCanLoad = true
                }
                val start = myAdapter.itemCount
                myAdapter.lists.addAll(list)
                myAdapter.notifyItemRangeInserted(start, list.size)
            }
        })
    }

    override fun onComment(comment: String) {
        val commentTrimed = comment.trim()
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val cb = CommentBean(result["id"] as Int,
                        Session.uid,
                        bean.id,
                        SettingManager.getInstance().userHeadImgUrl,
                        commentTrimed,
                        SettingManager.getInstance().nickname,
                        timestamp = (System.currentTimeMillis() / 1000).toInt())
                if (toReplyBean != null) {
                    cb.preContent = toReplyBean!!.content
                    cb.preName = toReplyBean!!.name
                }
                for (i in myAdapter.lists.indices) {
                    val item = myAdapter.lists[i]
                    if (item.localType == 1 && item.name.contains("最新评论")) {
                        ThreadManager.getInstance().post {
                            myAdapter.totalCommentCount = ++bean.comments
                            toolbar.title = "评论(${bean.comments})"
                            myAdapter.notifyNormalItemChanged(i)
                            myAdapter.lists.add(i + 1, cb)
                            myAdapter.notifyNormalItemInserted(i + 1)
                            myAdapter.removeFooterView(emptyView)
                            commentInputView.clear()
                            toReplyBean = null
                            val intent = Intent(RELOAD_SINGLE_ACTION)
                            intent.putExtra(BEAN, bean)
                            sendBroadcast(intent)
                        }
                        break
                    }
                }
                ToastTool.show("评论成功")
            }
        }
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
        if (toReplyBean != null) {
            NetManager.getInstance().replyComment(toReplyBean!!.id, commentTrimed, callback)
        } else {
            NetManager.getInstance().commentMovieList(bean.id, commentTrimed, callback)
        }
    }

    private inner class MyRecyclerItemListener : RecyclerItemListener() {
        init {
            clickable = true
            createContextMenuable = true
        }

        override fun onItemClick(v: View?, position: Int) {
            v?.showContextMenu()
        }

        override fun onItemCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?, position: Int) {
            val bean = myAdapter.getItem(position)
            if (menu == null || bean == null || v == null) return
            if (v.tag != null && v.tag as Boolean) {
                handleReportlMenu(bean, menu, position, v)
            } else {
                handleNormalMenu(bean, menu, position, v)
            }
            v.tag = null
        }

        private fun handleReportlMenu(bean: CommentBean, menu: ContextMenu, position: Int, v: View) {
            val listener = MenuItem.OnMenuItemClickListener { item ->
                val callback = object : NetRequestCallBack() {
                    override fun onSuccess(result: ArrayMap<*, *>) {
                        ToastTool.show("举报成功")
                    }
                }
                NetManager.getInstance().reportComment(bean.id, item.itemId, callback)
                return@OnMenuItemClickListener true
            }
            menu.add(0, 1, 0, "段子或无意义评论").setOnMenuItemClickListener(listener)
            menu.add(0, 2, 0, "恶意攻击谩骂").setOnMenuItemClickListener(listener)
            menu.add(0, 3, 0, "营销广告").setOnMenuItemClickListener(listener)
            menu.add(0, 4, 0, "淫秽色情").setOnMenuItemClickListener(listener)
            menu.add(0, 5, 0, "政治反动").setOnMenuItemClickListener(listener)
            menu.add(0, 6, 0, "其它").setOnMenuItemClickListener(listener)
        }

        private fun handleNormalMenu(bean: CommentBean, menu: ContextMenu, position: Int, v: View) {
            val listener = MenuItem.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        toReplyBean = bean
                        commentInputView.setHint("回复${bean.name}:")
                        commentInputView.showKeyboard()
                        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                    }
                    2 -> {
                        clipboardManager.text = bean.content
                        ToastTool.show("已复制到剪切板")
                    }
                    3 -> {
                        ThreadManager.getInstance().post {
                            v.tag = true
                            v.showContextMenu()
                        }
                    }
                    4 -> {
                        val callback = object : NetRequestCallBack() {
                            override fun onDefault() {
                                DialogTool.dismissWaitDialog()
                            }

                            override fun onSuccess(result: ArrayMap<*, *>) {
                                ThreadManager.getInstance().post {
                                    var i = 0
                                    while (i < myAdapter.lists.size) {
                                        val cb = myAdapter.lists[i]
                                        if (cb.name.contains("最新评论")) {
                                            myAdapter.notifyNormalItemChanged(i)
                                        }
                                        if (bean.id == cb.id) {
                                            val topCB = myAdapter.lists[i - 1]
                                            if (topCB != null && topCB.localType == 1 && !topCB.name.contains("最新评论")) {
                                                val bottomCB = myAdapter.lists[i + 1]
                                                if (bottomCB != null && bottomCB.localType == 1) {//当前分类只有一条评论，连分类一起删除
                                                    myAdapter.lists.removeAt(i - 1)
                                                    myAdapter.lists.removeAt(i - 1)
                                                    myAdapter.notifyItemRangeRemoved(myAdapter.headerSize + i - 1, myAdapter.headerSize + i)
                                                    i--
                                                    continue
                                                }
                                            }
                                            myAdapter.lists.removeAt(i)
                                            myAdapter.notifyNormalItemRemoved(i)
                                            myAdapter.notifyNormalItemChanged(i - 1)
                                            i--
                                        }
                                        i++
                                    }
                                    myAdapter.totalCommentCount = --this@MovieListCommentsActivity.bean.comments
                                    toolbar.title = "评论(${this@MovieListCommentsActivity.bean.comments})"
                                    if (myAdapter.lists.size == 1 && myAdapter.lists[0].localType == 1) {
                                        myAdapter.addFooterView(emptyView)
                                    }
                                }
                            }
                        }
                        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
                        NetManager.getInstance().deleteSelfComment(bean.id, callback)
                    }
                }
                return@OnMenuItemClickListener true
            }
            menu.add(0, 1, 0, "回复").setOnMenuItemClickListener(listener)
            menu.add(0, 2, 0, "复制").setOnMenuItemClickListener(listener)
            if (bean.uid != Session.uid) {
                menu.add(0, 3, 0, "举报").setOnMenuItemClickListener(listener)
            } else {
                menu.add(0, 4, 0, "删除").setOnMenuItemClickListener(listener)
            }
        }
    }
}
