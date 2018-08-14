package com.intlime.mark.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.bean.CommentBean
import com.intlime.mark.tools.StringTool
import com.intlime.mark.tools.glide.CircleTransform
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.rockerhieu.emojicon.EmojiconTextView
import com.wtuadn.pressable.PressableRelativeLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-21.
 */
class MyNewsCommentAdapter(lists: MutableList<CommentBean>) : RecyclerListAdapter<CommentBean>(lists) {
    private val ct = CircleTransform(AppEngine.getContext())
    private val placeholder = GradientDrawable().apply {
        setColor(Color.parseColor("#e1e1e1"))
        setShape(GradientDrawable.OVAL)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var holder = super.onCreateViewHolder(parent, viewType)
        if (holder == null) {
            holder = Holder(PressableRelativeLayout(recyclerView.context))
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is Holder) {
            val commentBean = getItem(position) ?: return
            Glide.with(holder.headImgView.context)
                    .load(commentBean.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transform(ct)
                    .placeholder(placeholder)
                    .error(R.drawable.setting_header_icon)
                    .into(holder.headImgView)
            holder.nameView.text = commentBean.name
            holder.timeView.text = StringTool.getTimeToShow(commentBean.timestamp)
            holder.contentView.text = "回复我: ${commentBean.content}"
            if (commentBean.type == 0) {
                holder.preContentView.text = "我的评论: ${commentBean.preContent}"
            } else {
                holder.preContentView.text = "我的影单: ${commentBean.preContent}"
            }
        }
    }

    private inner class Holder(itemView: PressableRelativeLayout) : RecyclerView.ViewHolder(itemView) {
        lateinit var headImgView: ImageView
        lateinit var nameView: TextView
        lateinit var timeView: TextView
        lateinit var contentView: TextView
        lateinit var preContentView: TextView
        lateinit var bottomLine: View

        init {
            itemView.apply {
                lparams(matchParent, wrapContent)
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                leftPadding = dip(15)
                topPadding = dip(13.5f)
                headImgView = imageView {
                    id = 3391
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }.lparams(dip(31), dip(31)) {
                    rightMargin = dip(15)
                }
                timeView = textView {
                    id = 3394
                    textSize = 12f
                    textColor = Color.parseColor("#496069")
                }.lparams {
                    alignParentRight()
                    rightMargin = dip(15)
                }
                nameView = EmojiconTextView(context).apply {
                    id = 3393
                    textSize = 13f
                    textColor = Color.parseColor("#507daf")
                    singleLine = true
                    lines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setEmojiconSize(sp(15))
                }.lparams {
                    rightOf(headImgView)
                    leftOf(timeView)
                }
                addView(nameView)
                contentView = EmojiconTextView(context).apply {
                    id = 3395
                    textSize = 15f
                    textColor = Color.parseColor("#10181e")
                    setLineSpacing(0f, 1.2f)
                    setEmojiconSize(sp(17))
                }.lparams {
                    topMargin = dip(16)
                    rightMargin = dip(15)
                    below(nameView)
                    rightOf(headImgView)
                }
                addView(contentView)
                preContentView = EmojiconTextView(context).apply {
                    id = 3396
                    textSize = 15f
                    textColor = Color.parseColor("#747e83")
                    setLineSpacing(0f, 1.2f)
                    setEmojiconSize(sp(17))
                    backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.parseColor("#ebebeb"))
                        setCornerRadius(dip(2).toFloat())
                    }
                    padding = dip(15f)
                }.lparams(matchParent, wrapContent) {
                    topMargin = dip(9.6f)
                    rightMargin = dip(15)
                    below(contentView)
                    rightOf(headImgView)
                }
                addView(preContentView)
                bottomLine = view {
                    backgroundColor = Color.parseColor("#e1e1e1")
                }.lparams(matchParent, 1) {
                    topMargin = dip(10f)
                    below(preContentView)
                    rightOf(headImgView)
                }
            }
        }
    }
}