package com.intlime.mark.activitys

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.adapter.RelateMovieListAdapter
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.bean.SingleAccessBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.*
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.DividerItemDecoration
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.YRecyclerView
import com.intlime.mark.view.widget.MovieStatusView
import com.intlime.mark.view.widget.RatingBar
import com.intlime.mark.view.widget.RatingView
import com.tencent.smtt.sdk.TbsVideo
import com.tendcloud.tenddata.TCAgent
import com.wtuadn.pressable.PressableTextView
import com.wtuadn.pressable.PressableUtils
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.dip
import org.jetbrains.anko.startActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by root on 15-11-4.
 */
class MovieDetailActivity : BaseActivity(), RatingBar.OnRatingBarChangeListener {
    private var single_id = -1
    private var type: Int = 0//0为本地有的,1为本地没有的
    private var bean: MovieBean? = null

    private var added_icon: View? = null
    private var button2: ImageView? = null
    private var button_layout_line: View? = null
    private var button1: TextView? = null

    private var ratingView: RatingView? = null
    private var ratingBar: RatingBar? = null
    private var pubdates: TextView? = null
    private var duration: TextView? = null
    private var movieType: TextView? = null
    private var directors: TextView? = null
    private var writers: TextView? = null
    private var casts: TextView? = null
    private var summary: TextView? = null

    private var imageView_1: ImageView? = null
    private var imageView_2: ImageView? = null
    private val stagePhotos = ArrayList<Drawable>()
    private var playingPosition = 0

    private var watchTime: TextView? = null
    private var editWatchTime: View? = null
    private var noteText: TextView? = null

    private var isVisiable: Boolean = false

    private var broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (BaseActivity.RELOAD_ALL_ACTION == action) {
                    val temp = intent.getParcelableExtra<MovieBean>(BaseActivity.BEAN)
                    if (temp != null) {
                        bean = temp
                        val type = intent.getIntExtra("type", -1)
                        if (type >= 0) {
                            this@MovieDetailActivity.type = type
                        }
                        updateButton()
                        updateRecord()
                        ratingBar!!.setRating((bean!!.mark_rating / 2).toInt())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            type = intent.getIntExtra("type", 0)
            bean = intent.getParcelableExtra<MovieBean>(BaseActivity.BEAN)
            if (bean == null) {
                finish()
                return
            }
            if (bean!!.id > 0) {
                type = 0
            }
            ZhugeTool.track("进入电影详情", ZhugeTool.getTrackArg(Pair<String, String>("电影名称", bean!!.name!!)))
        } else {
            type = savedInstanceState.getInt("type", 0)
            bean = savedInstanceState.getParcelable<MovieBean>(BaseActivity.BEAN)
        }
        if (bean == null) {
            finish()
            return
        }
        single_id = intent.getIntExtra("single_id", -1)
        setContentView(R.layout.activity_movie_detail_layout)
        val filter = IntentFilter()
        filter.addAction(BaseActivity.RELOAD_ALL_ACTION)
        registerReceiver(broadcastReceiver, filter)
        handleShareHint()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("type", type)
        outState.putParcelable(BaseActivity.BEAN, bean)
    }

    private fun handleShareHint() {
        if (SettingManager.getInstance().canShowMovieDetailShareHint()) {
            SettingManager.getInstance().setCanShowMovieDetailShareHint(false)
            val dialog = Dialog(this, R.style.mydialog)
            val imageView = ImageView(AppEngine.getContext())
            imageView.setImageResource(R.drawable.movie_detail_share_hint)
            imageView.setOnClickListener { dialog.dismiss() }
            dialog.setContentView(imageView)
            val window = dialog.window
            val lp = window.attributes
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.gravity = Gravity.TOP or Gravity.RIGHT
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.x = DensityUtils.dp2px(this, 3f)
            lp.y = DensityUtils.dp2px(this, 44f)
            lp.dimAmount = 0f
            window.attributes = lp
            dialog.show()
            ThreadManager.getInstance().postDelayed({
                try {
                    if (dialog.isShowing) dialog.dismiss()
                } catch (ignore: Exception) {
                }
            }, 5000)
        }
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = bean!!.name
        toolbar.setNavigationIcon(R.drawable.back_icon)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.menu.add("卡片制作").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        toolbar.setOnMenuItemClickListener {
            val intent = Intent(AppEngine.getContext(), MovieCardShareActivity::class.java)
            intent.putExtra(BaseActivity.BEAN, bean)
            startActivity(intent)
            true
        }
    }

