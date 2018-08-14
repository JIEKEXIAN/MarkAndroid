package com.intlime.mark.activitys

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DensityUtils
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.StringTool
import com.intlime.mark.tools.ZhugeTool
import com.intlime.mark.tools.glide.RoundTransform
import com.intlime.mark.view.MovieEmptyView
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.intlime.mark.view.widget.lor.LoadOrRefreshView
import com.tendcloud.tenddata.TCAgent
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import java.util.*

/**
 * Created by root on 16-1-22.
 */
class MovieListFavoriteActivity : BaseActivity(), LoadOrRefreshView.OnLORListener {
    private lateinit var lorView: LoadOrRefreshView
    private lateinit var adapter: MovieListAdapter
    private val limit = 10
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (BaseActivity.RELOAD_DISCOVER_ACTION == action) {//type 0:更新discoverView喜欢状态,1:更新discoverView已读状态,2:同步收藏影单状态
                    val bean = intent.getParcelableExtra<MovieListBean>(BaseActivity.BEAN)
                    val type = intent.getIntExtra("type", 0)
                    if (type == 2) {
                        updateMovieList(bean)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUI()

        val filter = IntentFilter()
        filter.addAction(RELOAD_DISCOVER_ACTION)
        registerReceiver(broadcastReceiver, filter)

        ZhugeTool.track("进入收藏", null)
    }

    private fun applyUI() {
        apply {
            rootView = verticalLayout {
                backgroundResource = R.color.bg
                lparams(matchParent, matchParent)
                toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                    navigationIconResource = R.drawable.back_icon
                    setNavigationOnClickListener { finish() }
                    title = "我喜欢的影单"
                }.lparams(matchParent, dip(49))
                frameLayout {
                    lorView = LoadOrRefreshView(this@MovieListFavoriteActivity)
                    adapter = MovieListAdapter(ArrayList<MovieListBean>())

                    val emptyView = MovieEmptyView(context,
                            R.drawable.collect_empty_icon, "没有收藏的影单", "点击影单的红心按钮可以收藏影单");
                    addView(emptyView)
                    addView(lorView, FrameLayout.LayoutParams(matchParent, matchParent))

                    val view = View(context)
                    view.minimumHeight = dip(7)
                    adapter.addFooterView(view)
                    lorView.setOnLORListener(this@MovieListFavoriteActivity)
                    lorView.getmLoadRecyclerView().inVisible = FrameLayout.VISIBLE
                    lorView.getmLoadRecyclerView().registerEmptyView(emptyView)
                    lorView.getmLoadRecyclerView().adapter = adapter
                    emptyView.visibility = FrameLayout.GONE
                    lorView.autoRefresh()
                }.lparams(matchParent, matchParent)
            }
        }
    }


    override fun onRefresh(lor: LoadOrRefreshView) {
        getMovieList(0)
    }

    override fun onLoad(lor: LoadOrRefreshView) {
        getMovieList(adapter.lists.size)
    }

    private fun getMovieList(start: Int) {
        if (Session.uid <= 0) {
            lorView.finishLOR()
            adapter.lists.clear()
            adapter.notifyDataSetChanged()
            return
        }
        NetManager.getInstance().getLikedMovieList(start, limit, object : NetRequestCallBack() {
            override fun onDefault() {
                lorView.finishLOR()
            }

            override fun onSuccess(result: ArrayMap<*, *>?) {
                if (start == 0) {
                    adapter.lists.clear()
                }
                val list = result!!["list"] as List<MovieListBean>
                adapter.lists.addAll(list)
                adapter.notifyDataSetChanged()
                if (list.size < limit) {
                    lorView.getmLoadRecyclerView().isCanLoad = false
                } else {
                    lorView.getmLoadRecyclerView().isCanLoad = true
                }
            }
        })
    }

