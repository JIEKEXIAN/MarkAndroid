package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.tools.JsonTool
import com.intlime.mark.view.recyclerview.*
import com.tencent.smtt.sdk.TbsVideo
import com.wtuadn.pressable.PressableImageView
import com.wtuadn.pressable.PressableTextView
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import org.json.JSONObject

/**
 * Created by root on 16-3-9.
 */
class MovieSourceHeader(context: Context, items: MutableList<JSONObject>) : LinearLayout(context) {
    private lateinit var mainImg: ImageView
    private lateinit var recyclerView: YRecyclerView
    private lateinit var playButton: PressableTextView
    private var cPosition = 0

    init {
        apply {
            orientation = VERTICAL
            backgroundColor = Color.WHITE

            frameLayout {
                mainImg = imageView {
                    layoutParams = LayoutParams(matchParent, dip(210.5f))
                    //                    scaleType = ScaleType.CENTER_CROP
                    Glide.with(context)
                            .load(JsonTool.optString(items[cPosition], "img_url"))
                            .diskCacheStrategy(DiskCacheStrategy.RESULT)
                            .centerCrop()
                            .into(this)
                }

                playButton = pressableTextView() {
                    backgroundResource = R.drawable.trailer_play_button_bg
                    val lp = FrameLayout.LayoutParams(dip(63), dip(25))
                    lp.gravity = Gravity.CENTER
                    layoutParams = lp
                    text = "预告"
                    textColor = Color.WHITE
                    textSize = 13f
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dip(10), 0, 0, 0)
                    compoundDrawablePadding = dip(6)
                    tag = JsonTool.optString(items[cPosition], "url")
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.white_play_icon, 0, 0, 0)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color), false, 0, 0.6f)
                    onClick {
                        try {
                            TbsVideo.openVideo(context, tag as String)
                        } catch(e: Exception) {
                        }
                    }
                }
            }

            textView("观看预告片/片花/拍摄花絮") {
                textColor = resources.getColor(R.color.dark_blue)
                textSize = 15f
                setPadding(dip(15.6f), dip(24.5f), 0, dip(21.5f))
            }

            recyclerView = yRecyclerView() {
                layoutParams = LayoutParams(matchParent, dip(58.5f))
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                addItemDecoration(DividerItemDecoration(Color.TRANSPARENT, dip(10), LinearLayoutManager.HORIZONTAL))
                adapter = Adapter(items)
                setRecyclerItemListener(object : RecyclerItemListener() {
                    init {
                        clickable = true
                    }

                    override fun onItemClick(v: View?, position: Int) {
                        cPosition = position
                        val item = (adapter as Adapter).getItem(position)
                        Glide.with(context)
                                .load(JsonTool.optString(item, "img_url"))
                                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                                .centerCrop()
                                .into(mainImg)
                        playButton.tag = JsonTool.optString(item, "url")
                        adapter.notifyDataSetChanged()
                    }
                })
            }

            view {
                val lp = LayoutParams(matchParent, dip(1))
                lp.topMargin = dip(16.5f)
                layoutParams = lp
                backgroundColor = Color.parseColor("#e1e1e1")
            }
        }
    }

    private inner class Adapter(lists: MutableList<JSONObject>) : RecyclerListAdapter<JSONObject>(lists) {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            val layout = FrameLayout(context)
            val imageView = PressableImageView(context)
            imageView.scaleType = ScaleType.CENTER_CROP
            imageView.layoutParams = RecyclerView.LayoutParams(dip(104), dip(58.5f))
            PressableUtils.setPressableDrawable(imageView, resources.getColor(R.color.black_pressed_color))

            layout.addView(imageView)
            return Holder(layout)
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                Glide.with(context)
                        .load(JsonTool.optString(getItem(position), "img_url"))
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .centerCrop()
                        .into(viewHolder.imageView)
                if (cPosition == position) {
                    (viewHolder.itemView as FrameLayout).foreground = resources.getDrawable(R.drawable.movie_source_red_stroke)
                } else {
                    (viewHolder.itemView as FrameLayout).foreground = ColorDrawable(Color.TRANSPARENT)
                }
            }
        }

        private inner class Holder(itemView: FrameLayout) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView by lazy {
                itemView.getChildAt(0) as ImageView
            }
        }
    }
}