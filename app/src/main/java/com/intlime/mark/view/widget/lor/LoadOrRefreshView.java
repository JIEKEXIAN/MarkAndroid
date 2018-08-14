package com.intlime.mark.view.widget.lor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.intlime.mark.view.recyclerview.LoadRecyclerView;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;

/**
 * Created by root on 15-11-9.
 */
public class LoadOrRefreshView extends YSwipeRefreshLayout implements LoadRecyclerView.OnLoadListener, OnRefreshListener {
    protected LoadRecyclerView mLoadRecyclerView;
    private OnLORListener onLORListener;
    private boolean isRefreshing;
    private boolean isLoading;

    public void setOnLORListener(OnLORListener onLORListener) {
        this.onLORListener = onLORListener;
    }

    public LoadRecyclerView getmLoadRecyclerView() {
        return mLoadRecyclerView;
    }

    public LoadOrRefreshView(Context context) {
        this(context, null);
    }

    public LoadOrRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mLoadRecyclerView = new LoadRecyclerView(getContext());
        mLoadRecyclerView.setInVisible(INVISIBLE);
        mLoadRecyclerView.setHasFixedSize(true);
        mLoadRecyclerView.setLoadListener(this);
        setOnRefreshListener(this);
        addView(mLoadRecyclerView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * 结束load或者是refresh
     */
    public void finishLOR() {
        if (isRefreshing) {
            refreshFinish();
        } else {
            loadFinish();
        }
    }

    public void loadFinish() {
        if (isLoading) {
            mLoadRecyclerView.loadFinish();
            setEnabled(true);
            isLoading = false;
        }
    }

    @Override
    public void refreshFinish() {
        if (isRefreshing) {
            super.refreshFinish();
            mLoadRecyclerView.setCanLoad(true);
            isRefreshing = false;
        }
    }

    @Override
    public void onLoad() {
        isLoading = true;
        if (onLORListener != null) {
            setEnabled(false);
            onLORListener.onLoad(this);
        }
    }

    @Override
    public void onRefresh() {
        isRefreshing = true;
        mLoadRecyclerView.setCanLoad(false);
        if (onLORListener != null) {
            onLORListener.onRefresh(this);
        }
    }

    public void disable() {
        setEnabled(false);
        mLoadRecyclerView.setDisableLoad(false);
    }

    public interface OnLORListener {
        /**
         * 下拉刷新
         */
        void onRefresh(LoadOrRefreshView lor);

        /**
         * 上拉加载
         */
        void onLoad(LoadOrRefreshView lor);
    }
}
