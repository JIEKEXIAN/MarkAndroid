package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.dip

/**
 * Created by root on 16-3-15.
 */
class PercentRatingBar(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val mPaint: Paint
    private val mPath: Path
    var bgColor = Color.parseColor("#dbdbdb")
    var fgColor = Color.parseColor("#10181e")
    var space = dip(7.1f)
    private var rating = 0f

    fun setRating(rating: Float) {
        this.rating = rating
        invalidate()
    }

    init {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPath = Path()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isEnabled = false
    }

    override fun onDraw(canvas: Canvas) {
        val radius = height / 2f
        mPath.reset()
        // top left
        mPath.moveTo(0f, radius * 0.73f)
        // top right
        mPath.lineTo(radius * 2, radius * 0.73f)
        // bottom left
        mPath.lineTo(radius * 0.38f, radius * 1.9f)
        // top
        mPath.lineTo(radius, 0f)
        // bottom right
        mPath.lineTo(radius * 1.62f, radius * 1.9f)
        // top left
        mPath.lineTo(0f, radius * 0.73f)
        mPath.close()

        mPaint.color = bgColor
        mPaint.xfermode = null
        var i = -1
        while (++i < 5) {//画五角星背景
            canvas.save()
            canvas.translate(i * radius * 2 + i * space, 0f)
            canvas.drawPath(mPath, mPaint)
            canvas.restore()
        }
        mPaint.color = fgColor
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        i = -1
        while (++i < 5) {//画裁剪过的五角星前景
            canvas.drawRect(i * radius * 2 + i * space, 0f, rating * radius + i * space, radius * 2, mPaint)
            if (rating < (i + 1) * 2) break
        }
    }
}

inline fun ViewManager.percentRatingBar(theme: Int = 0, init: PercentRatingBar.() -> Unit) = ankoView({ PercentRatingBar(it) },theme, init)