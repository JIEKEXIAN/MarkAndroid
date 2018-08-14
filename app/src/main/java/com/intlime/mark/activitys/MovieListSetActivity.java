package com.intlime.mark.activitys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.View;

import com.intlime.mark.R;
import com.intlime.mark.adapter.MovieListSetAdapter;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.bean.ClassifyBean;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ZhugeTool;
import com.intlime.mark.view.recyclerview.LoadRecyclerView;
import com.tendcloud.tenddata.TCAgent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

/**
 * Created by root on 16-1-29.
 */
public class MovieListSetActivity extends BaseActivity implements LoadRecyclerView.OnLoadListener, View.OnClickListener {
    private LoadRecyclerView recyclerView;
    private MovieListSetAdapter adapter;
    private int limit = 10;
    private ClassifyBean bean;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra("type", 0);
            if (type == 0) {
                MovieListBean bean2 = intent.getParcelableExtra(BEAN);
                if (bean2 != null) {
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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(BEAN)) {
            finish();
            return;
        }
        bean = getIntent().getParcelableExtra(BEAN);
        setContentView(R.layout.activity_movie_list_set_layout);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RELOAD_DISCOVER_ACTION);
        registerReceiver(broadcastReceiver, filter);

        ZhugeTool.INSTANCE.track("进入影单分类", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("分类名称", bean.getName())));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TCAgent.onPageStart(this, "影单分类");
    }

    @Override
    protected void onPause() {
        super.onPause();
        TCAgent.onPageEnd(this, "影单分类");
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle(bean.getName());
        toolbar.setNavigationIcon(R.drawable.back_icon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void initOther() {
        recyclerView = (LoadRecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLoadListener(this);
        adapter = new MovieListSetAdapter(new ArrayList<MovieListBean>());
        adapter.setItemClickListener(this);
        View view = new View(this);
        view.setMinimumHeight(DensityUtils.dp2px(this, 7));
        adapter.addFooterView(view);
        recyclerView.setAdapter(adapter);
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                List list = (List) result.get("list");
                if (list.size() < limit) {
                    recyclerView.setCanLoad(false);
                } else {
                    recyclerView.setCanLoad(true);
                }
                adapter.lists.addAll(list);
                adapter.notifyDataSetChanged();
            }
        };
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback);
        NetManager.getInstance().getMovieListSet(bean.getId(), 0, limit, callback);
    }

    @Override
    public void onLoad() {
        NetManager.getInstance().getMovieListSet(bean.getId(), adapter.lists.size(), limit, new NetRequestCallBack() {
            @Override
            public void onSuccess(ArrayMap result) {
                List list = (List) result.get("list");
                if (list.size() < limit) {
                    recyclerView.setCanLoad(false);
                }
                if (!list.isEmpty()) {
                    int start = adapter.lists.size();
                    adapter.lists.addAll(list);
                    adapter.notifyItemInserted(start);
                }
                recyclerView.loadFinish();
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                recyclerView.loadFinish();
            }
        });
    }

    @Override
    public void onClick(View v) {
        MovieListBean bean = adapter.lists.get(
                recyclerView.getChildViewHolder((View) v.getParent()).getAdapterPosition());
        Intent intent = new Intent(AppEngine.getContext(), MovieListDetailActivity.class);
        intent.putExtra(BaseActivity.BEAN, bean);
        startActivity(intent);
        Intent intent2 = new Intent(RELOAD_DISCOVER_ACTION);
        intent2.putExtra(BEAN, bean);
        intent2.putExtra("type", 1);
        sendBroadcast(intent2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception ignore) {
            }
        }
    }
}
