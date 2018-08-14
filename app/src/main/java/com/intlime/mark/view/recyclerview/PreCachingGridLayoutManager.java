package com.intlime.mark.view.recyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by root on 16-1-19.
 */
public class PreCachingGridLayoutManager extends GridLayoutManager {
    private int extraLayoutSpace = 100;

    public void setExtraLayoutSpace(int extraLayoutSpace) {
        this.extraLayoutSpace = extraLayoutSpace;
    }

    public PreCachingGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public PreCachingGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        return extraLayoutSpace;
    }
}
