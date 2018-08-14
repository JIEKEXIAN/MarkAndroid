package com.intlime.mark.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 自定义pageradapter
 */
public class ListPagerAdapter extends PagerAdapter {
    List<View> pagerViews;

    public ListPagerAdapter(List<View> pagerViews) {
        this.pagerViews = pagerViews;
    }

    @Override
    public int getCount() {
        return pagerViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }

    /**
     * 初始化每一页
     *
     * @param container
     * @param position
     * @return
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View pageItem = pagerViews.get(position);
        container.addView(pageItem);
        return pageItem;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}