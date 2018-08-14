package com.intlime.mark.view

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.intlime.mark.activitys.BaseActivity
import com.intlime.mark.adapter.MovieSingleAdapter
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.view.recyclerview.YRecyclerView
import com.intlime.mark.view.recyclerview.yRecyclerView
import org.jetbrains.anko.*

/**
 * Created by root on 16-1-21.
 */
class MovieSingleView(context: Context) : NestedScrollView(context), Runnable {
    companion object {
        val myAdapter = MovieSingleAdapter()
        private var cursor: Cursor? = null

        fun getLikesCursor(): Cursor {
            if (cursor == null || cursor!!.isClosed) {
                cursor = MovieSingleDbManager.getSingleCursor(0)
            }
            return cursor!!
        }
    }

    private lateinit var recyclerView: YRecyclerView
    private lateinit var movieHeaderView: MovieHeaderView
    private var isDragging: Boolean = false
    private var canUpdateMovieWord: Boolean = false
    private lateinit var detector: GestureDetector

    init {
        isFillViewport = true
        detector = GestureDetector(context, MyGestureListener())
        verticalLayout {
            lparams(matchParent, wrapContent)
            movieHeaderView = MovieHeaderView(context)
            addView(movieHeaderView)
            recyclerView = yRecyclerView {
                backgroundColor = Color.parseColor("#ededed")
                val rootView = (context as BaseActivity).rootView
                layoutParams = ViewGroup.LayoutParams(matchParent, rootView.measuredHeight - dip(98))
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(AppEngine.getContext())
                adapter = myAdapter
            }
        }
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val height = movieHeaderView.measuredHeight
                if (height != 0) {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    scrollTo(0, height)
                }
                return true
            }
        })
    }

    override fun run() {
        Session.isAnimating = true
        smoothScrollTo(0, movieHeaderView.measuredHeight)
        NetManager.getInstance().getMovieWord(NetRequestCallBack())
        ThreadManager.getInstance().postDelayed({
            scrollTo(0, movieHeaderView.measuredHeight)
            Session.isAnimating = false
        }, 300)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (detector.onTouchEvent(ev)) {
            handleUpEvent(ev)
            return true
        } else {
            handleUpEvent(ev)
            return super.dispatchTouchEvent(ev)
        }
    }

    private fun handleUpEvent(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            if (isDragging) {
                isDragging = false
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                ThreadManager.getInstance().post(this)
            } else if (scrollY != movieHeaderView.measuredHeight) {
                ThreadManager.getInstance().post(this)
            }
        }
    }

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scrollY < movieHeaderView.measuredHeight - 2) {
                isDragging = true
                if (canUpdateMovieWord) {
                    canUpdateMovieWord = false
                    movieHeaderView.updateMovieWord()
                    recyclerView.isLayoutFrozen = true
                }
                scrollBy(0, (distanceY / 2).toInt())
                return true
            } else if (isDragging) {
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                recyclerView.scrollBy(0, distanceY.toInt())
                return true
            }
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (isDragging && velocityY < 0) {
                if (recyclerView.isLayoutFrozen)
                    recyclerView.isLayoutFrozen = false
                recyclerView.fling(0, (-velocityY).toInt())
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            canUpdateMovieWord = true
            return false
        }
    }
}