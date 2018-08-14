package com.intlime.mark.view.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.intlime.mark.R
import com.intlime.mark.tools.MResource
import org.jetbrains.anko.custom.ankoView

/**
 * Created by root on 16-2-2.
 */
class RatingBar constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), View.OnClickListener {
    var checked = MResource.getDrawable(R.drawable.rating_on_icon)
    var unChecked = MResource.getDrawable(R.drawable.rating_off_icon)
    private val disabled = MResource.getDrawable(R.drawable.rating_off_icon)
    private val starCount = 5
    private var rating: Int = 0
    private var onRatingBarChangeListener: OnRatingBarChangeListener? = null

    fun setOnRatingBarChangeListener(onRatingBarChangeListener: OnRatingBarChangeListener) {
        this.onRatingBarChangeListener = onRatingBarChangeListener
    }

    init {
        orientation = LinearLayout.HORIZONTAL
        init()
    }

    private fun init() {
        initViews()
    }

    private fun initViews() {
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (i in 0..starCount - 1) {
            val imageView = ImageView(context)
            imageView.setOnClickListener(this)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            addView(imageView, lp)
        }
    }

    fun setRating(rating: Int) {
        this.rating = rating
        for (i in 0..starCount - 1) {
            if (isEnabled) {
                if (i > rating - 1) {
                    (getChildAt(i) as ImageView).setImageDrawable(unChecked)
                } else {
                    (getChildAt(i) as ImageView).setImageDrawable(checked)
                }
            } else {
                (getChildAt(i) as ImageView).setImageDrawable(disabled)
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setRating(rating)
    }

    override fun onClick(v: View) {
        if (isEnabled)
            for (i in 0..starCount - 1) {
                if (v === getChildAt(i)) {
                    setRating(i + 1)
                    if (onRatingBarChangeListener != null) {
                        onRatingBarChangeListener!!.onRatingChanged(this, i + 1)
                    }
                    break
                }
            }
    }

    interface OnRatingBarChangeListener {
        fun onRatingChanged(ratingBar: RatingBar, rating: Int)
    }
}

inline fun ViewManager.ratingBar(theme: Int = 0, init: RatingBar.() -> Unit) = ankoView({ RatingBar(it) },theme, init)
