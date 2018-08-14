package com.intlime.mark.adapter

import android.content.Context
import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieCardBean
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-21.
 */
class DailyCardAdapter(lists: List<MovieCardBean>) : RecyclerListAdapter<MovieCardBean>(lists) {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var holder = super.onCreateViewHolder(parent, viewType)
        if (holder == null) {
            holder = Holder(_RelativeLayout(recyclerView.context))
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is Holder) {
            val bean = getItem(position) ?: return
            if (position > 0 && position < normalItemCount - 1) {
                holder.imgView.visibility = View.VISIBLE
                holder.wordView.visibility = View.VISIBLE
                holder.nameView.visibility = View.VISIBLE
                holder.hintView.visibility = View.GONE
                holder.itemView.backgroundResource = R.drawable.share_img_bg

                Glide.with(holder.imgView.context)
                        .load(bean.imgUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .into(holder.imgView)
                holder.wordView.maxLines = Int.MAX_VALUE
                holder.wordView.textSize = 14f
                holder.nameView.textSize = 14f
                holder.wordView.text = bean.content
                holder.nameView.text = "━━《 ${bean.name} 》"
                (holder.wordView.layoutParams as ViewGroup.MarginLayoutParams).topMargin = AppEngine.getContext().dip(17)
                (holder.nameView.layoutParams as ViewGroup.MarginLayoutParams).topMargin = AppEngine.getContext().dip(26)
                resizeText(holder.itemView as ViewGroup, holder.wordView, holder.nameView)
            } else {
                holder.imgView.visibility = View.GONE
                holder.wordView.visibility = View.GONE
                holder.nameView.visibility = View.GONE
                holder.hintView.visibility = View.VISIBLE
                holder.itemView.backgroundDrawable = null
                val lp = holder.hintView.layoutParams as RelativeLayout.LayoutParams
                if (position == 0) {
                    holder.hintView.text = "每天6点更新"
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                } else {
                    holder.hintView.text = "左滑查看更多"
                    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                }
                holder.hintView.layoutParams = lp
            }
        }
    }

    private fun resizeText(card: ViewGroup, content: TextView, name: TextView) {
        var isCut = false
        name.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (name.bottom > card.measuredHeight - AppEngine.getContext().dip(10)) {
                    if (content.textSize < AppEngine.getContext().sp(9)) {
                        if (isCut) {
                            name.viewTreeObserver.removeOnPreDrawListener(this)
                            return true
                        } else {
                            isCut = true
                            content.maxLines = 7
                            content.textSize = 13f
                            name.textSize = 13f
                            (content.layoutParams as ViewGroup.MarginLayoutParams).topMargin = AppEngine.getContext().dip(16)
                            (name.layoutParams as ViewGroup.MarginLayoutParams).topMargin = AppEngine.getContext().dip(24.5f)
                            return false
                        }
                    }
                    content.setTextSize(TypedValue.COMPLEX_UNIT_PX, content.textSize - AppEngine.getContext().sp(1))
                    name.setTextSize(TypedValue.COMPLEX_UNIT_PX, content.textSize - AppEngine.getContext().sp(1))
                    (content.layoutParams as ViewGroup.MarginLayoutParams).topMargin -= AppEngine.getContext().dip(1)
                    (name.layoutParams as ViewGroup.MarginLayoutParams).topMargin -= AppEngine.getContext().dip(1.5f)
                } else {
                    name.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
                return false
            }
        })
    }

    inner class Holder(itemView: _RelativeLayout) : RecyclerView.ViewHolder(itemView) {
        lateinit var imgView: ImageView
        lateinit var wordView: TextView
        lateinit var nameView: TextView
        lateinit var hintView: TextView

        init {
            itemView.apply {
                lparams(matchParent, matchParent) {
                    horizontalMargin = dip(6)
                    topMargin = dip(6)
                    bottomMargin = dip(30)
                }
                imgView = MyImgView(context).lparams(matchParent, ((WWindowManager.getInstance().width - dip(12)) * 0.7).toInt()) {
                    topMargin = dip(6)
                    horizontalMargin = dip(6)
                }
                imgView.id = 11
                addView(imgView)
                wordView = textView { //台词
                    id = 12
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    setLineSpacing(0f, 1.2f)
                }.lparams(matchParent, wrapContent) {
                    below(11)
                    topMargin = dip(17)
                    horizontalMargin = dip(22)
                }
                nameView = textView { //电影名
                    id = 13
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    gravity = Gravity.RIGHT
                }.lparams(matchParent, wrapContent) {
                    below(12)
                    topMargin = dip(26)
                    horizontalMargin = dip(22)
                }
                hintView = textView { //提示
                    textSize = 14f
                    textColor = Color.parseColor("#818c91")
                }.lparams(wrapContent, wrapContent) {
                    horizontalMargin = dip(35)
                    centerVertically()
                }
            }
        }

        private inner class MyImgView(context: Context) : ImageView(context) {
            private val path by lazy {
                ImageTool.getRoundedRectPath(0f, 0f,
                        measuredWidth.toFloat(), measuredHeight.toFloat(), dip(6).toFloat(), dip(6).toFloat(), true)
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