package com.intlime.mark.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.v4.util.ArrayMap;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.GridLayoutManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.activitys.MovieDetailActivity;
import com.intlime.mark.activitys.MultiShareActivity;
import com.intlime.mark.adapter.MovieGridCursorAdapter;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.bean.SingleAccessBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.db.MovieDbManager;
import com.intlime.mark.tools.db.MovieSingleDbManager;
import com.intlime.mark.view.recyclerview.GridItemDecoration;
import com.intlime.mark.view.recyclerview.RecyclerItemListener;
import com.intlime.mark.view.recyclerview.YRecyclerView;
import com.intlime.mark.view.widget.MovieStatusView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 16-1-19.
 */
public class GridDoneView extends NestedScrollView implements Runnable {
    private LinearLayout container;
    private YRecyclerView recyclerView;
    private MovieHeaderView movieHeaderView;
    private MovieGridCursorAdapter adapter = new MovieGridCursorAdapter(1);
    private int space = DensityUtils.dp2px(getContext(), 12f);
    private GridLayoutManager layoutManager;
    private boolean isDragging;
    private boolean canUpdateMovieWord;
    private GestureDetector detector;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exitMultiMode();
        }
    };

    public YRecyclerView getRecyclerView() {
        return recyclerView;
    }

    public MovieGridCursorAdapter getAdapter() {
        return adapter;
    }

    public GridDoneView(Context context) {
        this(context, null);
    }

    public GridDoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        detector = new GestureDetector(context, new MyGestureListener());
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        init();

        AppEngine.getContext().registerReceiver(broadcastReceiver,
                new IntentFilter(MainMovieView.Companion.getEXIT_MULTI_CHOICE_ACTION()));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try{
            AppEngine.getContext().unregisterReceiver(broadcastReceiver);
        }catch (Exception ignore){
        }
    }

    private void init() {
        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        addView(container, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        movieHeaderView = new MovieHeaderView(getContext());
        container.addView(movieHeaderView);

        recyclerView = new YRecyclerView(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setRecycledViewPool(MovieGridCursorAdapter.pool);
        final MovieEmptyView emptyView = new MovieEmptyView(getContext(),
                R.drawable.done_empty_icon, "无已看的电影", "长按电影图片并选择已看可以存档电影");
        container.addView(emptyView);
        recyclerView.registerEmptyView(emptyView);
        layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setRecycleChildrenOnDetach(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setRecyclerItemListener(new MyRecyclerItemListener());

        recyclerView.addItemDecoration(new GridItemDecoration(space, layoutManager.getSpanCount()));
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) != 0)
                    return layoutManager.getSpanCount();
                else
                    return 1;
            }
        });
        container.addView(recyclerView);

        emptyView.getLayoutParams().height = 3000;
        recyclerView.getLayoutParams().height = 3000;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                int height = getMeasuredHeight();
                if (height != 0) {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    scrollTo(0, movieHeaderView.getMeasuredHeight());
                    emptyView.getLayoutParams().height = height;
                    recyclerView.getLayoutParams().height = height;
                    emptyView.setLayoutParams(emptyView.getLayoutParams());
                    recyclerView.setLayoutParams(recyclerView.getLayoutParams());
                }
                return true;
            }
        });
    }

    public void onSortChanged(int type, Cursor cursor, boolean scrollToTop) {
        if(scrollToTop) recyclerView.scrollToPosition(0);
        adapter.changeCursor(cursor);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (recyclerView.isShown()) {
            if (detector.onTouchEvent(ev)) {
                handleUpEvent(ev);
                return true;
            } else {
                handleUpEvent(ev);
                return super.dispatchTouchEvent(ev);
            }
        }
        return true;
    }

    private void handleUpEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isDragging) {
                isDragging = false;
                if (recyclerView.isLayoutFrozen())
                    recyclerView.setLayoutFrozen(false);
                ThreadManager.getInstance().post(this);
            } else if (getScrollY() != movieHeaderView.getMeasuredHeight()) {
                ThreadManager.getInstance().post(this);
            }
        }
    }

    @Override
    public void run() {
        smoothScrollTo(0, movieHeaderView.getMeasuredHeight());
        NetManager.getInstance().getMovieWord(new NetRequestCallBack());
        ThreadManager.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollTo(0, movieHeaderView.getMeasuredHeight());
                Session.isAnimating = false;
            }
        }, 300);
    }

    private class MyRecyclerItemListener extends RecyclerItemListener {
        public MyRecyclerItemListener() {
            clickable = true;
            longClickable = true;
        }

        @Override
        public void onItemClick(View v, int position) {
            MovieBean bean = adapter.getItem(position);
            Intent intent = new Intent(AppEngine.getContext(), MovieDetailActivity.class);
            intent.putExtra(BaseActivity.BEAN, bean);
            WWindowManager.getInstance().getCurrentActivity().startActivity(intent);
        }

        @Override
        public boolean onItemLongClick(View v, final int position) {
            MovieBean bean = adapter.getItem(position);
            MovieStatusView.INSTANCE.show(bean, 0, new MovieStatusView.OnMultiClickListener() {
                @Override
                public void onMultiClicked(@NotNull View v) {
                    prepareMultiMode(position);
                }
            }, null,null);
            return true;
        }
    }

    private void prepareMultiMode(int position) {
        adapter.getMultiSelectList().add(position);
        adapter.setMultiSelect(true);
        ViewGroup titleLayout = (ViewGroup) View.inflate(getContext(), R.layout.multi_mode_title_layout, null);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MResource.getDimensionPixelSize(R.dimen.title_bar_height));
        ((ViewGroup) getParent().getParent().getParent()).addView(titleLayout, lp);
        titleLayout.getChildAt(1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitMultiMode();
            }
        });
        final TextView title = (TextView) titleLayout.getChildAt(0);
        adapter.setOutMultiSelectListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                title.setText(String.format("已选择%d部", adapter.getMultiSelectList().size()));
            }
        });

        final ViewGroup tabLayout = (ViewGroup) View.inflate(getContext(), R.layout.multi_mode_tab_layout, null);
        lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MResource.getDimensionPixelSize(R.dimen.title_bar_height));
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        ((ViewGroup) getParent().getParent().getParent()).addView(tabLayout, lp);

        OnClickListener wrapperListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getMultiSelectList().isEmpty()) {
                    ToastTool.show("还没选择电影哦");
                    return;
                }
                if (v == tabLayout.getChildAt(0)) {
                    multiTodo();
                } else if (v == tabLayout.getChildAt(1)) {
                    multiCollect();
                } else if (v == tabLayout.getChildAt(2)) {
                    multiShare();
                } else {
                    multiDelete();
                }
            }
        };
        TextView tab = (TextView) tabLayout.getChildAt(0);
        tab.setText("想看");
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_todo_icon, 0, 0);
        tab.setOnClickListener(wrapperListener);
        tab = (TextView) tabLayout.getChildAt(1);
        tab.setText("喜欢");
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_collect_icon, 0, 0);
        tab.setOnClickListener(wrapperListener);
        tab = (TextView) tabLayout.getChildAt(2);
        tab.setText("分享");
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_share_icon, 0, 0);
        tab.setOnClickListener(wrapperListener);
        tab = (TextView) tabLayout.getChildAt(3);
        tab.setText("删除");
        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.multi_delete_icon, 0, 0);
        tab.setOnClickListener(wrapperListener);
    }

    private void exitMultiMode() {
        if (adapter.isMultiSelect()) {
            adapter.setMultiSelect(false);
            ViewGroup root = (ViewGroup) getParent().getParent().getParent();
            root.removeViewAt(root.getChildCount() - 1);
            root.removeViewAt(root.getChildCount() - 1);
        }
    }

    private void multiTodo() {
        final List<MovieBean> beanList = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < adapter.getMultiSelectList().size(); i++) {
            int position = adapter.getMultiSelectList().get(i);
            MovieBean bean = adapter.getItem(position);
            if(bean==null) continue;
            bean.setCursorPosition(position);
            beanList.add(bean);
            if(isFirst){
                isFirst = false;
            }else {
                stringBuilder.append(",");
            }
            stringBuilder.append(bean.getId());
        }
        NetRequestCallBack callBack = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                for (int i = 0; i < beanList.size(); i++) {
                    MovieBean bean = beanList.get(i);
                    bean.setDone(0);
                    bean.setUpdate_time((long) result.get("time"));
                    bean.setWatchTime(0);
                }
                MovieDbManager.getInstance().update(beanList);
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        exitMultiMode();
                        AppEngine.getContext()
                                .sendBroadcast(new Intent(BaseActivity.RELOAD_ALL_ACTION));
                    }
                });
            }
        };
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callBack);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("type", "2"));
        NetManager.getInstance().doneMovie(stringBuilder.toString(), params, callBack);
    }

    private void multiCollect() {
        final List<MovieBean> beanList = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < adapter.getMultiSelectList().size(); i++) {
            int position = adapter.getMultiSelectList().get(i);
            MovieBean bean = adapter.getItem(position);
            if(bean==null) continue;
            bean.setCursorPosition(position);
            beanList.add(bean);
            if(isFirst){
                isFirst = false;
            }else {
                stringBuilder.append(",");
            }
            stringBuilder.append(bean.getId());
        }
        NetRequestCallBack callBack = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                List<SingleAccessBean> list = new ArrayList<>();
                for (int i = 0; i < beanList.size(); i++) {
                    list.add(new SingleAccessBean(0, beanList.get(i).getId(), (long) result.get("time")));
                }
                MovieSingleDbManager.INSTANCE.insertAccess(list);
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        exitMultiMode();
                    }
                });
                AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.RELOAD_SINGLE_ACTION));
            }
        };
        DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callBack);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("type", "4"));
        NetManager.getInstance().favoriteMovie(stringBuilder.toString(), params, callBack);
    }

    private void multiShare() {
        if (adapter.getMultiSelectList().size() > 15) {
            ToastTool.show("一次最多只能分享15部");
            return;
        }
        final ArrayList<MovieBean> beanList = new ArrayList<>();
        for (int i = 0; i < adapter.getMultiSelectList().size(); i++) {
            int position = adapter.getMultiSelectList().get(i);
            MovieBean bean = adapter.getItem(position);
            if(bean==null) continue;
            bean.setCursorPosition(position);
            beanList.add(bean);
        }
        Intent intent = new Intent(getContext(), MultiShareActivity.class);
        intent.putParcelableArrayListExtra("list", beanList);
        WWindowManager.getInstance().getCurrentActivity().startActivity(intent);
        ThreadManager.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                exitMultiMode();
            }
        }, 500);
    }

    private void multiDelete() {
        final DialogTool.ConfirmDialog confirmDialog = DialogTool.getConfirmDialog("确认删除", "确定", "取消");
        confirmDialog.confirm_agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
                final List<MovieBean> beanList = new ArrayList<>();
                final StringBuilder stringBuilder = new StringBuilder();
                boolean isFirst = true;
                for (int i = 0; i < adapter.getMultiSelectList().size(); i++) {
                    int position = adapter.getMultiSelectList().get(i);
                    MovieBean bean = adapter.getItem(position);
                    if(bean==null) continue;
                    bean.setCursorPosition(position);
                    beanList.add(bean);
                    if(isFirst){
                        isFirst = false;
                    }else {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(bean.getId());
                }
                NetRequestCallBack callBack = new NetRequestCallBack() {
                    @Override
                    public void onDefault() {
                        DialogTool.dismissWaitDialog();
                    }

                    @Override
                    public void onSuccess(ArrayMap result) {
                        MovieDbManager.getInstance().delete(beanList);
                        ThreadManager.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                exitMultiMode();
                                AppEngine.getContext()
                                        .sendBroadcast(new Intent(BaseActivity.RELOAD_ALL_ACTION));
                            }
                        });
                    }
                };
                DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callBack);
                NetManager.getInstance().deleteMovie(stringBuilder.toString(), callBack);
            }
        });
        confirmDialog.confirm_disagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });
        confirmDialog.show();
    }


    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (getScrollY() < movieHeaderView.getMeasuredHeight() - 2) {
                isDragging = true;
                if (canUpdateMovieWord) {
                    recyclerView.setLayoutFrozen(true);
                    canUpdateMovieWord = false;
                    movieHeaderView.updateMovieWord();
                }
                scrollBy(0, (int) (distanceY / 2));
                return true;
            } else if (isDragging) {
                if (recyclerView.isLayoutFrozen())
                    recyclerView.setLayoutFrozen(false);
                recyclerView.scrollBy(0, (int) distanceY);
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isDragging && velocityY < 0) {
                if (recyclerView.isLayoutFrozen())
                    recyclerView.setLayoutFrozen(false);
                recyclerView.fling(0, (int) -velocityY);
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            canUpdateMovieWord = true;
            return false;
        }
    }
}
