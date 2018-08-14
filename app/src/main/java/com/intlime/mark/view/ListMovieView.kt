package com.intlime.mark.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.support.v4.util.ArrayMap
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.activitys.BaseActivity
import com.intlime.mark.activitys.MovieDetailActivity
import com.intlime.mark.activitys.MultiShareActivity
import com.intlime.mark.adapter.ListModeAdapter
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.view.recyclerview.PreCachingLinearLayoutManager
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.YRecyclerView
import com.intlime.mark.view.recyclerview.yRecyclerView
import com.intlime.mark.view.widget.MovieStatusView
import com.intlime.mark.view.widget.QuickPosBar
import com.intlime.mark.view.widget.quickPosBar
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import java.util.*

/**
 * Created by root on 16-3-12.
 * @param mode 1:想看 2:已看
 */
class ListMovieView(context: Context, val mode: Int = 1) : RelativeLayout(context), Runnable {
    val myAdapter: ListModeAdapter = ListModeAdapter(mode)
    private lateinit var recyclerView: YRecyclerView
    private lateinit var groupText: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var movieHeaderView: MovieHeaderView
    private lateinit var detector: GestureDetector
    private var isDragging: Boolean = false
    private var canUpdateMovieWord: Boolean = false
    private var barInited = false
    private val quickPosBar by lazy {
        barInited = true
        quickPosBar {
            val lp = LayoutParams(dip(25), matchParent)
            lp.alignParentRight()
            layoutParams = lp
            posChangeListener = object : QuickPosBar.OnPosChangeListener {
                override fun onPosChange(s: String, pos: Int) {
                    myAdapter.lastPos = Int.MAX_VALUE
                    if (pos == 0) {
                        (recyclerView.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(0, 0)
                        myAdapter.notifyDataSetChanged()
                        return
                    }
                    if (myAdapter.groupMap.contains(s)) {
                        (recyclerView.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(myAdapter.groupMap[s]!!, 0)
                        myAdapter.notifyDataSetChanged()
                    } else if (myAdapter.canReloadGroupMap) {
                        myAdapter.canReloadGroupMap = false
                        var jumped = false
                        var temp = '}'
                        val cursor = myAdapter.cCursor
                        cursor.moveToPosition(-1)
                        while (cursor.moveToNext()) {
                            val fChar = cursor.getString(MovieDbManager.PINYIN_P)[0]
                            if (!temp.equals(fChar)) {
                                temp = fChar
                                myAdapter.groupMap.put(if (fChar.equals('~')) "#" else fChar.toString(), cursor.position)
                                if (!jumped && s.equals(fChar.toString())) {
                                    (recyclerView.layoutManager as LinearLayoutManager)
                                            .scrollToPositionWithOffset(cursor.position, 0)
                                    myAdapter.notifyDataSetChanged()
                                    jumped = true
                                }
                            }
                        }
                        if (!jumped && pos == 27) {
                            recyclerView.scrollToPosition(myAdapter.itemCount - 1)
                            myAdapter.notifyDataSetChanged()
                            return
                        }
                    } else {
                        if (pos == 27) {
                            recyclerView.scrollToPosition(myAdapter.itemCount - 1)
                            myAdapter.notifyDataSetChanged()
                            return
                        }
                    }
                }

            }
        }
    }
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            exitMultiMode(true)
        }
    }