    override fun initOther() {
        initImage()
        initDetail()
        initButton()
        initRecord()
    }

    private fun initImage() {
        imageView_1 = findViewById(R.id.image_view1) as ImageView?
        imageView_2 = findViewById(R.id.image_view2) as ImageView?
        imageView_1!!.setBackgroundColor(Color.parseColor("#e1e1e1"))
    }

    private fun loadStagePhotos() {
        rootView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            private var validCount: Int = 0
            private var isPlaying: Boolean = false

            override fun onPreDraw(): Boolean {
                rootView.viewTreeObserver.removeOnPreDrawListener(this)
                val urls = bean!!.stagePhoto
                validCount = urls!!.size
                if (validCount == 0) {
                    playImage(true)
                    return true
                }
                var width = imageView_1!!.measuredWidth
                var height = imageView_1!!.measuredHeight
                if (width <= 0 || height <= 0) {
                    width = Target.SIZE_ORIGINAL
                    height = Target.SIZE_ORIGINAL
                }
                for (i in 0..validCount - 1) {
                    val url = urls[i]
                    Glide.with(this@MovieDetailActivity).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE).override(width, height).into(object : SimpleTarget<GlideDrawable>() {
                        override fun onLoadFailed(e: Exception?, errorDrawable: Drawable?) {
                            validCount--
                            if (stagePhotos.size == validCount) {
                                if (!isPlaying) {
                                    isPlaying = true
                                    playImage(true)
                                }
                            }
                        }

                        override fun onResourceReady(resource: GlideDrawable, glideAnimation: GlideAnimation<in GlideDrawable>) {
                            stagePhotos.add(resource)
                            if (stagePhotos.size == 1) {
                                imageView_1!!.setImageDrawable(resource)
                            }
                            if (stagePhotos.size == validCount || stagePhotos.size == 2 && validCount > 2) {
                                if (!isPlaying) {
                                    isPlaying = true
                                    playImage(true)
                                }
                            }
                        }
                    })
                }
                return true
            }
        })
    }

    private fun initButton() {
        added_icon = findViewById(R.id.added_icon)
        button2 = findViewById(R.id.button2) as ImageView?
        button1 = findViewById(R.id.button1) as TextView?
        button_layout_line = findViewById(R.id.button_layout_line)
        updateButton()
    }

    private fun updateButton() {
        if (type == 1) {
            val lp = button1!!.layoutParams as RelativeLayout.LayoutParams
            lp.addRule(RelativeLayout.LEFT_OF, R.id.button_layout_line)
            button1!!.layoutParams = lp
            added_icon!!.visibility = View.GONE
            button_layout_line!!.visibility = View.VISIBLE
            button1!!.text = "想看"
            button1!!.setTextColor(Color.WHITE)
            button2!!.setImageResource(R.drawable.detail_button2_icon_toadd)
            (button2!!.parent as View).setBackgroundResource(R.drawable.detail_button_bg_toadd)
        } else {
            val lp = button1!!.layoutParams as RelativeLayout.LayoutParams
            lp.addRule(RelativeLayout.LEFT_OF, 0)
            button1!!.layoutParams = lp
            if (bean!!.done == 1) {
                button1!!.text = "已 看"
            } else {
                button1!!.text = "想 看"
            }
            button1!!.setTextColor(MResource.getColor(R.color.a_main_text_color))
            added_icon!!.visibility = View.VISIBLE
            button_layout_line!!.visibility = View.GONE
            button2!!.setImageResource(R.drawable.detail_button2_icon_added)
            (button2!!.parent as View).setBackgroundResource(R.drawable.detail_button_bg_added)
        }
    }

    private fun initRecord() {
        watchTime = findViewById(R.id.watch_time) as TextView?
        editWatchTime = findViewById(R.id.edit_watch_time)
        noteText = findViewById(R.id.note) as TextView?
        updateRecord()
    }

    private fun updateRecord() {
        if (type == 1) {
            findViewById(R.id.record_layout)!!.visibility = View.GONE
        } else {
            findViewById(R.id.record_layout)!!.visibility = View.VISIBLE
            setNoteText()
        }
        setWatchTime()
    }

    private fun setWatchTime() {
        if (bean!!.watchTime == 0L) {
            watchTime!!.text = "暂无"
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = Date(bean!!.watchTime * 1000L)
            watchTime!!.text = formatter.format(date)
        }
        if (bean!!.done == 1) {
            editWatchTime!!.visibility = View.VISIBLE
        } else {
            editWatchTime!!.visibility = View.GONE
        }
    }

    private fun setNoteText() {
        if (TextUtils.isEmpty(bean!!.note)) {
            noteText!!.setTextColor(Color.parseColor("#818c91"))
            noteText!!.text = "记录看电影的时光..."
        } else {
            noteText!!.setTextColor(Color.parseColor("#262626"))
            noteText!!.text = bean!!.note
        }
    }

    private fun playImage(setHolder: Boolean) {
        if (!stagePhotos.isEmpty()) {
            imageView_1!!.setImageDrawable(stagePhotos[playingPosition])
            if (stagePhotos.size > 1) {
                switchView(imageView_1!!, imageView_2!!)
            }
        } else if (setHolder) {
            imageView_1!!.setBackgroundDrawable(EmptyDrawable(104f, 104f))
        }
    }

    private var lastSwitchTime: Long = 0

    private fun switchView(inView: ImageView, outView: ImageView) {
        if (broadcastReceiver == null || System.currentTimeMillis() - lastSwitchTime < 1000) return
        inView.alpha = 1f
        inView.scaleX = 1f
        inView.scaleY = 1f
        inView.animate().scaleX(1.05f).scaleY(1.05f).setInterpolator(LinearInterpolator()).setDuration(6000).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                synchronized(stagePhotos) {
                    if (!isVisiable) return
                    playingPosition++
                    if (playingPosition == stagePhotos.size) {
                        playingPosition = 0
                    }
                    (rootView as ViewGroup).removeView(outView)
                    (rootView as ViewGroup).addView(outView, 0)
                    outView.setImageDrawable(stagePhotos[playingPosition])
                    switchView(outView, inView)
                }
            }
        }).start()
        outView.animate().alpha(0f).setInterpolator(LinearInterpolator()).setDuration(2000).setListener(null).start()
        lastSwitchTime = System.currentTimeMillis()
    }

    private fun initDetail() {
        ratingView = findViewById(R.id.rating_view) as RatingView?
        ratingBar = findViewById(R.id.rating_bar) as RatingBar?
        pubdates = findViewById(R.id.pubdate) as TextView?
        duration = findViewById(R.id.duration) as TextView?
        movieType = findViewById(R.id.movie_type) as TextView?
        directors = findViewById(R.id.directors) as TextView?
        writers = findViewById(R.id.writers) as TextView?
        casts = findViewById(R.id.casts) as TextView?
        summary = findViewById(R.id.summary) as TextView?
        ratingBar!!.setOnRatingBarChangeListener(this)

        val callBack = object : NetRequestCallBack() {
            override fun onSuccess(result: ArrayMap<*, *>) {
                val movieBean = result["movie"] as MovieBean
                if (movieBean != null) {
                    bean!!.id = movieBean.id
                    bean!!.image = movieBean.image
                    bean!!.directors = movieBean.directors
                    bean!!.casts = movieBean.casts
                    bean!!.movieType = movieBean.movieType
                    bean!!.countries = movieBean.countries
                    bean!!.pubdate = movieBean.pubdate
                    bean!!.summary = movieBean.summary
                    bean!!.urls = movieBean.urls
                    bean!!.db_rating = movieBean.db_rating
                    bean!!.duration = movieBean.duration
                    bean!!.stagePhoto = movieBean.stagePhoto
                    bean!!.scriptWriter = movieBean.scriptWriter
                    bean!!.trailer = movieBean.trailer
                    bean!!.pubdateTimestamp = movieBean.pubdateTimestamp
                    loadStagePhotos()
                    ThreadManager.getInstance().post {
                        setContents()
                        setMovieLists(result)
                        DialogTool.dismissWaitDialog()
                    }
                    if (type == 0)
                        MovieDbManager.getInstance().update(bean)
                }
            }

            override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                DialogTool.dismissWaitDialog()
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callBack)
        var rate: String? = null
        if (bean!!.db_rating > 0) {
            rate = java.lang.Float.toString(bean!!.db_rating)
        }
        NetManager.getInstance().getMovieDetail(bean!!.db_num, bean!!.image, rate, callBack)
    }

    private fun setContents() {
        ratingView!!.rate = bean!!.db_rating
        if (!TextUtils.isEmpty(bean!!.pubdate)) {
            pubdates!!.text = bean!!.pubdate
            StringTool.handleLinesToExpand(pubdates, 2)
        } else {
            pubdates!!.text = "-"
        }
        if (!TextUtils.isEmpty(bean!!.duration)) {
            duration!!.text = bean!!.duration
        } else {
            duration!!.text = "-"
        }
        val movieTypes = bean!!.movieType!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (movieTypes.size == 0 || TextUtils.isEmpty(bean!!.movieType)) {
            movieType!!.text = "-"
        } else {
            for (i in movieTypes.indices) {
                val tag = movieTypes[i]
                if (!TextUtils.isEmpty(tag)) {
                    if (i != 0) {
                        movieType!!.append(" | ")
                    }
                    movieType!!.append(tag)
                }
            }
            StringTool.handleLinesToExpand(movieType, 2)
        }
        if (!TextUtils.isEmpty(bean!!.directors)) {
            directors!!.text = bean!!.directors
            StringTool.handleLinesToExpand(directors, 2)
        } else {
            directors!!.text = "-"
        }
        if (!TextUtils.isEmpty(bean!!.scriptWriter)) {
            writers!!.text = bean!!.scriptWriter
            StringTool.handleLinesToExpand(writers, 2)
        } else {
            writers!!.text = "-"
        }
        if (!TextUtils.isEmpty(bean!!.casts)) {
            casts!!.text = bean!!.casts
            StringTool.handleLinesToExpand(casts, 2)
        } else {
            casts!!.text = "-"
        }
        if (!TextUtils.isEmpty(bean!!.summary)) {
            summary!!.text = bean!!.summary
            StringTool.handleLinesToExpand(summary, 7)
        } else {
            summary!!.visibility = View.GONE
        }
        ratingBar!!.setRating((bean!!.mark_rating / 2).toInt())

        if (!TextUtils.isEmpty(bean!!.trailer)) {
            findViewById(R.id.play_movie)!!.visibility = View.VISIBLE
        }
        val urlLayout = findViewById(R.id.url_layout) as LinearLayout?
        if (bean!!.urls == null || bean!!.urls!!.isEmpty) {
            urlLayout!!.visibility = View.GONE
        } else {
            val listener = View.OnClickListener { v ->
                if (bean != null && bean!!.urls != null && !bean!!.urls!!.isEmpty) {
                    val name = (v as TextView).text.toString()
                    val intent = Intent(this@MovieDetailActivity, WebActivity::class.java)
                    intent.putExtra("url", bean!!.urls!![name])
                    startActivity(intent)
                    if ("豆瓣" == name) {
                        ZhugeTool.track("点击豆瓣链接", null)
                    } else {
                        ZhugeTool.track("点击IMDb", null)
                    }
                }
            }
            for (i in 0..bean!!.urls!!.size - 1) {
                val name = bean!!.urls!!.keyAt(i)
                val textView = PressableTextView(this)
                textView.text = name
                textView.setTextColor(MResource.getColor(R.color.a_main_text_color))
                textView.textSize = 13f
                textView.gravity = Gravity.CENTER
                textView.setBackgroundResource(R.drawable.movie_detail_url_button_bg)
                val lp = LinearLayout.LayoutParams(
                        DensityUtils.dp2px(this, 156.5f), DensityUtils.dp2px(this, 39.5f))
                PressableUtils.setPressableDrawable(textView, MResource.getColor(R.color.black_pressed_color))
                textView.setOnClickListener(listener)
                if (i != 0) {
                    lp.leftMargin = DensityUtils.dp2px(this, 20f)
                }
                if (bean!!.urls!!.size > 1) {
                    lp.weight = 1f
                }
                urlLayout!!.addView(textView, lp)
            }
        }
    }

    private fun setMovieLists(result: ArrayMap<*, *>) {
        if (result.containsKey("movieLists")) {
            val list = result["movieLists"] as List<MovieListBean>
            if (list.size > 0) {
                val vg = findViewById(R.id.movielists_layout)
                vg!!.visibility = View.VISIBLE
                val recyclerView = vg.findViewById(R.id.movielists_recycler) as YRecyclerView
                recyclerView.apply {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    addItemDecoration(DividerItemDecoration(Color.TRANSPARENT, dip(7.2f), LinearLayoutManager.HORIZONTAL))
                    val myAdapter = RelateMovieListAdapter(list)
                    adapter = myAdapter
                    setRecyclerItemListener(object : RecyclerItemListener() {
                        init {
                            clickable = true
                        }

                        override fun onItemClick(v: View?, position: Int) {
                            val bean = myAdapter.getItem(position) ?: return
                            startActivity<MovieListDetailActivity>(BEAN to bean)
                        }
                    })
                }
            }
        }
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.button1 -> if (type == 1) {
                addMovie(false, null)
            } else {
                openMovieStatusView()
            }
            R.id.button2 -> openMovieStatusView()
            R.id.edit_watch_time -> chooseWatchTime()
            R.id.note -> {
                var intent = Intent(applicationContext, MovieNoteActivity::class.java)
                intent.putExtra(BaseActivity.BEAN, bean)
                startActivity(intent)
            }
            R.id.play_movie -> {
                TbsVideo.openVideo(this, bean!!.trailer)
                ZhugeTool.track("点击预告", null)
            }
            R.id.movie_source_play -> {
                intent = Intent(applicationContext, MovieSourceActivity::class.java)
                intent.putExtra(BaseActivity.BEAN, bean)
                startActivity(intent)
                ZhugeTool.track("点击播放", null)
            }
        }
    }

    private fun openMovieStatusView() {
        if (single_id > -1) {
            MovieStatusView.show(bean, type, null, object : MovieStatusView.OnDeleteSingleClickListener {
                override fun onDeleteSingleClicked(v: View) {
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<*, *>) {
                            MovieSingleDbManager.deleteAccess(SingleAccessBean(single_id, bean!!.id, 0))
                            sendBroadcast(Intent(BaseActivity.RELOAD_SINGLE_ACTION))
                            single_id = -1
                        }
                    }
                    DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("db_num", bean!!.db_num))
                    params.add(BasicNameValuePair("is_done", Integer.toString(bean!!.done)))
                    params.add(BasicNameValuePair("add_ids", CryptTool.encrypt("")))
                    params.add(BasicNameValuePair("delete_ids", CryptTool.encrypt(Integer.toString(single_id))))
                    NetManager.getInstance().addSingleAccess(params, callback)
                }
            }, null)
        } else {
            MovieStatusView.show(bean, type, null, null, null)
        }
    }

    private fun addMovie(isDone: Boolean, callBack: NetRequestCallBack?) {
        val callBack1 = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                bean!!.done = if (isDone) 1 else 0
                type = 0
                val temp = result["bean"] as MovieBean
                bean!!.id = temp.id
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
                ThreadManager.getInstance().post {
                    updateButton()
                    updateRecord()
                }
                MovieDbManager.getInstance().insert(bean)
                sendBroadcast(Intent(BaseActivity.RELOAD_ALL_ACTION))
                callBack?.onSuccess(null)
            }

            override fun onFail(result: ArrayMap<*, *>, error_code: Int) {
                callBack?.onFail(null, 0)
            }
        }
        DialogTool.showWaitDialog("添加中", DialogTool.CANCEL_ON_BACK, callBack1)
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("db_num", bean!!.db_num))
        params.add(BasicNameValuePair("is_done", if (isDone) "1" else "0"))
        params.add(BasicNameValuePair("dbrating",
                CryptTool.base64Encode(CryptTool.encrypt(java.lang.Float.toString(bean!!.db_rating)))))
        NetManager.getInstance().addMovie(params, callBack1)
    }

    private fun chooseWatchTime() {
        val calendar = Calendar.getInstance(Locale.CHINA)
        val dialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            if (!view.isShown) return@OnDateSetListener
            val callback = object : NetRequestCallBack() {
                override fun onDefault() {
                    DialogTool.dismissWaitDialog()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    bean!!.watchTime = GregorianCalendar(year, monthOfYear, dayOfMonth).timeInMillis / 1000
                    MovieDbManager.getInstance().update(bean)
                    sendBroadcast(Intent(BaseActivity.RELOAD_ALL_ACTION))
                    ThreadManager.getInstance().post { setWatchTime() }
                }
            }
            DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback)
            val watchTime = String.format("%4d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            val params = ArrayList<NameValuePair>()
            params.add(BasicNameValuePair("watchdate", watchTime))
            NetManager.getInstance().movieWatchTime(bean!!.id, params, callback)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun onRatingChanged(ratingBar: RatingBar, rating: Int) {
        if (rating <= 0) {
            ratingBar.setRating((bean!!.mark_rating / 2).toInt())
            return
        }
        val trulyRating = rating * 2
        if (trulyRating == bean!!.mark_rating.toInt()) return
        if (type == 1) {
            addMovie(true, object : NetRequestCallBack() {
                override fun onSuccess(result: ArrayMap<*, *>) {
                    rate(ratingBar, trulyRating)
                }

                override fun onFail(result: ArrayMap<*, *>, error_code: Int) {
                    ThreadManager.getInstance().post { ratingBar.setRating((bean!!.mark_rating / 2).toInt()) }
                }
            })
        } else {
            rate(ratingBar, trulyRating)
        }
    }

    private fun rate(ratingBar: RatingBar, trulyRating: Int) {
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("rating", Integer.toString(trulyRating)))
        NetManager.getInstance().rateMovie(bean!!.id, params, object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                if (bean!!.done == 0) {
                    bean!!.done = 1
                    bean!!.update_time = result["time"] as Long
                    updateWatchTime()
                }
                bean!!.mark_rating = trulyRating.toFloat()
                ThreadManager.getInstance().post {
                    updateButton()
                    updateRecord()
                    ratingBar.setRating((bean!!.mark_rating / 2).toInt())
                }
                MovieDbManager.getInstance().update(bean)
                sendBroadcast(Intent(BaseActivity.RELOAD_ALL_ACTION))
            }

            override fun onFail(result: ArrayMap<*, *>, error_code: Int) {
                ThreadManager.getInstance().post { ratingBar.setRating((bean!!.mark_rating / 2).toInt()) }
            }
        })
    }

    private fun updateWatchTime() {
        if (bean!!.done == 1) {
            bean!!.watchTime = bean!!.update_time
        } else {
            bean!!.watchTime = 0
        }
    }

    override fun onResume() {
        super.onResume()
        TCAgent.onPageStart(this, "电影详情")
    }

    override fun onPause() {
        super.onPause()
        TCAgent.onPageEnd(this, "电影详情")
    }

    override fun onStart() {
        super.onStart()
        isVisiable = true
        playImage(false)
    }

    override fun onStop() {
        super.onStop()
        isVisiable = false
        imageView_1!!.animate().cancel()
        imageView_2!!.animate().cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            broadcastReceiver = null
        }
    }
}
