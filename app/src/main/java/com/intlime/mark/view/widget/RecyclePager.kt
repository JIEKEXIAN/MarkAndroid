package com.intlime.mark.view.widget

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewManager
import com.intlime.mark.view.recyclerview.YRecyclerView
import org.jetbrains.anko.custom.ankoView

/**
 * Created by root on 16-2-22.
 */
open class RecyclePager(context: Context) : YRecyclerView(context), Runnable {
    var currentItem: Int = 0
    private var downX: Float = 0.toFloat()
    private var currentX: Float = 0.toFloat()
    private val detector: GestureDetector
    var pageChangeListener: OnPageChangeListener? = null

    init {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (Math.abs(velocityX) > 1800) {
                    if (velocityX > 0) {
                        smoothScrollToPosition(currentItem - 1)
                    } else {
                        smoothScrollToPosition(currentItem + 1)
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun scrollToPosition(position: Int) {
        if (adapter == null || adapter.itemCount == 0 || position < 0 || position >= adapter.itemCount) return
        super.scrollToPosition(position)
        if (position != currentItem) pageChangeListener?.onPageChange(position)
        currentItem = position
    }

    override fun smoothScrollToPosition(position: Int) {
        if (adapter == null || adapter.itemCount == 0 || position < 0 || position >= adapter.itemCount) return
        super.smoothScrollToPosition(position)
        if (position != currentItem) pageChangeListener?.onPageChange(position)
        currentItem = position
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val bool = detector.onTouchEvent(ev)
        val action = ev.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            if (!bool) {
                currentX = ev.x
                post(this)
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            downX = ev.x
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun run() {
        if (currentX - downX > measuredWidth / 2) {//左滑超过一半
            smoothScrollToPosition(currentItem - 1)
        } else if (currentX - downX < -measuredWidth / 2) {//右滑超过一半
            smoothScrollToPosition(currentItem + 1)
        } else {//复位
            smoothScrollToPosition(currentItem)
        }
    }

    interface OnPageChangeListener {
        abstract fun onPageChange(position: Int)
    }
}

inline fun ViewManager.recyclePager(theme: Int = 0, init: RecyclePager.() -> Unit) = ankoView({ RecyclePager(it) },theme, init)
