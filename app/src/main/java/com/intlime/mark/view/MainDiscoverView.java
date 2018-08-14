package com.intlime.mark.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.disklrucache.DiskLruCache;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.activitys.MovieListDetailActivity;
import com.intlime.mark.activitys.MovieListSetActivity;
import com.intlime.mark.adapter.DiscoverAdapter;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.ClassifyBean;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.glide.BannerKey;
import com.intlime.mark.view.recyclerview.RecyclerItemListener;
import com.intlime.mark.view.widget.Banner;
import com.intlime.mark.view.widget.DiscoverClassify;
import com.intlime.mark.view.widget.lor.LoadOrRefreshView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by root on 16-2-22.
 */
public class MainDiscoverView extends LoadOrRefreshView implements LoadOrRefreshView.OnLORListener{
    private DiscoverAdapter adapter;
    private List<MovieListBean> bannerList;
    private Banner banner;
    private DiscoverClassify classify;
    private int limit = 10;
    private boolean isFirstRefresh = true;
    private int lastRefreshHour = -1;
    private List<Integer> readedList;
    private int groupCount;
    private int lastReadedId = -1;//最后一个已读id
    private String tempData;

    public MainDiscoverView(Context context) {
        this(context, null);
    }

    public MainDiscoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnLORListener(this);
        setBackgroundColor(Color.parseColor("#ebebeb"));
        init();
    }

    private void init() {
        bannerList = new ArrayList<>();
        adapter = new DiscoverAdapter(new ArrayList<MovieListBean>());
        mLoadRecyclerView.setAdapter(adapter);
        RecyclerItemListener recyclerItemListener = new RecyclerItemListener() {
            @Override
            public void onItemClick(View v, int position) {
                BaseActivity baseActivity = WWindowManager.getInstance().getCurrentActivity();
                if (baseActivity == null) return;
                final DiscoverAdapter.Holder holder = (DiscoverAdapter.Holder)
                        mLoadRecyclerView.getChildViewHolder(v);
                final MovieListBean bean = adapter.getItem(position);
                if (bean == null) return;
                if (bean.isNew() == 1) {
                    holder.tag.setVisibility(GONE);
                    bean.setNew(0);
                    readedList.add(bean.getId());
                    sortReadedList();
                    SettingManager.getInstance().setReadedMovieList(readedList);
                }
                Intent intent = new Intent(AppEngine.getContext(), MovieListDetailActivity.class);
                intent.putExtra(BaseActivity.BEAN, bean);
                baseActivity.startActivity(intent);
            }
        };
        recyclerItemListener.clickable = true;
        mLoadRecyclerView.setRecyclerItemListener(recyclerItemListener);
        autoRefresh();
    }

    private void initBanner() {
        if (banner != null) return;
        banner = new Banner(getContext(), new Banner.ViewFactory<ImageView, MovieListBean>(bannerList) {
            @Override
            protected ImageView createView() {
                ImageView imageView = new ImageView(AppEngine.getContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new LayoutParams(WWindowManager.getInstance().getWidth(), LayoutParams.MATCH_PARENT));
                return imageView;
            }

            @Override
            protected void apply(ImageView imageView, MovieListBean bean) {
                Glide.with(imageView.getContext())
                        .load(bean.getImg_url())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .signature(new BannerKey(bean))
                        .into(imageView);
            }

            @Override
            protected void onItemClick(ImageView imageView, MovieListBean bean) {
                if (bean.getType() == 2) {
                    Intent intent = new Intent(AppEngine.getContext(), MovieListSetActivity.class);
                    ClassifyBean classifyBean = new ClassifyBean();
                    classifyBean.setId(bean.getId());
                    classifyBean.setName(bean.getName());
                    classifyBean.setImgUrl(bean.getImg_url());
                    intent.putExtra(BaseActivity.BEAN, classifyBean);
                    WWindowManager.getInstance().getCurrentActivity().startActivity(intent);
                } else {
                    readed(bean);
                    Intent intent = new Intent(AppEngine.getContext(), MovieListDetailActivity.class);
                    intent.putExtra(BaseActivity.BEAN, bean);
                    WWindowManager.getInstance().getCurrentActivity().startActivity(intent);
                }
            }
        });
        banner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (WWindowManager.getInstance().getWidth() * 0.487)));
        if (!adapter.getHeaderList().contains(banner)) {
            adapter.addHeaderView(banner, 0);
        }
    }

    private void initClassify() {
        if (classify != null) return;
        classify = new DiscoverClassify(getContext());
        if (!adapter.getHeaderList().contains(classify)) {
            adapter.addHeaderView(classify);
        }
    }

    @Override
    public void onRefresh(LoadOrRefreshView lor) {
        groupCount = 0;
        tempData = "";
        NetManager.getInstance().getBanner(new NetRequestCallBack() {

            @Override
            public void onSuccess(final ArrayMap result) {
                final List<MovieListBean> list = (List<MovieListBean>) result.get("banner");
                checkBannerCache(list);
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        initBanner();
                        initClassify();
                        classify.updateDay();
                        bannerList.clear();
                        bannerList.addAll(list);
                        banner.setDotCount(bannerList.size());
                        banner.getAdapter().notifyDataSetChanged();
                        if (bannerList.size() <= 1) {
                            banner.setCanSwitch(false);
                        } else {
                            banner.startSwitch();
                            if (isFirstRefresh) {
                                isFirstRefresh = false;
                                banner.scrollToPosition(bannerList.size() * 1000);
                            }
                        }
                    }
                });
            }
        });
        getDiscoverList(true);
    }

    @Override
    public void onLoad(LoadOrRefreshView lor) {
        getDiscoverList(false);
    }

    public void onResume() {
        if (lastRefreshHour == -1) return;
        int hour = Calendar.getInstance(Locale.CHINA).get(Calendar.HOUR_OF_DAY);
        if (hour >= 8 && hour < 12 && (lastRefreshHour < 8 || lastRefreshHour >= 12)) {
            autoRefresh();
        }
        if ((hour < 8 || hour >= 12) && lastRefreshHour >= 8 && lastRefreshHour < 12) {
            autoRefresh();
        }
    }

    private void getDiscoverList(final boolean isRefresh) {
        NetManager.getInstance().getDiscoverList(isRefresh ? 0 : adapter.lists.size(), limit, new NetRequestCallBack() {

            @Override
            public void onSuccess(ArrayMap result) {
                final List<MovieListBean> movieLists = (List) result.get("list");
                generateMovieList(movieLists);
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        if (isRefresh) {
                            adapter.lists.clear();
                        }
                        int start = adapter.lists.size();
                        adapter.lists.addAll(movieLists);
                        if (isRefresh) {
                            adapter.notifyDataSetChanged();
                            if (!adapter.lists.isEmpty())
                                mLoadRecyclerView.scrollToPosition(0);
                        } else if (!movieLists.isEmpty()) {
                            adapter.notifyNormalItemInserted(start);
                        }
                        if (movieLists.size() < limit) {
                            mLoadRecyclerView.setCanLoad(false);
                        } else {
                            mLoadRecyclerView.setCanLoad(true);
                        }
                        finishLOR();
                        lastRefreshHour = Calendar.getInstance(Locale.CHINA).get(Calendar.HOUR_OF_DAY);
                    }
                });
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        finishLOR();
                    }
                });
            }
        });
    }

    private void generateMovieList(List<MovieListBean> list) {
        if (list.isEmpty()) return;
        if (readedList == null) readedList = SettingManager.getInstance().getReadedMovieList();
        SimpleDateFormat sdf = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA);

        for (int i = 0; i < list.size(); i++) {
            MovieListBean bean = list.get(i);
            try {
                Date date = new Date(bean.getPublish_time() * 1000L);
                bean.setTimeToShow(sdf.format(date));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!tempData.equals(bean.getTimeToShow())) {
                groupCount++;
                tempData = bean.getTimeToShow();
            }
            if (groupCount < 4) {
                boolean isReaded = false;
                for (int j = 0; j < readedList.size(); j++) {
                    if (bean.getId() == readedList.get(j)) {
                        isReaded = true;
                        lastReadedId = bean.getId();
                        break;
                    }
                }
                if (!isReaded) {
                    bean.setNew(1);
                }
            }
        }
        if (lastReadedId > 0 && groupCount > 3) {
            boolean needClear = false;
            for (int i = 0; i < readedList.size(); i++) {
                int id = readedList.get(i);
                if (needClear) readedList.remove(i--);
                if (id == lastReadedId) needClear = true;
            }
            lastReadedId = -1;
        }
        SettingManager.getInstance().setReadedMovieList(readedList);
    }

    private void sortReadedList() {
        List<Integer> tempList = new ArrayList<>();
        for (int i = 0; i < adapter.lists.size(); i++) {
            MovieListBean bean1 = adapter.lists.get(i);
            for (int j = 0; j < readedList.size(); j++) {
                if (bean1.getId() == readedList.get(j)) {
                    tempList.add(bean1.getId());
                    break;
                }
            }
        }
        readedList.clear();
        readedList.addAll(tempList);
    }

    public void updateLikes(MovieListBean bean2) {
        for (int i = 0; i < adapter.lists.size(); i++) {
            MovieListBean bean = adapter.lists.get(i);
            if (bean.getId() == bean2.getId()) {
                bean.setLiked(bean2.getLiked());
                bean.setLikes(bean2.getLikes());
                adapter.notifyNormalItemChanged(i);
                return;
            }
        }
    }

    public void readed(MovieListBean bean2) {
        for (int i = 0; i < adapter.lists.size(); i++) {
            MovieListBean bean = adapter.lists.get(i);
            if (bean.getId() == bean2.getId()) {
                bean.setNew(0);
                adapter.notifyNormalItemChanged(i);
                readedList.add(bean.getId());
                sortReadedList();
                SettingManager.getInstance().setReadedMovieList(readedList);
                return;
            }
        }
    }

    private void checkBannerCache(List<MovieListBean> list) {
        File[] cacheFiles = BannerKey.Companion.getCachedFiles();
        if (cacheFiles != null) {
            for (int i = 0; i < cacheFiles.length; i++) {
                File file = cacheFiles[i];
                String idStr = file.getName().split("_")[0];
                boolean found = false;
                if (TextUtils.isDigitsOnly(idStr)) {
                    int id = Integer.parseInt(idStr);
                    for (int j = 0; j < list.size(); j++) {
                        if (list.get(j).getId() == id) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found && !file.getName().equals(DiskLruCache.JOURNAL_FILE)) {
                    file.delete();
                }
            }
        }
    }
}
