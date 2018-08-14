package com.intlime.mark.activitys

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.Toolbar
import android.view.ContextMenu
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.adapter.MyNewsCommentAdapter
import com.intlime.mark.adapter.MyNewsNotifyAdapter
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.CommentBean
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.widget.CommentInputView
import com.intlime.mark.view.widget.lor.LoadOrRefreshView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource

/**
 * Created by wtuadn on 16-6-21.
 */
class MyNewsActivity : BaseActivity(), LoadOrRefreshView.OnLORListener, CommentInputView.OnCommentListener {
    private val limit = 20
    private val commentInputViewId = 8907
    private lateinit var notifyCount: TextView
    private lateinit var commentAdapter: MyNewsCommentAdapter
    private val commentLOR by lazy {
        var lor: LoadOrRefreshView? = null
        (rootView as _RelativeLayout).apply {
            lor = LoadOrRefreshView(context).apply {
                setOnLORListener(this@MyNewsActivity)
                commentAdapter = MyNewsCommentAdapter(arrayListOf())
                getmLoadRecyclerView().adapter = commentAdapter
                getmLoadRecyclerView().setRecyclerItemListener(MyRecyclerItemListener())
                getmLoadRecyclerView().registerEmptyView(emptyView)
                emptyView.visibility = View.GONE
            }.lparams(matchParent, matchParent) {
                below(4)
                above(commentInputViewId)
            }
            addView(lor)
        }
        return@lazy lor!!
    }
    private lateinit var notifyAdapter: MyNewsNotifyAdapter
    private val notifyLOR by lazy {
        var lor: LoadOrRefreshView? = null
        (rootView as _RelativeLayout).apply {
            lor = LoadOrRefreshView(context).apply {
                setOnLORListener(this@MyNewsActivity)
                notifyAdapter = MyNewsNotifyAdapter(arrayListOf())
                getmLoadRecyclerView().adapter = notifyAdapter
                getmLoadRecyclerView().setRecyclerItemListener(object : RecyclerItemListener() {
                    init {
                        clickable = true
                    }

                    override fun onItemClick(v: View?, position: Int) {
                        val bean = notifyAdapter.getItem(position) ?: return
                        if(bean.type==0) {
                            startActivity<MovieListCommentsActivity>(BEAN to MovieListBean(id = bean.singleId), "comment_id" to bean.id)
                        }else{
                            startActivity<MovieListCommentsActivity>(BEAN to MovieListBean(id = bean.singleId))
                        }
                    }
                })
            }.lparams(matchParent, matchParent) {
                below(4)
                above(commentInputViewId)
            }
            addView(lor)
        }
        lor?.autoRefresh()
        return@lazy lor!!
    }
    private val civ by lazy {
        var v: CommentInputView? = null
        (rootView as _RelativeLayout).apply {
            v = CommentInputView(context, this)
            v!!.id = commentInputViewId
            v!!.onCommentListener = this@MyNewsActivity
            addView(v)
        }
        return@lazy v!!
    }
    private var toReplyBean: CommentBean? = null
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        customAnimation = intent.getBooleanExtra(ANIMATION, true)
        super.onCreate(savedInstanceState)
        applyUI()
        commentLOR.autoRefresh()
    }

    private fun applyUI() {
        var radioGroup: RadioGroup? = null
        rootView = relativeLayout {
            backgroundColor = resources.getColor(R.color.bg)
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                backgroundColor = Color.TRANSPARENT
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "我的消息"
            }.lparams(matchParent, dip(49))
            radioGroup = radioGroup {
                id = 1
                orientation = LinearLayout.HORIZONTAL
                val d = GradientDrawable()
                d.setColor(Color.TRANSPARENT)
                d.setCornerRadius(dip(2).toFloat())
                d.setStroke(1, resources.getColor(R.color.dark_blue))
                backgroundDrawable = d
                radioButton {
                    id = 2
                    isChecked = true
                    gravity = Gravity.CENTER
                    buttonDrawable = ColorDrawable()
                    text = "评论"
                    textSize = 13f
                    setTextColor(resources.getColorStateList(R.color.selector_list_mode_text_color))
                    backgroundResource = R.drawable.selector_list_mode_radio_bg_left
                    lparams(0, matchParent) {
                        weight = 1f
                    }
                }
                view {
                    lparams(1, matchParent)
                    backgroundColor = resources.getColor(R.color.dark_blue)
                }
                radioButton {
                    id = 3
                    gravity = Gravity.CENTER
                    buttonDrawable = ColorDrawable()
                    text = "通知"
                    textSize = 13f
                    setTextColor(resources.getColorStateList(R.color.selector_list_mode_text_color))
                    backgroundResource = R.drawable.selector_list_mode_radio_bg_right
                    lparams(0, matchParent) {
                        weight = 1f
                    }
                }
            }.lparams(matchParent, dip(26)) {
                below(R.id.toolbar)
                horizontalMargin = dip(38)
                verticalMargin = dip(9)
            }
            notifyCount = textView {
                minWidth = dip(17.5f)
                val count = SettingManager.getInstance().notifyCount
                if (count <= 0) visibility = View.GONE
                text = "$count"
                textSize = 11f
                textColor = Color.WHITE
                gravity = Gravity.CENTER
                horizontalPadding = dip(4)
                backgroundResource = R.drawable.red_circle
            }.lparams(wrapContent, dip(17.5f)) {
                below(R.id.toolbar)
                alignParentRight()
                rightMargin = dip(42)
            }
            view {
                id = 4
                backgroundColor = Color.parseColor("#e1e1e1")
            }.lparams(matchParent, dip(1)) {
                below(1)
            }
            emptyView = textView {
                text = "暂无评论"
                textSize = 15f
                textColor = Color.parseColor("#818c91")
            }.lparams {
                centerHorizontally()
                topMargin = dip(89)
                below(4)
            }
        }
        var isFirstClick = true
        radioGroup!!.setOnCheckedChangeListener { radioGroup, id ->
            if (id == 2) {
                commentLOR.visibility = View.VISIBLE
                notifyLOR.visibility = View.GONE
                emptyView.text = "暂无评论"
                commentLOR.getmLoadRecyclerView().registerEmptyView(emptyView)
                notifyLOR.getmLoadRecyclerView().registerEmptyView(null)
                if (toReplyBean != null) {
                    civ.visibility = View.VISIBLE
                }
            } else {
                commentLOR.visibility = View.GONE
                notifyLOR.visibility = View.VISIBLE
                emptyView.text = "暂无通知"
                commentLOR.getmLoadRecyclerView().registerEmptyView(null)
                notifyLOR.getmLoadRecyclerView().registerEmptyView(emptyView)
                if (isFirstClick) {
                    isFirstClick = false
                    emptyView.visibility = View.GONE
                    notifyCount.visibility = View.GONE
                    SettingManager.getInstance().notifyCount = 0
                    sendBroadcast(Intent(NOTIFY_COUNT_ACTION))
                }
                if (toReplyBean != null) {
                    civ.visibility = View.GONE
                    WWindowManager.getInstance().hideInput()
                }
            }
        }
    }

    override fun onRefresh(lor: LoadOrRefreshView) {
        if (lor === commentLOR) {
            NetManager.getInstance().getUserComments(0, object : NetRequestCallBack() {
                override fun onDefault() {
                    lor.finishLOR()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    val list = result["list"] as List<CommentBean>
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    } else {
                        lor.getmLoadRecyclerView().isCanLoad = true
                    }
                    commentAdapter.lists.clear()
                    commentAdapter.lists.addAll(list)
                    commentAdapter.notifyDataSetChanged()

                    SettingManager.getInstance().commentsCount = 0
                    sendBroadcast(Intent(NOTIFY_COUNT_ACTION))
                }
            })
        } else {
            NetManager.getInstance().getUserNotify(0, object : NetRequestCallBack() {
                override fun onDefault() {
                    lor.finishLOR()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    val list = result["list"] as List<CommentBean>
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    } else {
                        lor.getmLoadRecyclerView().isCanLoad = true
                    }
                    notifyAdapter.lists.clear()
                    notifyAdapter.lists.addAll(list)
                    notifyAdapter.notifyDataSetChanged()

                    SettingManager.getInstance().notifyCount = 0
                    sendBroadcast(Intent(NOTIFY_COUNT_ACTION))
                }
            })
        }
    }

    override fun onLoad(lor: LoadOrRefreshView) {
        if (lor === commentLOR) {
            NetManager.getInstance().getUserComments(commentAdapter.lists.last().timestamp, object : NetRequestCallBack() {
                override fun onDefault() {
                    lor.finishLOR()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    val list = result["list"] as List<CommentBean>
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    } else {
                        lor.getmLoadRecyclerView().isCanLoad = true
                    }
                    val start = commentAdapter.itemCount
                    commentAdapter.lists.addAll(list)
                    commentAdapter.notifyItemRangeInserted(start, list.size)
                }
            })
        } else {
            NetManager.getInstance().getUserComments(notifyAdapter.lists.last().timestamp, object : NetRequestCallBack() {
                override fun onDefault() {
                    lor.finishLOR()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    val list = result["list"] as List<CommentBean>
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    } else {
                        lor.getmLoadRecyclerView().isCanLoad = true
                    }
                    val start = notifyAdapter.itemCount
                    notifyAdapter.lists.addAll(list)
                    notifyAdapter.notifyItemRangeInserted(start, list.size)
                }
            })
        }
    }

    override fun onComment(comment: String) {
        toReplyBean ?: return
        val commentTrimed = comment.trim()
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                ThreadManager.getInstance().post {
                    civ.clear()
                    civ.visibility = View.GONE
                    toReplyBean = null
                }
                ToastTool.show("评论成功")
            }
        }
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
        NetManager.getInstance().replyComment(toReplyBean!!.id, commentTrimed, callback)
    }

    override fun finish() {
        customAnimation = true
        super.finish()
        if ("from_push" == intent.action) {
            SettingManager.getInstance().commentsCount = 0
            sendBroadcast(Intent(NOTIFY_COUNT_ACTION))
            if (WWindowManager.getInstance().windows.isEmpty()) {
                startActivity<MainActivity>(BaseActivity.ANIMATION to true)
            }
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
            val bean = commentAdapter.getItem(position)
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
                        civ.visibility = View.VISIBLE
                        civ.setHint("回复${bean.name}:")
                        civ.showKeyboard()
                    }
                    2 -> {
                        startActivity<MovieListDetailActivity>(BEAN to MovieListBean(id = bean.singleId))
                    }
                    3 -> {
                        ThreadManager.getInstance().post {
                            v.tag = true
                            v.showContextMenu()
                        }
                    }
                }
                return@OnMenuItemClickListener true
            }
            menu.add(0, 1, 0, "回复").setOnMenuItemClickListener(listener)
            menu.add(0, 2, 0, "查看影单").setOnMenuItemClickListener(listener)
            menu.add(0, 3, 0, "举报").setOnMenuItemClickListener(listener)
        }
    }
}