    init {
        detector = GestureDetector(context, MyGestureListener())
        initScrollView(context)
        initEmptyView()
        initGroupText()

        AppEngine.getContext().registerReceiver(broadcastReceiver,
                IntentFilter(MainMovieView.EXIT_MULTI_CHOICE_ACTION))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            AppEngine.getContext().unregisterReceiver(broadcastReceiver)
        } catch (ignore: Exception) {
        }

    }

    private fun initScrollView(context: Context) {
        scrollView = myScrollView {
            val lp = LayoutParams(matchParent, matchParent)
            layoutParams = lp
            isFillViewport = true
            viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val height = movieHeaderView.measuredHeight
                    if (height != 0) {
                        viewTreeObserver.removeOnPreDrawListener(this)
                        scrollTo(0, height)
                    }
                    return true
                }
            })
            verticalLayout {
                movieHeaderView = MovieHeaderView(context)
                addView(movieHeaderView)
                recyclerView = yRecyclerView {
                    inVisible = INVISIBLE
                    val rootView = (context as BaseActivity).rootView
                    layoutParams = LayoutParams(matchParent, rootView.measuredHeight - dip(141.9f))
                    setHasFixedSize(true)
                    val llm = PreCachingLinearLayoutManager(context, dip(300))
                    //                    llm.stackFromEnd = true
                    llm.recycleChildrenOnDetach = true
                    layoutManager = llm
                    recycledViewPool = ListModeAdapter.pool
                    adapter = myAdapter
                    scrollToPosition(0)
                    setRecyclerItemListener(object : RecyclerItemListener() {
                        init {
                            clickable = true
                            longClickable = true
                        }

                        override fun onItemClick(v: View?, position: Int) {
                            val bean = myAdapter.getItem(position)
                            if (bean == null || bean.cursorPosition == -2) return
                            val intent = Intent(AppEngine.getContext(), MovieDetailActivity::class.java)
                            intent.putExtra(BaseActivity.BEAN, bean)
                            WWindowManager.getInstance().currentActivity.startActivity(intent)
                        }

                        override fun onItemLongClick(v: View?, position: Int): Boolean {
                            val bean = myAdapter.getItem(position)
                            if (bean == null || bean.cursorPosition == -2) return true
                            MovieStatusView.show(bean, 0, object : MovieStatusView.OnMultiClickListener {
                                override fun onMultiClicked(v: View) {
                                    prepareMultiMode(position)
                                }
                            }, null, null)
                            return true
                        }
                    })
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            var translated = false
                            val fp = llm.findFirstVisibleItemPosition()
                            if (fp > -1) {
                                val fvh = recyclerView.findViewHolderForLayoutPosition(fp)
                                if (fvh is ListModeAdapter.Holder) {
                                    groupText.text = fvh.group.text
                                    val sp = fp + 1
                                    if (sp < myAdapter.itemCount) {
                                        val svh = recyclerView.findViewHolderForLayoutPosition(sp)
                                        if (svh is ListModeAdapter.Holder && svh.group.isShown) {
                                            if (svh.itemView.top < groupText.measuredHeight) {
                                                groupText.translationY = (svh.itemView.top - groupText.measuredHeight).toFloat()
                                                translated = true
                                            }
                                        }
                                    }
                                }
                            }
                            if (!translated) {
                                groupText.translationY = 0f
                            }
                        }
                    })
                }
            }
        }
    }

    private fun initEmptyView() {
        val text1 = if (mode == 1) "无想看的电影" else "无已看的电影"
        val text2 = if (mode == 1) "你可以点击右上角的搜索按钮添加想看的电影" else "长按电影图片并选择已看可以存档电影"
        val icon = if (mode == 1) R.drawable.todo_empty_icon else R.drawable.done_empty_icon
        val emptyView = MovieEmptyView(context, icon, text1, text2)
        addView(emptyView, 0)
        recyclerView.registerEmptyView(emptyView)
        if (recyclerView.visibility == View.GONE) recyclerView.visibility = View.INVISIBLE
    }

    private fun initGroupText() {
        groupText = textView {
            textColor = Color.parseColor("#6f93a2")
            textSize = 13f
            singleLine = true
            gravity = Gravity.CENTER_VERTICAL
            val lp = LayoutParams(matchParent, dip(26))
            layoutParams = lp
            backgroundColor =Color.parseColor("#ebebeb")
            leftPadding = dip(12.5f)
        }
        myAdapter.pinGroupText = groupText
    }

    fun onSortChanged(type: Int, cursor: Cursor, scrollToTop:Boolean = true) {
        myAdapter.cCursor = cursor
        myAdapter.type = type
        myAdapter.onDataChanged()
        if(scrollToTop) recyclerView.scrollToPosition(0)
        if (type == 4) {
            quickPosBar.visibility = VISIBLE
            myAdapter.groupMap.clear()
        } else if (barInited) {
            quickPosBar.visibility = GONE
        }
    }

    private fun prepareMultiMode(position: Int) {
        myAdapter.multiSelectList.add(position)
        myAdapter.setMultiSelect(true, true)
        val titleLayout = View.inflate(context, R.layout.multi_mode_title_layout, null) as ViewGroup
        var lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MResource.getDimensionPixelSize(R.dimen.title_bar_height))
        (parent.parent.parent as ViewGroup).addView(titleLayout, lp)
        titleLayout.getChildAt(1).setOnClickListener { exitMultiMode(true) }
        val title = titleLayout.getChildAt(0) as TextView
        myAdapter.outMultiSelectListener = View.OnClickListener {
            title.text = String.format("已选择%d部", myAdapter.multiSelectList.size)
        }

        val tabLayout = View.inflate(context, R.layout.multi_mode_tab_layout, null) as ViewGroup
        lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MResource.getDimensionPixelSize(R.dimen.title_bar_height))
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        (parent.parent.parent as ViewGroup).addView(tabLayout, lp)

        val wrapperListener = View.OnClickListener { v ->
            if (myAdapter.multiSelectList.isEmpty()) {
                ToastTool.show("还没选择电影哦")
                return@OnClickListener
            }
            if (v === tabLayout.getChildAt(0)) {
                multiDone(mode)
            } else if (v === tabLayout.getChildAt(1)) {
                multiCollect()
            } else if (v === tabLayout.getChildAt(2)) {
                multiShare()
            } else {
                multiDelete()
            }
        }
        var tab = tabLayout.getChildAt(0) as TextView
        if (mode == 1) {
            tab.text = "已看"
            tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tab_done_unchecked, 0, 0)
        } else {
            tab.text = "想看"
            tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_todo_icon, 0, 0)
        }
        tab.setOnClickListener(wrapperListener)
        tab = tabLayout.getChildAt(1) as TextView
        tab.text = "喜欢"
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_collect_icon, 0, 0)
        tab.setOnClickListener(wrapperListener)
        tab = tabLayout.getChildAt(2) as TextView
        tab.text = "分享"
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_share_icon, 0, 0)
        tab.setOnClickListener(wrapperListener)
        tab = tabLayout.getChildAt(3) as TextView
        tab.text = "删除"
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_delete_icon, 0, 0)
        tab.setOnClickListener(wrapperListener)
    }

    private fun exitMultiMode(notify: Boolean = false) {
        if (myAdapter.multiSelect) {
            myAdapter.setMultiSelect(false, notify)
            val root = parent.parent.parent as ViewGroup
            root.removeViewAt(root.childCount - 1)
            root.removeViewAt(root.childCount - 1)
        }
    }

    private fun multiDone(type: Int) {
        val beanList = ArrayList<MovieBean>()
        val stringBuilder = StringBuilder()
        var isFirst = true
        for (i in myAdapter.multiSelectList.indices) {
            val position = myAdapter.multiSelectList[i]
            val bean = myAdapter.getItem(position) ?: continue
            bean.cursorPosition = position
            beanList.add(bean)
            if (isFirst) {
                isFirst = false
            } else {
                stringBuilder.append(",")
            }
            stringBuilder.append(bean.id)
        }
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<Any, Any>) {
                for (i in beanList.indices) {
                    val bean = beanList[i]
                    bean.done = 2 - type
                    bean.update_time = result["time"] as Long
                    bean.watchTime = if (bean.done == 1) bean.update_time else 0
                }
                MovieDbManager.getInstance().update(beanList)
                ThreadManager.getInstance().post {
                    exitMultiMode()
                    AppEngine.getContext().sendBroadcast(Intent(BaseActivity.RELOAD_ALL_ACTION))
                }
            }
        }
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("type", type.toString()))
        NetManager.getInstance().doneMovie(stringBuilder.toString(), params, callback)
    }

    private fun multiCollect() {
        val beanList = ArrayList<MovieBean>()
        val stringBuilder = StringBuilder()
        var isFirst = true
        for (i in myAdapter.multiSelectList.indices) {
            val position = myAdapter.multiSelectList[i]
            val bean = myAdapter.getItem(position) ?: continue
            bean.cursorPosition = position
            beanList.add(bean)
            if (isFirst) {
                isFirst = false
            } else {
                stringBuilder.append(",")
            }
            stringBuilder.append(bean.id)
        }
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<Any, Any>) {
                val list = ArrayList<SingleAccessBean>()
                for (i in beanList.indices) {
                    list.add(SingleAccessBean(0, beanList[i].id, result["time"] as Long))
                }
                MovieSingleDbManager.insertAccess(list)
                ThreadManager.getInstance().post { exitMultiMode(true) }
                AppEngine.getContext().sendBroadcast(Intent(BaseActivity.RELOAD_SINGLE_ACTION))
            }
        }
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("type", "4"))
        NetManager.getInstance().favoriteMovie(stringBuilder.toString(), params, callback)
    }

    private fun multiShare() {
        if (myAdapter.multiSelectList.size > 15) {
            ToastTool.show("一次最多只能分享15部")
            return
        }
        val beanList = ArrayList<MovieBean>()
        for (i in myAdapter.multiSelectList.indices) {
            val position = myAdapter.multiSelectList[i]
            val bean = myAdapter.getItem(position) ?: continue
            bean.cursorPosition = position
            beanList.add(bean)
        }
        val intent = Intent(context, MultiShareActivity::class.java)
        intent.putParcelableArrayListExtra("list", beanList)
        WWindowManager.getInstance().currentActivity.startActivity(intent)
        ThreadManager.getInstance().postDelayed({ exitMultiMode(true) }, 500)
    }

    private fun multiDelete() {
        val confirmDialog = DialogTool.getConfirmDialog("确认删除", "确定", "取消")
        confirmDialog.confirm_agree.setOnClickListener {
            confirmDialog.dismiss()
            val beanList = ArrayList<MovieBean>()
            val stringBuilder = StringBuilder()
            var isFirst = true
            for (i in myAdapter.multiSelectList.indices) {
                val position = myAdapter.multiSelectList[i]
                val bean = myAdapter.getItem(position) ?: continue
                bean.cursorPosition = position
                beanList.add(bean)
                if (isFirst) {
                    isFirst = false
                } else {
                    stringBuilder.append(",")
                }
                stringBuilder.append(bean.id)
            }
            val callback = object : NetRequestCallBack() {
                override fun onDefault() {
                    DialogTool.dismissWaitDialog()
                }

                override fun onSuccess(result: ArrayMap<Any, Any>) {
                    MovieDbManager.getInstance().delete(beanList)
                    ThreadManager.getInstance().post {
                        exitMultiMode()
                        AppEngine.getContext().sendBroadcast(Intent(BaseActivity.RELOAD_ALL_ACTION))
                    }
                }
            }
            DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
            NetManager.getInstance().deleteMovie(stringBuilder.toString(), callback)
        }
        confirmDialog.confirm_disagree.setOnClickListener { confirmDialog.dismiss() }
        confirmDialog.show()
    }

    private fun handleUpEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            if (isDragging) {
                isDragging = false
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                ThreadManager.getInstance().post(this)
            } else if (scrollView.scrollY != movieHeaderView.measuredHeight) {
                ThreadManager.getInstance().post(this)
            }
        }
    }

    override fun run() {
        Session.isAnimating = true
        scrollView.smoothScrollTo(0, movieHeaderView.measuredHeight)
        NetManager.getInstance().getMovieWord(NetRequestCallBack())
        ThreadManager.getInstance().postDelayed({
            scrollView.scrollTo(0, movieHeaderView.measuredHeight)
            groupText.visibility = VISIBLE
            Session.isAnimating = false
        }, 300)
    }

    private inner class MyScrollView(context: Context) : NestedScrollView(context) {
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (recyclerView.isShown) {
                if (detector.onTouchEvent(ev)) {
                    handleUpEvent(ev)
                    return true
                } else {
                    handleUpEvent(ev)
                    return super.dispatchTouchEvent(ev)
                }
            }
            return true
        }
    }

    private inline fun ViewManager.myScrollView(theme: Int = 0, init: MyScrollView.() -> Unit) = ankoView({ MyScrollView(it) }, theme, init)

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scrollView.scrollY < movieHeaderView.measuredHeight - 2) {
                isDragging = true
                groupText.visibility = INVISIBLE
                if (canUpdateMovieWord) {
                    canUpdateMovieWord = false
                    movieHeaderView.updateMovieWord()
                    recyclerView.isLayoutFrozen = true
                }
                scrollView.scrollBy(0, (distanceY / 2).toInt())
                return true
            } else if (isDragging) {
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                recyclerView.scrollBy(0, distanceY.toInt())
                return true
            }
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (isDragging && velocityY < 0) {
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                recyclerView.fling(0, (-velocityY).toInt())
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            canUpdateMovieWord = true
            return false
        }
    }
}