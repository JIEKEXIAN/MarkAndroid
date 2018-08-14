package com.intlime.mark.view.recyclerview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ProgressBar;

/**
 * Created by root on 15-12-29.
 */
public class LoadRecyclerView extends YRecyclerView {
    private ProgressBar mProgressBar = null;
    private boolean canLoad = false;//能否上拉加载
    private boolean disableLoad = false;//彻底关闭上拉加载功能
    private boolean isLoading;
    private OnLoadListener onLoadListener;

    public boolean isLoading() {
        return isLoading;
    }

    public void setDisableLoad(boolean disableLoad) {
        this.disableLoad = disableLoad;
        if (disableLoad) {
            mProgressBar.setVisibility(GONE);
        }
    }

    public boolean isCanLoad() {
        return canLoad;
    }

    public void setCanLoad(boolean mCanLoad) {
        if (this.canLoad != mCanLoad) {
            if (mCanLoad && isLoading && !disableLoad) {
                mProgressBar.setVisibility(VISIBLE);
            } else {
                mProgressBar.setVisibility(GONE);
            }
            this.canLoad = mCanLoad;
        }
    }

    public void setLoadListener(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
    }

    public LoadRecyclerView(Context context) {
        this(context, null);
    }

    public LoadRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        addOnScrollListener(new LoadOnScrollListener());
        setLayoutManager(new LinearLayoutManager(getContext()));
        initProgressBar();
    }

    private void initProgressBar() {
        if (mProgressBar == null) {
            mProgressBar = new ProgressBar(getContext());
            mProgressBar.setLayoutParams(new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            40, getContext().getResources().getDisplayMetrics())));
            mProgressBar.setVisibility(GONE);
        }
    }

    public void loadFinish() {
        if (isLoading) {
            isLoading = false;
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter instanceof RecyclerAdapter) {
            super.setAdapter(adapter);
            ((RecyclerAdapter) adapter).removeFooterView(mProgressBar);
            ((RecyclerAdapter) adapter).addFooterView(mProgressBar);
        } else {
            throw new RuntimeException("Unsupported Adapter used. Valid one is RecyclerAdapter！");
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (layout instanceof LinearLayoutManager) {
            super.setLayoutManager(layout);
        } else {
            throw new RuntimeException("Unsupported LayoutManager used. Valid one is LinearLayoutManager！");
        }
    }

    private class LoadOnScrollListener extends OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            if (totalItemCount > visibleItemCount && lastVisibleItem >= totalItemCount - 2) {
                if (!disableLoad && canLoad && onLoadListener != null && !isLoading) {
                    isLoading = true;
                    mProgressBar.setVisibility(VISIBLE);
                    onLoadListener.onLoad();
                }
            }
        }
    }

    public interface OnLoadListener {
        void onLoad();
    }
}
