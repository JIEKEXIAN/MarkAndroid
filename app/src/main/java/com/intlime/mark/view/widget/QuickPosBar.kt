package com.intlime.mark.view.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewManager
import android.view.ViewTreeObserver
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.application.WWindowManager
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColor

/**
 * 快速定位条
 * Created by root on 16-3-15.
 */
class QuickPosBar(context: Context, var posChangeListener: OnPosChangeListener? = null,
                  attrs: AttributeSet? = null) : TextView(context, attrs) {
    private val strs = arrayOf("↑", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#")
    private lateinit var dialog: Dialog
    private val hintView = TextView(context)
    private var lastPos = -1

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        for (i in strs.indices) {
            if (i != 0) {
                append("\n")
            }
            append(strs[i])
        }
        textColor = resources.getColor(R.color.dark_blue)
        textSize = 11f

        setOnTouchListener { view, event ->
            val pos = (event.y / measuredHeight * strs.size).toInt()
            if (lastPos != pos && pos >= 0 && pos < strs.size) {
                posChangeListener?.onPosChange(strs[pos], pos)
                hintView.text = strs[pos]
                lastPos = pos
            }
            dialog.show()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    backgroundColor = Color.parseColor("#40000000")
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    backgroundColor = Color.TRANSPARENT
                    dialog.dismiss()
                }
            }
            return@setOnTouchListener true
        }

        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (measuredHeight != 0 && lineCount != 0 && lineHeight != 0) {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    val line_space = measuredHeight / lineCount / lineHeight.toFloat()
                    setLineSpacing(0f, line_space)
                }
                return true
            }
        })

        dialog = Dialog(WWindowManager.getInstance().currentActivity, R.style.mydialog)
        hintView.apply {
            paint.isFakeBoldText = true
            textColor = Color.WHITE
            textSize = 45f
            gravity = Gravity.CENTER
            val d = GradientDrawable()
            d.setColor(Color.parseColor("#80000000"))
            d.setCornerRadius(dip(5).toFloat())
            backgroundDrawable = d
        }
        dialog.setContentView(hintView)
        val window = dialog.window
        val lp = window.attributes
        lp.gravity = Gravity.CENTER
        lp.width = dip(70)
        lp.height = lp.width
        lp.dimAmount = 0f
        window.attributes = lp
    }

    interface OnPosChangeListener {
        fun onPosChange(s: String, pos: Int)
    }
}

inline fun ViewManager.quickPosBar(theme: Int = 0, init: QuickPosBar.() -> Unit) = ankoView({ QuickPosBar(it) },theme, init)