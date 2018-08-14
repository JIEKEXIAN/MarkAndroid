package com.intlime.mark.view.widget.lor;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

/**
 * Created by root on 15-11-9.
 */
public class YSwipeRefreshLayout extends SwipeRefreshLayout {
    private OnRefreshListener mListener;

    @Override
    public void setOnRefreshListener(OnRefreshListener listener) {
        super.setOnRefreshListener(listener);
        if (listener != null) {
            this.mListener = listener;
            setEnabled(true);
        }
    }

    public YSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public YSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColorSchemeColors(Color.BLACK, Color.parseColor("#7fbc41"), Color.parseColor("#586b95"), Color.LTGRAY);
        setEnabled(false);
    }

    public void autoRefresh() {
        if (mListener != null) {
            if (!isRefreshing()) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        setRefreshing(true);
                        //动画显示完成后再调用接口
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onRefresh();
                            }
                        }, 300);
                    }
                });
            }
        }
    }

    public void refreshFinish() {
        setRefreshing(false);
    }
}
