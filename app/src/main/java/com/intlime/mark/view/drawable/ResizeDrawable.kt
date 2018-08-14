package com.intlime.mark.view.drawable

import android.graphics.*
import android.graphics.drawable.Drawable
import com.intlime.mark.application.AppEngine
import org.jetbrains.anko.dip

/**
 * @param width in dp
 * @param height in dp
 * Created by wtuadn on 16/04/28.
 */
open class ResizeDrawable(val bg: Drawable?, val bitmap: Bitmap, var width: Float, var height: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    init {
        width = AppEngine.getContext().dip(width).toFloat()
        height = AppEngine.getContext().dip(height).toFloat()
    }

    override fun draw(canvas: Canvas?) {
        if (canvas != null) {
            bg?.setBounds(0, 0, canvas.width, canvas.height)
            bg?.draw(canvas)
            matrix.reset()
            val scaleX = width / bitmap.width
            val scaleY = height / bitmap.height
            matrix.setScale(scaleX, scaleY)
            matrix.postTranslate((canvas.width - width) / 2f, (canvas.height - height) / 2f)
            canvas.drawBitmap(bitmap, matrix, paint)
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}