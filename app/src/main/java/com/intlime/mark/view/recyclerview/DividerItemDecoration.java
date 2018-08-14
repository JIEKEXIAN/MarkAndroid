package com.intlime.mark.view.recyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by root on 15-12-22.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private int mOrientation = LinearLayoutManager.VERTICAL;

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;

    private int mDividerSize;

    /**
     * Default divider will be used
     */
    public DividerItemDecoration(Context context, int mOrientation) {
        this.mOrientation = mOrientation;
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        mDivider = styledAttributes.getDrawable(0);
        mDividerSize = mDivider.getIntrinsicHeight();
        styledAttributes.recycle();
    }

    /**
     * Custom divider will be used
     */
    public DividerItemDecoration(Drawable mDivider, int mOrientation) {
        this.mOrientation = mOrientation;
        this.mDivider = mDivider;
        mDividerSize = mDivider.getIntrinsicHeight();
    }

    /**
     * Custom color divider will be used
     */
    public DividerItemDecoration(int color, int mDividerSize, int mOrientation) {
        this.mOrientation = mOrientation;
        mDivider = new ColorDrawable(color);
        this.mDividerSize = mDividerSize;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    private void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getMeasuredHeight() - parent.getPaddingBottom();
        final int childSize = parent.getChildCount();
        for (int i = 0; i < childSize; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            RecyclerAdapter adapter = (RecyclerAdapter) parent.getAdapter();
            int position = params.getViewLayoutPosition();
            if (adapter.getNormalItemCount() > 0 && position >= adapter.headerSize - 1 && position < adapter.headerSize + adapter.getNormalItemCount()) {
                final int left = child.getRight() + params.rightMargin;
                final int right = left + mDividerSize;

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    private void drawVertical(Canvas c, RecyclerView parent) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            RecyclerAdapter adapter = (RecyclerAdapter) parent.getAdapter();
            int position = params.getViewLayoutPosition();
            if (adapter.getNormalItemCount() > 0 && position >= adapter.headerSize - 1 && position < adapter.headerSize + adapter.getNormalItemCount()) {
                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDividerSize;

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        RecyclerAdapter adapter = (RecyclerAdapter) parent.getAdapter();
        int position = lp.getViewLayoutPosition();
        if (adapter.getNormalItemCount() > 0 && position >= adapter.headerSize && position < adapter.headerSize + adapter.getNormalItemCount()) {
            if (mOrientation == LinearLayoutManager.VERTICAL) {
                outRect.top = mDividerSize;
                if (position == adapter.headerSize + adapter.getNormalItemCount() - 1) {
                    outRect.bottom = mDividerSize;
                }
            } else {
                outRect.left = mDividerSize;
                if (position == adapter.headerSize + adapter.getNormalItemCount() - 1) {
                    outRect.right = mDividerSize;
                }
            }
        }
    }
}