package com.wtuadn.pressable

import android.content.Context
import android.graphics.drawable.Drawable

/**
 * Created by root on 16-1-8.
 */
interface IPressable : Drawable.Callback {
    fun getContext(): Context

    fun getBackground(): Drawable

    fun setPressableDrawable(pressableDrawable: Drawable)

    fun setBackgroundDrawable(background: Drawable)
}