    fun updateMovieList(bean: MovieListBean) {
        if (bean.liked == 1) {
            adapter.lists.add(0, bean)
            adapter.notifyNormalItemInserted(0)
        } else {
            for (i in adapter.lists.indices) {
                val b = adapter.lists[i]
                if (bean.id == b.id) {
                    adapter.lists.removeAt(i)
                    adapter.notifyNormalItemRemoved(i)
                    return
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TCAgent.onPageStart(this, "收藏")
    }

    override fun onPause() {
        super.onPause()
        TCAgent.onPageEnd(this, "收藏")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (ignore: Exception) {
        }

    }

    private inner class MovieListAdapter(lists: List<MovieListBean>) : RecyclerListAdapter<MovieListBean>(lists) {

        private val likeChecked = MResource.getDrawable(R.drawable.like_checked)
        private val likeUnchecked = MResource.getDrawable(R.drawable.like_unchecked)

        private val roundTransform = RoundTransform(AppEngine.getContext(), 4f)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var viewHolder: RecyclerView.ViewHolder? = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                viewHolder = Holder(View.inflate(AppEngine.getContext(), R.layout.movie_list_set_item_layout, null))
            }
            return viewHolder
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            if (viewHolder is Holder) {
                val bean = getItem(position)
                val imgUrl = StringTool.getQiniuScaledImgUrl(bean.img_url, (viewHolder.imgViewWidth * 0.95).toInt(), (viewHolder.imgViewHeight * 0.95).toInt())
                Glide.with(viewHolder.imageView.context)
                        .load(imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .transform(roundTransform)
                        .into(viewHolder.imageView)
                viewHolder.name.text = bean.name
                viewHolder.likes.text = Integer.toString(bean.likes)
                if (bean.liked == 1) {
                    viewHolder.likes.setCompoundDrawablesWithIntrinsicBounds(null, likeChecked, null, null)
                } else {
                    viewHolder.likes.setCompoundDrawablesWithIntrinsicBounds(null, likeUnchecked, null, null)
                }
            }
        }

        private inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var imageView: ImageView
            var imgViewWidth: Int = 0
            var imgViewHeight: Int = 0
            var name: TextView
            var likes: TextView
            var tag: View

            init {
                itemView.setPadding(0, DensityUtils.dp2px(itemView.context, 7f), 0, 0)
                itemView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                imageView = itemView.findViewById(R.id.image_view) as ImageView
                name = itemView.findViewById(R.id.name) as TextView
                likes = itemView.findViewById(R.id.likes) as TextView
                tag = itemView.findViewById(R.id.tag)
                tag.visibility = View.GONE

                imgViewWidth = WWindowManager.getInstance().width - DensityUtils.dp2px(AppEngine.getContext(), 14f)
                imgViewHeight = (imgViewWidth * 0.446).toInt()
                imageView.layoutParams.height = imgViewHeight
                imageView.layoutParams = imageView.layoutParams

                imageView.onClick { v ->
                    val bean = getItem(adapterPosition)
                    val intent = Intent(AppEngine.getContext(), MovieListDetailActivity::class.java)
                    intent.putExtra(BaseActivity.BEAN, bean)
                    WWindowManager.getInstance().currentActivity.startActivity(intent)
                    val intent2 = Intent(RELOAD_DISCOVER_ACTION)
                    intent2.putExtra(BaseActivity.BEAN, bean)
                    intent2.putExtra("type", 1)
                    AppEngine.getContext().sendBroadcast(intent2)
                }
                likes.setOnClickListener(View.OnClickListener {
                    val position = adapterPosition
                    if (position < 0 || position > itemCount - 1) return@OnClickListener
                    val bean = getItem(position)
                    if (bean.liked == 1)
                        bean.likes = bean.likes - 1
                    bean.liked = 0
                    likes.setCompoundDrawablesWithIntrinsicBounds(null, likeUnchecked, null, null)
                    likes.text = Integer.toString(bean.likes)
//                ZhugeTool.track("影单取消点赞", ZhugeTool.getTrackArg(Pair("影单名称", bean.name)))
                    NetManager.getInstance().likeMovieList(bean.id, 2,
                            object : NetRequestCallBack() {
                                override fun onSuccess(result: ArrayMap<Any, Any>) {
                                    val intent = Intent(RELOAD_DISCOVER_ACTION)
                                    intent.putExtra(BaseActivity.BEAN, bean)
                                    AppEngine.getContext().sendBroadcast(intent)
                                    ThreadManager.getInstance().post {
                                        try {
                                            lists.removeAt(position)
                                            notifyNormalItemRemoved(position)
                                        } catch(e: Exception) {
                                        }
                                    }
                                }

                                override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                                    ThreadManager.getInstance().post {
                                        bean.liked = 1
                                        bean.likes = bean.likes + 1
                                        if (position == adapterPosition) {
                                            likes.text = Integer.toString(bean.likes)
                                            likes.setCompoundDrawablesWithIntrinsicBounds(null, likeChecked, null, null)
                                        }
                                    }
                                }
                            })
                })
            }
        }
    }
}
