package com.intlime.mark.view.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import static android.view.ViewTreeObserver.OnScrollChangedListener;

/**
 * Created by Bruce on 11/24/14.
 */
public class SwipeMenuLayout extends LinearLayout implements Runnable, OnScrollChangedListener {

    private ViewDragHelper viewDragHelper;
    private View contentView;
    private View actionView;
    private int dragDistance;
    private final double AUTO_OPEN_SPEED_LIMIT = 800.0;
    private int draggedX;
    private boolean canDrag = true;

    public void setCanDrag(boolean canDrag) {
        this.canDrag = canDrag;
    }

    public SwipeMenuLayout(Context context) {
        this(context, null);
    }

    public SwipeMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        viewDragHelper = ViewDragHelper.create(this, new DragHelperCallback());
        getViewTreeObserver().addOnScrollChangedListener(this);
    }

    @Override
    protected void onFinishInflate() {
        if (isInEditMode()) {
            return;
        }
        contentView = getChildAt(0);
        actionView = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isInEditMode()) {
            return;
        }
        dragDistance = actionView.getMeasuredWidth();
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View view, int i) {
            return view == contentView || view == actionView;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            draggedX = left;
            if (changedView == contentView) {
                actionView.offsetLeftAndRight(dx);
            } else {
                contentView.offsetLeftAndRight(dx);
            }
            invalidate();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (Math.abs(left) > 20) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (child == contentView) {
                final int minLeftBound = -getPaddingLeft() - dragDistance;
                return Math.min(Math.max(minLeftBound, left), 0);
            } else {
                final int minLeftBound = getPaddingLeft() + contentView.getMeasuredWidth() - dragDistance;
                final int maxLeftBound = getPaddingLeft() + contentView.getMeasuredWidth() + getPaddingRight();
                return Math.min(Math.max(left, minLeftBound), maxLeftBound);
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return dragDistance;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            boolean settleToOpen = false;
            if (xvel > AUTO_OPEN_SPEED_LIMIT) {
                settleToOpen = false;
            } else if (xvel < -AUTO_OPEN_SPEED_LIMIT) {
                settleToOpen = true;
            } else if (draggedX <= -dragDistance / 2) {
                settleToOpen = true;
            } else if (draggedX > -dragDistance / 2) {
                settleToOpen = false;
            }

            final int settleDestX = settleToOpen ? -dragDistance : 0;
            viewDragHelper.smoothSlideViewTo(contentView, settleDestX, 0);
            ViewCompat.postInvalidateOnAnimation(SwipeMenuLayout.this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (canDrag) {
            if (viewDragHelper.shouldInterceptTouchEvent(ev)) {
                return true;
            } else if (dragDistance != 0 && ((contentView.getLeft() == -dragDistance) && (ev.getX() < contentView.getRight()))) {
                post(SwipeMenuLayout.this);
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (canDrag) {
            viewDragHelper.processTouchEvent(event);
            if (contentView.getLeft() != 0) {
                event.setAction(MotionEvent.ACTION_CANCEL);
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * 重置contentView的位置
     */
    @Override
    public void run() {
        if (contentView.getLeft() == -dragDistance) {
            viewDragHelper.smoothSlideViewTo(contentView, 0, 0);
            ViewCompat.postInvalidateOnAnimation(SwipeMenuLayout.this);
        }
    }

    @Override
    public void onScrollChanged() {
        run();
    }
}
