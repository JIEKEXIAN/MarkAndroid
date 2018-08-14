package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import com.intlime.mark.tools.DensityUtils

/**
 * Created by root on 16-3-10.
 */
class BigPlayButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : TextView(context, attrs) {
    private val paint: Paint
    private var isPress: Boolean = false

    init {
        paint = Paint()
        paint.isAntiAlias = true
        paint.strokeWidth = DensityUtils.dp2px(context, 1f).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = paint.strokeWidth
        val a = measuredWidth - stroke
        val b: Float = measuredHeight / 2 - stroke
        val r = ((a * a + b * b) / (2 * a)).toFloat()
        if (isPress) {
            paint.color = Color.parseColor("#8ce1e1e1")
            paint.style = Paint.Style.FILL
            canvas.drawArc(RectF(stroke, b - r + stroke, 2 * r, b + r + stroke), 0f, 360f, false, paint)

            paint.color = Color.parseColor("#bfffffff")
            paint.style = Paint.Style.STROKE
            canvas.drawArc(RectF(stroke, b - r + stroke, 2 * r, b + r + stroke), 0f, 360f, false, paint)
        } else {
            paint.color = Color.parseColor("#66000000")
            paint.style = Paint.Style.FILL
            canvas.drawArc(RectF(stroke, b - r + stroke, 2 * r, b + r + stroke), 0f, 360f, false, paint)

            paint.color = Color.parseColor("#a6ffffff")
            paint.style = Paint.Style.STROKE
            canvas.drawArc(RectF(stroke, b - r + stroke, 2 * r, b + r + stroke), 0f, 360f, false, paint)
        }

        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            isPress = true
            invalidate()
        } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            isPress = false
            invalidate()
        }
        return super.onTouchEvent(event)
    }
}
