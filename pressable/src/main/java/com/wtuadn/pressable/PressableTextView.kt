package com.wtuadn.pressable

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewManager
import android.widget.TextView
import org.jetbrains.anko.custom.ankoView

/**
 * Created by root on 15-12-17.
 */
class PressableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : TextView(context, attrs), IPressable {
    private var pressableDrawable: Drawable? = null

    init {
        PressableUtils.init(this, attrs)
    }

    @TargetApi(21)
    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        if (pressableDrawable != null) {
            pressableDrawable!!.setHotspot(x, y)
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        if (pressableDrawable != null) {
            pressableDrawable!!.state = drawableState
            invalidate()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldwidth: Int, oldheight: Int) {
        super.onSizeChanged(width, height, oldwidth, oldheight)

        if (pressableDrawable != null) {
            pressableDrawable!!.setBounds(0, 0, width, height)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (pressableDrawable != null) {
            pressableDrawable!!.draw(canvas)
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        if (pressableDrawable != null) {
            return super.verifyDrawable(who) || who === pressableDrawable
        }
        return super.verifyDrawable(who)
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()

        if (pressableDrawable != null) {
            pressableDrawable!!.jumpToCurrentState()
        }
    }

    override fun setPressableDrawable(pressableDrawable: Drawable) {
        this.pressableDrawable = pressableDrawable
    }
}

inline fun ViewManager.pressableTextView(theme: Int = 0, init: PressableTextView.() -> Unit) = ankoView({ PressableTextView(it) }, theme,init)