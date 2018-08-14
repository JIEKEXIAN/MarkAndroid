package com.intlime.mark.view.recyclerview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by root on 15-12-30.
 */
public class PreCachingLinearLayoutManager extends LinearLayoutManager {
    private int extraLayoutSpace;

    public PreCachingLinearLayoutManager(Context context, int extraLayoutSpace) {
        super(context);
        this.extraLayoutSpace = extraLayoutSpace;
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        return extraLayoutSpace;
    }
}
