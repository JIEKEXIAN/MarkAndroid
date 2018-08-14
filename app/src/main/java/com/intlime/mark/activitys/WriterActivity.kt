package com.intlime.mark.activitys

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.bean.WriterBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.tools.glide.CircleTransform
import com.intlime.mark.view.recyclerview.DividerItemDecoration
import com.intlime.mark.view.recyclerview.LoadRecyclerView
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.wtuadn.pressable.PressableRelativeLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by root on 16-1-22.
 */
class WriterActivity : BaseActivity(), LoadRecyclerView.OnLoadListener {
    private val limit = 10
    private lateinit var lrv: LoadRecyclerView
    private lateinit var myAdapter: ArticleAdapter
    private lateinit var nickname: TextView
    private lateinit var count_text: TextView
    private lateinit var saying: TextView
    private lateinit var head: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUI()
        applyData()
    }

    private fun applyUI() {
        apply {
            rootView = relativeLayout {
                backgroundResource = R.color.bg
                lparams(matchParent, matchParent)
                toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                    navigationIconResource = R.drawable.back_icon
                    setNavigationOnClickListener { finish() }
                    title = "${SettingManager.getInstance().nickname}"
                }.lparams(matchParent, dip(49))
                lrv = LoadRecyclerView(context).apply {
                    backgroundColor = Color.parseColor("#ebebeb")
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(DividerItemDecoration(Color.parseColor("#e1e1e1"), dip(1), LinearLayoutManager.VERTICAL))
                    myAdapter = ArticleAdapter(arrayListOf())
                    adapter = myAdapter
                    setRecyclerItemListener(MyRecyclerItemListener())
                    setLoadListener(this@WriterActivity)
                    val header1 = View.inflate(context, R.layout.writer_header_layout, null)
                    val header2 = View.inflate(context, R.layout.writer_header2_layout, null)
                    myAdapter.addHeaderView(header1)
                    myAdapter.addHeaderView(header2)
                    nickname = header1.find(R.id.nickname)
                    head = header1.find(R.id.head)
                    saying = header1.find(R.id.saying)
                    count_text = header1.find(R.id.count_text)
                    updateHeader(header1)
                }.lparams(matchParent, matchParent) {
                    below(toolbar)
                }
                addView(lrv)
            }
        }
    }

    fun updateHeader(header: View) {
        val nicknameStr = SettingManager.getInstance().nickname
        val headUrl = SettingManager.getInstance().userHeadImgUrl
        setHeadAndNickname(headUrl, nicknameStr)
        header.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                header.viewTreeObserver.removeOnPreDrawListener(this)
                try {
                    val width = 800
                    val top = 575
                    val sWidth = WWindowManager.getInstance().width
                    val scale = sWidth.toFloat() / header.measuredHeight
                    val height = (width / scale).toInt()
                    val inputStream = resources.openRawResource(R.drawable.splash_bg)
                    val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    options.inSampleSize = 2
                    val drawable = BitmapDrawable(
                            regionDecoder.decodeRegion(Rect(0, top, width, top + height), options))
                    drawable.setColorFilter(Color.parseColor("#f2496069"), PorterDuff.Mode.SRC_ATOP)
                    header.setBackgroundDrawable(drawable)
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        })
    }

    private fun setHeadAndNickname(headUrl: String, nicknameStr: String) {
        nickname.text = nicknameStr
        if (TextUtils.isEmpty(headUrl)) {
            head.setImageResource(R.drawable.setting_header_icon)
        } else {
            Glide.with(this)
                    .load(headUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transform(CircleTransform(this))
                    .error(R.drawable.setting_header_icon)
                    .into(head)
        }
    }

    private fun updateWriterInfo(writer: WriterBean) {
        setHeadAndNickname(writer.imgUrl, writer.nickname)
        saying.text = writer.saying
        val singleCount = writer.singleCount.toString()
        val singleLikes = writer.singleLikes.toString()
        val t = "写了 $singleCount 篇影单，获得 $singleLikes 个喜欢。"
        val scStart = t.indexOf(singleCount)
        val slStart = t.indexOf(singleLikes, scStart + 1)
        val ss = SpannableString(t)
        ss.setSpan(Span(), scStart, scStart + singleCount.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(Span(), slStart, slStart + singleLikes.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        count_text.text = ss
    }

    private inner class Span : CharacterStyle(), UpdateAppearance {
        override fun updateDrawState(tp: TextPaint?) {
            tp ?: return
            tp.textSize = sp(19).toFloat()
            tp.color = Color.parseColor("#ade6fc")
        }
    }

    private fun applyData() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val writer = result["writer"] as WriterBean
                val list = result["list"] as ArrayList<MovieListBean>
                ThreadManager.getInstance().post {
                    updateWriterInfo(writer)
                    if (list.size < limit) {
                        lrv.isCanLoad = false
                    } else {
                        lrv.isCanLoad = true
                    }
                    if(list.isEmpty()){
                        val emptyView = TextView(applicationContext).apply{
                            text = "还没有文章哦，赶快投稿吧～"
                            textSize = 14f
                            textColor = Color.parseColor("#818c91")
                            gravity = Gravity.CENTER_HORIZONTAL
                            topPadding = dip(70)
                            compoundDrawablePadding = dip(28.4f)
                            setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.writer_article_empty_icon,0,0)
                        }
                        myAdapter.addFooterView(emptyView)
                    }else{
                        myAdapter.lists.addAll(list)
                        myAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getWriterInfo(0, callback)
    }

    override fun onLoad() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                lrv.loadFinish()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["list"] as ArrayList<MovieListBean>
                ThreadManager.getInstance().post {
                    if (list.size < limit) {
                        lrv.isCanLoad = false
                    } else {
                        lrv.isCanLoad = true
                    }
                    if (list.size > 0) {
                        val offset = myAdapter.lists.size
                        myAdapter.lists.addAll(list)
                        myAdapter.notifyItemRangeInserted(offset, list.size)
                    }
                }
            }
        }
        NetManager.getInstance().getLikedMovieCards(myAdapter.lists.size, callback)
    }

    private inner class MyRecyclerItemListener : RecyclerItemListener() {
        init {
            clickable = true
        }

        override fun onItemClick(v: View?, position: Int) {
            val bean = myAdapter.getItem(position) ?: return
            startActivity<MovieListDetailActivity>(BEAN to bean)
        }
    }

    private inner class ArticleAdapter(lists: MutableList<MovieListBean>) : RecyclerListAdapter<MovieListBean>(lists) {

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                val bean = getItem(position) ?: return
                Glide.with(viewHolder.imageView.context)
                        .load(bean.img_url)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .into(viewHolder.imageView)
                viewHolder.nameView.text = bean.name
                viewHolder.likesView.text = bean.likes.toString()
                viewHolder.commentsView.text = bean.comments.toString()

                val format = SimpleDateFormat("yyyy.MM.dd")//设置日期格式
                format.timeZone = TimeZone.getTimeZone("GMT+8")
                viewHolder.dateView.text = format.format(Date(bean.publish_time * 1000L))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            var viewHolder = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                viewHolder = Holder(PressableRelativeLayout(this@WriterActivity))
            }
            return viewHolder
        }

        private inner class Holder(view: PressableRelativeLayout) : RecyclerView.ViewHolder(view) {
            lateinit var imageView: ImageView
            lateinit var nameView: TextView
            lateinit var likesView: TextView
            lateinit var commentsView: TextView
            lateinit var dateView: TextView

            init {
                view.apply {
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    verticalPadding = dip(15)
                    layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                    imageView = MyImgView(context)
                    imageView.id = 46
                    imageView.lparams(dip(110.3f), dip(59)) {
                        centerVertically()
                        leftMargin = dip(15)
                    }
                    addView(imageView)
                    nameView = textView {
                        textSize = 15f
                        textColor = Color.parseColor("#10181e")
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                    }.lparams(matchParent, wrapContent) {
                        rightOf(imageView)
                        horizontalMargin = dip(12.5f)
                    }
                    linearLayout {
                        gravity = Gravity.CENTER_VERTICAL
                        likesView = textView {
                            textSize = 13f
                            textColor = Color.parseColor("#818c91")
                            gravity = Gravity.BOTTOM
                            compoundDrawablePadding = dip(4.5f)
                            val icon = resources.getDrawable(R.drawable.writer_article_like_icon)
                            icon.setBounds(0, 0, dip(14), dip(13))
                            setCompoundDrawables(icon, null, null, null)
                        }.lparams(0, wrapContent) {
                            weight = 3f
                        }
                        commentsView = textView {
                            textSize = 13f
                            textColor = Color.parseColor("#818c91")
                            gravity = Gravity.BOTTOM
                            compoundDrawablePadding = dip(4.5f)
                            val icon = resources.getDrawable(R.drawable.movie_list_comment_icon)
                            icon.setBounds(0, 0, dip(14), dip(13))
                            setCompoundDrawables(icon, null, null, null)
                        }.lparams(0, wrapContent) {
                            weight = 3f
                        }
                        dateView = textView {
                            textSize = 13f
                            textColor = Color.parseColor("#818c91")
                        }.lparams(0, wrapContent) {
                            weight = 4f
                        }
                    }.lparams(matchParent, wrapContent) {
                        alignParentBottom()
                        rightOf(imageView)
                        horizontalMargin = dip(12.5f)
                    }
                }
            }
        }

        private inner class MyImgView(context: Context) : ImageView(context) {
            private val path by lazy {
                ImageTool.getRoundedRectPath(0f, 0f,
                        measuredWidth.toFloat(), measuredHeight.toFloat(), dip(2).toFloat(), dip(2).toFloat(), false)
            }
            private val paint = Paint()

            init {
                paint.isAntiAlias = true
                setLayerType(LAYER_TYPE_SOFTWARE, null)
            }

            override fun onDraw(canvas: Canvas?) {
                if (canvas != null && drawable is BitmapDrawable) {
                    paint.xfermode = null
                    canvas.drawPath(path, paint)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    canvas.drawBitmap((drawable as BitmapDrawable).bitmap, 0f, 0f, paint)
                }
            }
        }
    }
}
