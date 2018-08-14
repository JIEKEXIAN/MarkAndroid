package com.intlime.mark.activitys

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieCardBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.tools.StringTool
import com.intlime.mark.view.recyclerview.LoadRecyclerView
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.wtuadn.pressable.PressableLinearLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import java.util.*

/**
 * Created by root on 16-1-22.
 */
class MoreCardsActivity : BaseActivity(), LoadRecyclerView.OnLoadListener {
    private val limit = 10
    private lateinit var lrv: LoadRecyclerView
    private lateinit var myAdapter: MoreCardsAdapter

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
                    title = "更多电影卡片"
                }.lparams(matchParent, dip(49))
                lrv = LoadRecyclerView(context).apply {
                    backgroundColor = resources.getColor(R.color.bg)
                    setHasFixedSize(true)
                    val glm = GridLayoutManager(context, 2)
                    layoutManager = glm
                    glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            if (adapter.getItemViewType(position) != 0)
                                return glm.spanCount
                            else
                                return 1
                        }
                    }
                    addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                            view ?: return
                            outRect ?: return
                            val params = view.layoutParams as GridLayoutManager.LayoutParams
                            val position = params.viewLayoutPosition
                            outRect.set(0, 0, 0, 0)
                            if (position < adapter.itemCount - 1) {
                                outRect.top = dip(7)
                                if (position % 2 == 0) {
                                    outRect.left = dip(10)
                                    outRect.right = dip(4)
                                } else {
                                    outRect.left = dip(4)
                                    outRect.right = dip(10)
                                }
                            }
                        }
                    })
                    myAdapter = MoreCardsAdapter(arrayListOf())
                    adapter = myAdapter
                    setRecyclerItemListener(MyRecyclerItemListener())
                    setLoadListener(this@MoreCardsActivity)
                }.lparams(matchParent, matchParent) {
                    below(toolbar)
                }
                addView(lrv)
            }
        }
    }

    private fun applyData() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["list"] as ArrayList<MovieCardBean>
                ThreadManager.getInstance().post {
                    if (list.size < limit) {
                        lrv.isCanLoad = false
                    } else {
                        lrv.isCanLoad = true
                    }
                    myAdapter.lists.addAll(list)
                    myAdapter.notifyDataSetChanged()
                }
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getMoreMovieCards(0, callback)
    }

    override fun onLoad() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                lrv.loadFinish()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["list"] as ArrayList<MovieCardBean>
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
        NetManager.getInstance().getMoreMovieCards(myAdapter.lists.size, callback)
    }

    private inner class MyRecyclerItemListener : RecyclerItemListener() {
        init {
            clickable = true
        }

        override fun onItemClick(v: View?, position: Int) {
            val bean = myAdapter.getItem(position) ?: return
            startActivity<CardDetailActivity>(BEAN to bean)
        }
    }

    private inner class MoreCardsAdapter(lists: MutableList<MovieCardBean>) : RecyclerListAdapter<MovieCardBean>(lists) {

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                val bean = getItem(position) ?: return
                val imgUrl = StringTool.getQiniuScaledImgUrl(bean.imgUrl, viewHolder.imgWidth, viewHolder.imgHeight)
                Glide.with(viewHolder.imageView.context)
                        .load(imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .into(viewHolder.imageView)
                viewHolder.contentView.text = bean.content
                viewHolder.nameView.text = "━━《${bean.name}》"
                viewHolder.nameView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    private var offset = bean.name.length - 1

                    override fun onPreDraw(): Boolean {
                        if (viewHolder.nameView.lineCount > 1) {
                            viewHolder.nameView.text = "━━《${bean.name.substring(0, offset)}...》"
                            offset--
                            return false
                        }
                        viewHolder.nameView.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            var viewHolder = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                viewHolder = Holder(PressableLinearLayout(this@MoreCardsActivity))
            }
            return viewHolder
        }

        private inner class Holder(view: PressableLinearLayout) : RecyclerView.ViewHolder(view) {
            lateinit var imageView: ImageView
            lateinit var contentView: TextView
            lateinit var nameView: TextView
            var imgWidth = 0
            var imgHeight = 0

            init {
                view.apply {
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    orientation = LinearLayout.VERTICAL
                    backgroundResource = R.drawable.liked_card_shadow
                    padding = dip(2)
                    imgWidth = (WWindowManager.getInstance().width - dip(36)) / 2
                    imgHeight = (imgWidth * 0.695).toInt()
                    imageView = MyImgView(context)
                    imageView.layoutParams = ViewGroup.LayoutParams(matchParent, imgHeight)
                    addView(imageView)
                    contentView = textView {
                        textSize = 11f
                        textColor = Color.parseColor("#464d51")
                        gravity = Gravity.CENTER_VERTICAL
                        setLineSpacing(0f, 1.2f)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        horizontalPadding = dip(6)
                        layoutParams = ViewGroup.LayoutParams(matchParent, dip(43.3f))
                    }
                    nameView = textView {
                        textSize = 11f
                        textColor = Color.parseColor("#464d51")
                        gravity = Gravity.RIGHT
                        horizontalPadding = dip(6)
                        bottomPadding = dip(10)
                        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                    }
                }
            }
        }

        private inner class MyImgView(context: Context) : ImageView(context) {
            private val path by lazy {
                ImageTool.getRoundedRectPath(0f, 0f,
                        measuredWidth.toFloat(), measuredHeight.toFloat(), dip(4).toFloat(), dip(4).toFloat(), true)
            }
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)

            init {
                setLayerType(LAYER_TYPE_HARDWARE, paint)
            }

            override fun onDraw(canvas: Canvas?) {
                super.onDraw(canvas)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas?.drawPath(path, paint)
                paint.xfermode = null
            }
        }
    }
}
