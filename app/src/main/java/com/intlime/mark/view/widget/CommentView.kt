package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
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
import com.rockerhieu.emojicon.EmojiconTextView
import com.wtuadn.pressable.PressableRelativeLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-20.
 */
class CommentView(ctx: Context) : PressableRelativeLayout(ctx) {
    companion object {
        private val ct = CircleTransform(AppEngine.getContext())
        private val placeholder = GradientDrawable().apply {
            setColor(Color.parseColor("#e1e1e1"))
            setShape(GradientDrawable.OVAL)
        }
    }

    var commentBean: CommentBean? = null
    var onLikeChangedListener: OnLikeChangedListener? = null
    lateinit var headImgView: ImageView
    lateinit var likeView: TextView
    lateinit var nameView: TextView
    lateinit var timeView: TextView
    lateinit var contentView: TextView
    lateinit var preContentView: TextView
    lateinit var bottomLine: View

    init {
        apply {
            lparams(matchParent, wrapContent)
            PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
            leftPadding = dip(15)
            headImgView = imageView {
                id = 3391
                scaleType = ImageView.ScaleType.CENTER_CROP
            }.lparams(dip(31), dip(31)) {
                rightMargin = dip(15)
                topMargin = dip(13.5f)
            }
            likeView = textView {
                id = 3392
                textSize = 12f
                gravity = Gravity.BOTTOM
                topPadding = dip(13.5f)
                rightPadding = dip(15f)
                leftPadding = dip(7.5f)
                bottomPadding = dip(7.5f)
                compoundDrawablePadding = dip(3)
                onClick {
                    onLikeChangedListener?.onLikeChangedListener(commentBean, this@CommentView)
                }
            }.lparams {
                alignParentRight()
            }
            nameView = EmojiconTextView(context).apply {
                id = 3393
                textSize = 13f
                textColor = Color.parseColor("#496069")
                singleLine = true
                lines = 1
                ellipsize = TextUtils.TruncateAt.END
                setEmojiconSize(sp(15))
            }.lparams {
                rightOf(3391)
                leftOf(3392)
                topMargin = dip(13.5f)
            }
            addView(nameView)
            timeView = textView {
                id = 3394
                textSize = 10f
                textColor = Color.parseColor("#818c91")
            }.lparams {
                topMargin = dip(5)
                below(nameView)
                rightOf(3391)
                leftOf(3392)
            }
            contentView = EmojiconTextView(context).apply {
                id = 3395
                textSize = 15f
                textColor = Color.parseColor("#10181e")
                setLineSpacing(0f, 1.2f)
                setEmojiconSize(sp(17))
            }.lparams {
                topMargin = dip(12)
                rightMargin = dip(15)
                below(timeView)
                rightOf(3391)
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
                topMargin = dip(11.6f)
                rightMargin = dip(15)
                below(contentView)
                rightOf(3391)
            }
            addView(preContentView)
            bottomLine = view {
                backgroundColor = Color.parseColor("#e1e1e1")
            }.lparams(matchParent, 1) {
                topMargin = dip(13.5f)
                below(preContentView)
                rightOf(3391)
            }
        }
    }

    fun update() {
        updateHead()
        updateLike()
        updateName()
        updateTime()
        updateContent()
        updatePreContent()
    }

    private fun updateHead() {
        Glide.with(context)
                .load(commentBean!!.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .transform(ct)
                .placeholder(placeholder)
                .error(R.drawable.setting_header_icon)
                .into(headImgView)
    }

    private fun updateLike() {
        likeView.text = "${commentBean!!.likes}"
        if (commentBean!!.liked == 1) {
            likeView.textColor = Color.parseColor("#496069")
            likeView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.comment_like_checked, 0)
        } else {
            likeView.textColor = Color.parseColor("#818c91")
            likeView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.comment_like_unchecked, 0)
        }
    }

    private fun updateName() {
        nameView.text = commentBean!!.name
    }

    private fun updateTime() {
        timeView.text = StringTool.getTimeToShow(commentBean!!.timestamp)
    }

    private fun updateContent() {
        if (commentBean!!.preContent.isNullOrEmpty()) {
            contentView.text = commentBean!!.content
        } else {
            val colorString = "@${commentBean!!.preName}"
            val span = SpannableString("回复$colorString: ${commentBean!!.content}")
            span.setSpan(ForegroundColorSpan(Color.parseColor("#507daf")), 2, colorString.length + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            contentView.text = span
        }
    }

    private fun updatePreContent() {
        if (commentBean!!.preContent.isNullOrEmpty()) {
            preContentView.visibility = GONE
            return
        }
        preContentView.visibility = VISIBLE
        val colorString = "@${commentBean!!.preName}"
        val span = SpannableString("$colorString: ${commentBean!!.preContent}")
        span.setSpan(ForegroundColorSpan(Color.parseColor("#507daf")), 0, colorString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        preContentView.text = span
    }

    interface OnLikeChangedListener {
        fun onLikeChangedListener(commentBean: CommentBean?, commentView: CommentView)
    }
}