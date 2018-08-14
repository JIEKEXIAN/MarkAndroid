package com.intlime.mark.activitys

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.intlime.mark.R
import com.intlime.mark.adapter.MovieSourceAdapter
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.JsonTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.tools.ZhugeTool
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.YRecyclerView
import com.intlime.mark.view.recyclerview.yRecyclerView
import com.intlime.mark.view.widget.MovieSourceHeader
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import org.json.JSONObject
import java.util.*

/**
 * Created by root on 16-3-9.
 */
class MovieSourceActivity : BaseActivity() {
    private val movieSourceAdapter: MovieSourceAdapter by lazy {
        MovieSourceAdapter(ArrayList<JSONObject>())
    }
    private lateinit var recyclerView: YRecyclerView
    private var bean: MovieBean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bean = intent.getParcelableExtra(BEAN)
        if (bean == null) {
            finish()
            return
        }
        initRootView()
        initData()
    }

    private fun initRootView() {
        rootView = verticalLayout {
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                title = "播放源"
                setNavigationOnClickListener {
                    finish()
                }
                if (Session.isDebug) {
                    menu.add("修改").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    onMenuItemClick { item ->
                        if (item == null) return@onMenuItemClick true
                        startActivity<WebActivity>("title" to "修改播放源",
                                "url" to "http://mark.intlime.com/MarkAdmin/addVideoResource/movie_id/${bean!!.id}/name/${bean!!.name}")
                        return@onMenuItemClick true
                    }
                }
            }.lparams(matchParent, dip(49))

            recyclerView = yRecyclerView() {
                backgroundColor = resources.getColor(R.color.bg)
                layoutParams = LinearLayout.LayoutParams(matchParent, matchParent)
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = movieSourceAdapter
                setRecyclerItemListener(object : RecyclerItemListener() {
                    init {
                        clickable = true
                    }

                    override fun onItemClick(v: View?, position: Int) {
                        val item = movieSourceAdapter.getItem(position)
                        item ?: return
                        if (item.optBoolean("is_other")) {
                            ZhugeTool.track("点击播放源", ZhugeTool.getTrackArg(
                                    Pair<String, String>("视频名称", JsonTool.optString(item, "name"))))
                        } else {
                            ZhugeTool.track("点击可能免费的播放源", ZhugeTool.getTrackArg(
                                    Pair<String, String>("视频名称", JsonTool.optString(item, "name"))))
                        }
                        val url = JsonTool.optString(item, "url")
                        try {
                            val type = JsonTool.optString(item, "type")
                            val video_id = JsonTool.optString(item, "video_id")
                            if (TextUtils.isEmpty(video_id)) {
                                if ("baiduyun".equals(type)) {
                                    startActivity<WebActivity>("url" to url, "type" to 1)
                                } else {
                                    startActivity<WebActivity>("url" to url, "type" to 3)
                                }
                                return
                            }
                            when (type) {
                                "leshi" -> {
                                    val uri = "letvclient://msiteAction?actionType=9&vid=" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "qq" -> {
                                    val uri = "tenvideo2://?action=1&cover_id=" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "tudou" -> {
                                    val uri = "tudou://itemcode=" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "youku" -> {
                                    val uri = "youku://play?source=mplaypage2&vid=" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "bilibili" -> {
                                    val uri = "bilibili://video/" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "acfun" -> {
                                    val uri = "acfun://detail/video/" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "imgo" -> {
                                    val uri = "imgotv://player?videoId=" + video_id
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "sohu" -> {
                                    val uri = "sohuvideo://action.cmd?action=1.1&sid=$video_id"
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "xunlei" -> {
                                    val uri = "intent://com.xunlei.kankan/movies/1/$video_id/0/%E5%A4%A7%E5%8A%9B%E7%A5%9E/400555?source=wap#Intent;scheme=kankan;package=com.xunlei.kankan;end"
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "qiyi" -> {
                                    val uri = "qiyimobile://self/com_qiyi_video/res.made?identifier=qymobile&aid=$video_id"
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                "pptv" -> {
                                    val uri = "pptv://player?type=ppvod&channel_id=$video_id&playmode=2"
                                    val intent = Intent.parseUri(
                                            uri,
                                            Intent.URI_INTENT_SCHEME)
                                    startActivity(intent);
                                }
                                else -> {
                                    startActivity<WebActivity>("url" to url, "type" to 3)
                                }
                            }
                        } catch(e: Exception) {
                            startActivity<WebActivity>("url" to url, "type" to 3)
                            e.printStackTrace()
                        }
                    }
                })
            }
        }
    }

    private fun initData() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>?) {
                ThreadManager.getInstance().post {
                    val videos: MutableList<JSONObject> = result?.get("videos") as MutableList<JSONObject>
                    if (videos.isNotEmpty()) {
                        movieSourceAdapter.addHeaderView(MovieSourceHeader(AppEngine.getContext(), videos), false)
                    }
                    val sources = result?.get("sources") as MutableList<JSONObject>
                    if (sources.isNotEmpty()) {
                        val layout = FrameLayout(applicationContext).apply {
                            backgroundColor = resources.getColor(R.color.bg)
                            val dark_blue = resources.getColor(R.color.dark_blue)
                            textView {
                                text = "在线观看全片"
                                textColor = dark_blue
                                textSize = 15f
                                setPadding(dip(15.6f), dip(22.5f), 0, dip(6))
                                layoutParams = FrameLayout.LayoutParams(wrapContent, wrapContent).apply {
                                    gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                                }
                            }
                            pressableTextView {
                                text = "反馈"
                                textColor = dark_blue
                                textSize = 15f
                                gravity = Gravity.CENTER
                                backgroundDrawable = GradientDrawable().apply {
                                    setColor(Color.TRANSPARENT)
                                    setCornerRadius(dip(2).toFloat())
                                    setStroke(1, Color.parseColor("#496069"))
                                }
                                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                                layoutParams = FrameLayout.LayoutParams(dip(72), dip(27)).apply {
                                    gravity = Gravity.BOTTOM or Gravity.RIGHT
                                    rightMargin = dip(12)
                                    bottomMargin = dip(3)
                                }
                                onClick {
                                    onFeedbackClick()
                                }
                            }
                        }
                        movieSourceAdapter.addHeaderView(layout)
                    } else {
                        val sourceEmptyView = LinearLayout(AppEngine.getContext()).apply {
                            bottomPadding = dip(25)
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            textView {
                                layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
                                text = "Sorry，这部电影暂时没有播放源"
                                textSize = 14f
                                textColor = resources.getColor(R.color.a_main_text_color)
                                topPadding = dip(30)
                                bottomPadding = dip(20)
                            }
                            pressableTextView {
                                layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
                                text = "给我们反馈"
                                textSize = 14f
                                textColor = resources.getColor(R.color.a_main_text_color)
                                gravity = Gravity.CENTER
                                backgroundDrawable = GradientDrawable().apply {
                                    setColor(Color.WHITE)
                                    setCornerRadius(dip(2.5f).toFloat())
                                    setStroke(1, Color.parseColor("#cfcfcf"))
                                }
                                verticalPadding = dip(12)
                                horizontalPadding = dip(29)
                                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                                onClick {
                                    onFeedbackClick()
                                }
                            }
                        }
                        movieSourceAdapter.addHeaderView(sourceEmptyView, false)
                    }
                    val others = result?.get("others") as MutableList<JSONObject>
                    sources.addAll(others)
                    movieSourceAdapter.lists.addAll(sources)
                    movieSourceAdapter.notifyDataSetChanged()
                }
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getMovieSource(bean!!.id, callback)
    }

    private fun onFeedbackClick() {
        val builder = AlertDialogBuilder(this)
        builder.items(arrayOf<CharSequence>("没有任何资源", "资源链接有误", "共享资源链接"), { which ->
            if (which == 2) {
                startActivity<WebActivity>("url" to "http://mark.intlime.com/videofeedback")
                return@items
            }
            val callback = object : NetRequestCallBack() {
                override fun onDefault() {
                    DialogTool.dismissWaitDialog()
                }

                override fun onSuccess(result: ArrayMap<*, *>?) {
                    ToastTool.show("感谢您的反馈")
                }
            }
            DialogTool.showWaitDialog("反馈中", DialogTool.CANCEL_ON_BACK, callback)
            NetManager.getInstance().sourceFeedback(bean!!.id, bean!!.name, which + 1, callback)
        })
        builder.show()
    }
}