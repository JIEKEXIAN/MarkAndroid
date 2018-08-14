package com.intlime.mark.activitys;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.disklrucache.DiskLruCache;
import com.igexin.download.DownloadService;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.PushService;
import com.intlime.mark.R;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.UmengTool;
import com.intlime.mark.tools.ZhugeTool;
import com.intlime.mark.tools.db.DBHelper;
import com.intlime.mark.tools.glide.MovieListKey;
import com.intlime.mark.view.MainDiscoverView;
import com.intlime.mark.view.MainMovieView;
import com.intlime.mark.view.MainUserView;
import com.intlime.mark.view.MovieDoneView;
import com.intlime.mark.view.MovieSearchView;
import com.intlime.mark.view.MovieSingleView;
import com.intlime.mark.view.MovieTodoView;
import com.intlime.mark.view.widget.NestRadioGroup;
import com.tendcloud.tenddata.TCAgent;
import com.umeng.socialize.UMShareAPI;
import com.umeng.update.UmengUpdateAgent;

import org.apache.http.NameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends BaseActivity implements NestRadioGroup.OnCheckedChangeListener {
    private NestRadioGroup tabLayout;
    private View titleBar;
    private TextView titleText;
    private MainDiscoverView mainDiscoverView;
    private MainMovieView mainMovieView;
    private MainUserView mainUserView;
    private View search;
    private View movieSort;
    private View setting;
    private TextView newsCount;
    private CheckBox modeCheckBox;
    private long lastPressTime = 0;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (RELOAD_ALL_ACTION.equals(action)) {
                    reloadAll();
                    View view = ((ViewGroup) rootView).getChildAt(((ViewGroup) rootView).getChildCount() - 1);
                    if (view instanceof MovieSearchView
                            && WWindowManager.getInstance().getCurrentActivity() != MainActivity.this) {
                        ((MovieSearchView) view).onChange();
                    }
                } else if (RELOAD_DISCOVER_ACTION.equals(action)) {//type 0:更新discoverView喜欢状态,1:更新discoverView已读状态,2:同步收藏影单状态
                    MovieListBean bean = intent.getParcelableExtra(BEAN);
                    int type = intent.getIntExtra("type", 0);
                    if (type == 0) {
                        mainDiscoverView.updateLikes(bean);
                    } else if (type == 1) {
                        mainDiscoverView.readed(bean);
                    }
                } else if (RELOAD_SINGLE_ACTION.equals(action)) {
                    MovieSingleView.Companion.getLikesCursor().close();
                    MovieSingleView.Companion.getMyAdapter().newCursor();
                    MovieSingleView.Companion.getMyAdapter().notifyDataSetChanged();
                } else if (NOTIFY_COUNT_ACTION.equals(action)) {
                    updateNewsCount();
                } else {
                    if (mainUserView != null) mainUserView.updateLayout();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void reloadAll() {
        if (mainMovieView != null) {
            mainMovieView.reloadAll();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        customAnimation = getIntent().getBooleanExtra(ANIMATION, false);
        if (customAnimation) {
            while (!WWindowManager.getInstance().getWindows().isEmpty()) {
                WWindowManager.getInstance().getWindows().pop().finish();
            }
            reloadAll();
            MovieSingleView.Companion.getLikesCursor().close();
            if (mainUserView != null) {
                MovieSingleView.Companion.getLikesCursor().close();
                MovieSingleView.Companion.getMyAdapter().newCursor();
                MovieSingleView.Companion.getMyAdapter().notifyDataSetChanged();
            }
        }
        super.onCreate(savedInstanceState);
        customAnimation = false;
        setContentView(R.layout.activity_main_layout);

        UmengUpdateAgent.update(this);
        PushManager.getInstance().initialize(getApplicationContext(),PushService.class);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RELOAD_ALL_ACTION);
        filter.addAction(RELOAD_DISCOVER_ACTION);
        filter.addAction(RELOAD_SINGLE_ACTION);
        filter.addAction(NOTIFY_COUNT_ACTION);
        filter.addAction(RELOAD_USER_CENTER_ACTION);
        registerReceiver(broadcastReceiver, filter);

        updateData(false);
        DBHelper.getInstance().getReadableDatabase();

        ZhugeTool.INSTANCE.track("进入发现", null);
        updatePushId();
    }

    private void updatePushId() {
        ThreadManager.getInstance().postDelayed(new Runnable() {
            private int count = 0;

            @Override
            public void run() {
                if (Session.uid > 0) {
                    count++;
                    final String pushId = PushManager.getInstance().getClientid(getApplicationContext());
                    if (!TextUtils.isEmpty(pushId)) {
                        if (!pushId.equals(SettingManager.getInstance().getPushId())) {
                            NetManager.getInstance().updatePushId(pushId, new NetRequestCallBack() {
                                @Override
                                public void onSuccess(ArrayMap result) {
                                    SettingManager.getInstance().setPushId(pushId);
                                }
                            });
                        }
                    } else if (count < 3) {
                        ThreadManager.getInstance().postDelayed(this, 10000);
                    }
                }
            }
        }, 10000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void initOther() {
        titleBar = findViewById(R.id.titleBar);
        search = titleBar.findViewById(R.id.search);
        movieSort = titleBar.findViewById(R.id.movie_sort);
        setting = titleBar.findViewById(R.id.setting);
        titleText = (TextView) titleBar.findViewById(R.id.title);
        titleText.setText("发现");
        modeCheckBox = (CheckBox) findViewById(R.id.mode_checkbox);
        initTabLayout();
        initDiscoverView();
        updateNewsCount();
        NetManager.getInstance().getUserUnreadCount(new NetRequestCallBack() {
            @Override
            public void onSuccess(ArrayMap result) {
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        updateNewsCount();
                    }
                });
            }
        });
    }

    private void initTabLayout() {
        tabLayout = (NestRadioGroup) findViewById(R.id.main_tab_layout);
        tabLayout.setOnCheckedChangeListener(this);
        newsCount = (TextView) tabLayout.findViewById(R.id.news_count);
    }

    private void initDiscoverView() {
        mainDiscoverView = new MainDiscoverView(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.main_tab_layout);
        lp.addRule(RelativeLayout.BELOW, R.id.titleBar);
        ((RelativeLayout) rootView).addView(mainDiscoverView, lp);
    }

    private void initMovieView() {
        mainMovieView = new MainMovieView(this, modeCheckBox, (TextView) movieSort);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.main_tab_layout);
        lp.addRule(RelativeLayout.BELOW, R.id.titleBar);
        ((RelativeLayout) rootView).addView(mainMovieView, lp);
    }

    private void initUserView() {
        mainUserView = new MainUserView(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.main_tab_layout);
        lp.addRule(RelativeLayout.BELOW, R.id.titleBar);
        ((RelativeLayout) rootView).addView(mainUserView, lp);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            case R.id.search:
                ((RelativeLayout) rootView).addView(new MovieSearchView(MainActivity.this));
                ZhugeTool.INSTANCE.track("搜索", null);
                break;
        }
    }

    private void updateNewsCount() {
        int count = SettingManager.getInstance().getCommentsCount() + SettingManager.getInstance().getNotifyCount();
        if (count > 0) {
            newsCount.setVisibility(View.VISIBLE);
            newsCount.setText(Integer.toString(count));
        } else {
            newsCount.setVisibility(View.GONE);
        }
        if (mainUserView != null) {
            mainUserView.updateNewsCount();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View view = ((ViewGroup) rootView).getChildAt(((ViewGroup) rootView).getChildCount() - 1);
        if (!(view instanceof MovieSearchView) && tabLayout != null
                && tabLayout.getCheckedRadioButtonId() == R.id.discover && mainDiscoverView != null) {
            mainDiscoverView.onResume();
        }
        switch (tabLayout.getCheckedRadioButtonId()) {
            case R.id.discover:
                TCAgent.onPageStart(this, "发现");
                break;
            case R.id.movies:
                if (mainMovieView != null) {
                    int id = mainMovieView.radioGroup.getCheckedRadioButtonId();
                    if (id == 1) {
                        TCAgent.onPageStart(this, "想看");
                    } else if (id == 2) {
                        TCAgent.onPageStart(this, "已看");
                    } else {
                        TCAgent.onPageStart(this, "自建影单");
                    }
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        switch (tabLayout.getCheckedRadioButtonId()) {
            case R.id.discover:
                TCAgent.onPageEnd(this, "发现");
                break;
            case R.id.movies:
                if (mainMovieView != null) {
                    int id = mainMovieView.radioGroup.getCheckedRadioButtonId();
                    if (id == 1) {
                        TCAgent.onPageEnd(this, "想看");
                    } else if (id == 2) {
                        TCAgent.onPageEnd(this, "已看");
                    } else {
                        TCAgent.onPageEnd(this, "自建影单");
                    }
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(NestRadioGroup group, int checkedId) {
        Session.isAnimating = true;
        View inView = null;
        switch (checkedId) {
            case R.id.discover:
                ZhugeTool.INSTANCE.track("进入发现", null);
                TCAgent.onPageStart(this, "发现");
                titleText.setText("发现");
                if (mainDiscoverView == null) {
                    initDiscoverView();
                }

                search.setVisibility(View.VISIBLE);
                setting.setVisibility(View.GONE);
                movieSort.setVisibility(View.GONE);
                modeCheckBox.setVisibility(View.GONE);
                titleBar.setBackgroundResource(R.drawable.toolbar_bg);
                inView = mainDiscoverView;
                break;
            case R.id.movies:
                titleText.setText("我的电影");
                if (mainMovieView == null) {
                    initMovieView();
                }
                int id = mainMovieView.radioGroup.getCheckedRadioButtonId();
                if (id == 1) {
                    ZhugeTool.INSTANCE.track("进入想看", null);
                    TCAgent.onPageStart(this, "想看");
                } else if (id == 2) {
                    ZhugeTool.INSTANCE.track("进入已看", null);
                    TCAgent.onPageStart(this, "已看");
                } else {
                    ZhugeTool.INSTANCE.track("进入自建影单", null);
                    TCAgent.onPageStart(this, "自建影单");
                }
                search.setVisibility(View.GONE);
                setting.setVisibility(View.GONE);
                movieSort.setVisibility(View.VISIBLE);
                modeCheckBox.setVisibility(View.VISIBLE);
                titleBar.setBackgroundResource(R.color.bg);
                inView = mainMovieView;
                break;
            case R.id.user_center:
                ZhugeTool.INSTANCE.track("个人中心", null);
                titleText.setText("账号");
                if (mainUserView == null) {
                    initUserView();
                }
                search.setVisibility(View.GONE);
                setting.setVisibility(View.VISIBLE);
                movieSort.setVisibility(View.GONE);
                modeCheckBox.setVisibility(View.GONE);
                titleBar.setBackgroundResource(R.drawable.toolbar_bg);
                inView = mainUserView;
                break;
        }
        if (inView != null) {
            inView.setVisibility(View.GONE);
        }
        View outView = getCurrentShowTab();
        switchByAnimation(inView, outView);
        if (outView == mainDiscoverView) {
            TCAgent.onPageEnd(this, "发现");
        } else if (outView == mainMovieView) {
            int id = mainMovieView.radioGroup.getCheckedRadioButtonId();
            if (id == 1) {
                TCAgent.onPageEnd(this, "想看");
            } else if (id == 2) {
                TCAgent.onPageEnd(this, "已看");
            } else {
                TCAgent.onPageEnd(this, "自建影单");
            }
        }
        if (Session.map.containsKey("hint_dialog")) {
            Dialog dialog = (Dialog) Session.map.remove("hint_dialog");
            try {
                if (dialog.isShowing()) dialog.dismiss();
            } catch (Exception ignore) {
            }
        }
    }

    public void switchByAnimation(final View inView, final View outView) {
        if (outView != null) {
            Session.isAnimating = true;
            PropertyValuesHolder outScaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.9f);
            PropertyValuesHolder outScaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.9f);
            PropertyValuesHolder outAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
            ObjectAnimator outAnimator = ObjectAnimator
                    .ofPropertyValuesHolder(outView, outScaleX, outScaleY, outAlpha).setDuration(200);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (inView == null) {
                        Session.isAnimating = false;
                        outView.setVisibility(View.GONE);
                        return;
                    }
                    showInView(inView, outView);
                }
            });
            outAnimator.setInterpolator(new LinearInterpolator());
            outAnimator.start();
        } else if (inView != null) {
            Session.isAnimating = true;
            showInView(inView, outView);
        }
    }

    private void showInView(View inView, final View outView) {
        inView.setVisibility(View.VISIBLE);
        PropertyValuesHolder inScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.9f, 1f);
        PropertyValuesHolder inScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.9f, 1f);
        PropertyValuesHolder inAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        ObjectAnimator inAnimator = ObjectAnimator
                .ofPropertyValuesHolder(inView, inScaleX, inScaleY, inAlpha).setDuration(200);
        inAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Session.isAnimating = false;
                if (outView != null) {
                    outView.setVisibility(View.GONE);
                }
            }
        });
        inAnimator.setInterpolator(new LinearInterpolator());
        inAnimator.start();
    }

    public View getCurrentShowTab() {
        if (mainDiscoverView != null && mainDiscoverView.isShown()) return mainDiscoverView;
        if (mainMovieView != null && mainMovieView.isShown()) return mainMovieView;
        return mainUserView;
    }

    public void updateData(boolean isRetry) {
        if (!isRetry) {
            if (!SettingManager.getInstance().needUpdateData()) return;
            DialogTool.showWaitDialog("数据升级中");
        }
        NetManager.getInstance().syncUserData(new ArrayList<NameValuePair>(), new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                SettingManager.getInstance().setNeedUpdateData(false);
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                ThreadManager.getInstance().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateData(true);
                    }
                }, 1000);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mainUserView != null) mainUserView.onRequestPermissionsSuccess();
            } else {
                ToastTool.show("获取权限失败");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (Session.isAnimating) return;
        View view = ((ViewGroup) rootView).getChildAt(((ViewGroup) rootView).getChildCount() - 1);
        if (view instanceof MovieSearchView) {
            ((MovieSearchView) view).exit();
        } else if (view.getId() == R.id.multi_mode_tab) {
            sendBroadcast(new Intent(MainMovieView.Companion.getEXIT_MULTI_CHOICE_ACTION()));
        } else if (view instanceof MainMovieView.SortDialog) {
            ((ViewGroup) rootView).removeView(view);
        } else {
            if (System.currentTimeMillis() - lastPressTime < 2000) {
                super.onBackPressed();
            } else {
                ToastTool.show("再按一次退出程序");
                lastPressTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            DailyCardActivity.Companion.setLastP(1);
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
            if (mainMovieView != null) {
                MovieTodoView.Companion.getMyCursor().close();
                MovieDoneView.Companion.getMyCursor().close();
            }
            ZhugeTool.INSTANCE.onDestroy();
            ThreadManager.shutDown();
            NetManager.shutDown();
//            Http.shutdown();
            checkMovieListCache();
        } catch (Exception ignore) {
        }
    }

    @Override
    public void finish() {
        customAnimation = false;
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (UmengTool.isInited())
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void checkMovieListCache() {
        ThreadManager.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                File[] cacheFiles = MovieListKey.Companion.getCachedFiles();
                if (cacheFiles != null && cacheFiles.length > 30) {
                    List<File> sortedFiles = new ArrayList();
                    for (int i = 0; i < cacheFiles.length; i++) {
                        File file = cacheFiles[i];
                        String pubTime = file.getName().split("_")[0];
                        if (!pubTime.equals(DiskLruCache.JOURNAL_FILE)) {
                            sortedFiles.add(file);
                        }
                    }
                    if (sortedFiles.size() > 30) {
                        Collections.sort(sortedFiles, new Comparator<File>() {
                            @Override
                            public int compare(File lhs, File rhs) {
                                return rhs.getName().split("_")[0].compareTo(lhs.getName().split("_")[0]);
                            }
                        });
                        for (int i = 30; i < sortedFiles.size(); i++) {
                            sortedFiles.get(i).delete();
                        }
                    }
                }
            }
        });
    }
}
