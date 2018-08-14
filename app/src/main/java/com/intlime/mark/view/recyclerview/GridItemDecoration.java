package com.intlime.mark.view.recyclerview;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by root on 16-1-19.
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private int mSpace;
    private int mSpanCount;
    private int mRadixX;
    private int mCountInFirstLine = 1;

    public GridItemDecoration(int space, int spanCount) {
        this.mSpace = space;
        this.mSpanCount = spanCount;
        this.mRadixX = space / spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, final RecyclerView parent, RecyclerView.State state) {
        final GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) view.getLayoutParams();
        final int position = params.getViewLayoutPosition();
        final int spanSize = params.getSpanSize();
        final int index = params.getSpanIndex();

        //calculate real num in line
        if (index == 0 && mSpanCount > 1 && position == 0) {
            int tempPosition = position;
            int countInLine = 0;
            int spanIndex;
            do {
                countInLine++;
                if (tempPosition < state.getItemCount() - 1) {
                    spanIndex = ((GridLayoutManager) parent.getLayoutManager()).getSpanSizeLookup().getSpanIndex(++tempPosition, mSpanCount);
                } else {
                    spanIndex = 0;
                }
            } while (spanIndex != 0);

            mCountInFirstLine = countInLine;
        }

        // invalid value
        if (spanSize < 1 || index < 0 || spanSize > mSpanCount)
            return;

        int left = mSpace - mRadixX * index;
        int right = mRadixX + mRadixX * (index + spanSize - 1);
        int top = 0;
        if (position < mCountInFirstLine) {
            top = mSpace;
        }
        int bottom = mSpace;
        outRect.set(left, top, right, bottom);
    }
}
