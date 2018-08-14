package com.intlime.mark.activitys

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
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
import com.intlime.mark.adapter.ListPagerAdapter
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.db.MovieDbManager
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.intlime.mark.view.widget.MovieStatusView
import com.intlime.mark.view.widget.PercentRatingBar
import com.intlime.mark.view.widget.ViewPagerIndicator
import com.intlime.mark.view.widget.lor.LoadOrRefreshView
import com.intlime.mark.view.widget.percentRatingBar
import com.tendcloud.tenddata.TCAgent
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.support.v4.viewPager

/**
 * Created by wtuadn on 16/04/23.
 */
class CinemaActivity : BaseActivity(), LoadOrRefreshView.OnLORListener {
    private val limit = 10
    private lateinit var viewPager: ViewPager
    private lateinit var lor1: LoadOrRefreshView
    private lateinit var lor2: LoadOrRefreshView
    private lateinit var adapter1: MyAdapter
    private lateinit var adapter2: MyAdapter
    private var isFirstSwitchLor2 = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUI()
        lor1.autoRefresh()
    }

    override fun onResume() {
        super.onResume()
        TCAgent.onPageStart(this, "院线")
    }

    override fun onPause() {
        super.onPause()
        TCAgent.onPageEnd(this, "院线")
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            val indicator = object : ViewPagerIndicator(context) {
                val paint = Paint()
                override fun dispatchDraw(canvas: Canvas?) {
                    paint.isAntiAlias = true
                    paint.color = Color.parseColor("#e1e1e1")
                    paint.strokeWidth = dip(1).toFloat()
                    paint.style = Paint.Style.STROKE
                    canvas?.drawLine(measuredWidth / 2f, measuredHeight / 3f, measuredWidth / 2f, (measuredHeight / 3f) * 2.2f, paint)
                    super.dispatchDraw(canvas)
                }
            }
            indicator.apply {
                setmIndicatorWidth(dip(34))
                setmIndicatorHeight(dip(3))
                setTextSize(18f)
                setColorTextNormal(resources.getColor(R.color.dark_blue))
                setColorTextHighlight(resources.getColor(R.color.a_main_text_color))
                setIndicatorColor(resources.getColor(R.color.a_main_text_color))
                setTabItemTitles(arrayListOf("热映", "待映"), 2)
                setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(state: Int) {
                    }

                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    }

                    override fun onPageSelected(position: Int) {
                        if (position == 1 && isFirstSwitchLor2) {
                            isFirstSwitchLor2 = false
                            lor2.autoRefresh()
                        }
                    }

                })
            }.lparams(dip(172), matchParent) {
                leftMargin = WWindowManager.getInstance().width / 2 - dip(137)
            }
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                addView(indicator)
            }.lparams(matchParent, dip(49))
            viewPager = viewPager {
                backgroundColor = resources.getColor(R.color.bg)
                indicator.setViewPager(this, 0)
                lor1 = LoadOrRefreshView(context).lparams(matchParent, matchParent)
                lor1.setOnLORListener(this@CinemaActivity)
                lor1.getmLoadRecyclerView().setDisableLoad(true)
                adapter1 = MyAdapter(arrayListOf())
                lor1.getmLoadRecyclerView().adapter = adapter1
                lor2 = LoadOrRefreshView(context).lparams(matchParent, matchParent)
                lor2.setOnLORListener(this@CinemaActivity)
                adapter2 = MyAdapter(arrayListOf())
                lor2.getmLoadRecyclerView().adapter = adapter2
                adapter = ListPagerAdapter(arrayListOf(lor1, lor2) as List<View>)
            }.lparams(matchParent, matchParent)
        }
        val listener = object : RecyclerItemListener() {
            override fun onItemClick(v: View?, position: Int) {
                val bean = if (viewPager.currentItem == 0) adapter1.getItem(position) else adapter2.getItem(position)
                bean ?: return
                val localBean = MovieDbManager.getInstance().get(bean.db_num)
                if (localBean != null) {
                    startActivity<MovieDetailActivity>(BEAN to localBean)
                } else {
                    startActivity<MovieDetailActivity>(BEAN to bean, "type" to 1)
                }
            }

            override fun onItemLongClick(v: View, position: Int): Boolean {
                var bean = if (viewPager.currentItem == 0) adapter1.getItem(position) else adapter2.getItem(position)
                bean ?: return true
                val localBean = MovieDbManager.getInstance().get(bean.db_num)
                if (localBean != null) {
                    bean = localBean
                }
                MovieStatusView.show(bean, if (localBean != null) 0 else 1, null, null, object : MovieStatusView.OnStatusChangeListener {
                    override fun onStatusChange(bean1: MovieBean) {
                        if (viewPager.currentItem == 0) {
                            adapter1.notifyDataSetChanged()
                        } else {
                            adapter2.notifyDataSetChanged()
                        }
                    }
                })
                return true
            }
        }
        listener.clickable = true
        listener.longClickable = true
        lor1.getmLoadRecyclerView().setRecyclerItemListener(listener)
        lor2.getmLoadRecyclerView().setRecyclerItemListener(listener)
    }

    override fun onRefresh(lor: LoadOrRefreshView) {
        val type = if (viewPager.currentItem == 0) 1 else 2
        NetManager.getInstance().getCinemaMovie(0, type, object : NetRequestCallBack() {
            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["movies"] as List<MovieBean>
                ThreadManager.getInstance().post {
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    } else {
                        lor.getmLoadRecyclerView().isCanLoad = true
                    }
                    if (type == 1) {
                        adapter1.lists.clear()
                        adapter1.lists.addAll(list)
                        adapter1.notifyDataSetChanged()
                    } else {
                        adapter2.lists.clear()
                        adapter2.lists.addAll(list)
                        adapter2.notifyDataSetChanged()
                    }
                    lor.finishLOR()
                }
            }

            override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                ThreadManager.getInstance().post {
                    lor.finishLOR()
                }
            }
        })
    }

    override fun onLoad(lor: LoadOrRefreshView) {
        val type = if (viewPager.currentItem == 0) 1 else 2
        val adapter = if (viewPager.currentItem == 0) adapter1 else adapter2
        NetManager.getInstance().getCinemaMovie(adapter.normalItemCount, type, object : NetRequestCallBack() {
            override fun onSuccess(result: ArrayMap<*, *>) {
                val list = result["movies"] as List<MovieBean>
                ThreadManager.getInstance().post {
                    if (list.size < limit) {
                        lor.getmLoadRecyclerView().isCanLoad = false
                    }
                    if (list.isNotEmpty()) {
                        if (type == 1) {
                            adapter1.lists.addAll(list)
                            adapter1.notifyDataSetChanged()
                        } else {
                            adapter2.lists.addAll(list)
                            adapter2.notifyDataSetChanged()
                        }
                    }
                    lor.finishLOR()
                }
            }

            override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                ThreadManager.getInstance().post {
                    lor.finishLOR()
                }
            }
        })
    }

    private inner class MyAdapter(lists: MutableList<MovieBean>) : RecyclerListAdapter<MovieBean>(lists) {
        private val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
        private val error = EmptyDrawable(50f, 50f)

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                val bean = getItem(position)
                bean ?: return
                viewHolder.nameText.text = if (bean.name.isNullOrEmpty()) "-" else bean.name
                viewHolder.pubdate.text = if (bean.pubdate.isNullOrEmpty()) "-" else bean.pubdate
                viewHolder.duration_movieType.text = ""

                val sb = StringBuilder()
                val movieTypes = bean.movieType!!.split("/")
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
                if (bean.duration.isNullOrEmpty() && sb.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = "-"
                } else if (bean.duration.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = sb
                } else if (sb.isNullOrEmpty()) {
                    viewHolder.duration_movieType.text = bean.duration
                } else {
                    viewHolder.duration_movieType.text = "${bean.duration} - $sb"
                }

                viewHolder.rating.text = "${bean.db_rating}分(豆瓣)"

                Glide.with(viewHolder.imageView.context)
                        .load(bean.image)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .placeholder(placeholder)
                        .error(error)
                        .into(viewHolder.imageView)

                viewHolder.ratingBar.setRating(bean.db_rating)
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