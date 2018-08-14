package com.intlime.mark.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.JavascriptInterface;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.JsonTool;
import com.intlime.mark.tools.LogTool;
import com.intlime.mark.tools.StringTool;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.UmengTool;
import com.intlime.mark.tools.ZhugeTool;
import com.intlime.mark.tools.db.MovieDbManager;
import com.intlime.mark.view.widget.MovieStatusView;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.tendcloud.tenddata.TCAgent;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;

/**
 * Created by root on 16-1-29.
 */
public class MovieListDetailActivity extends BaseActivity {
    private boolean activityStackSortChanged;
    private MovieListBean bean;
    private MovieBean handlingMovieBean;
    private WebView webView;
    private TextView likes;
    private TextView comment;
    private TextView shares;
    private int lastScrollY;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (RELOAD_ALL_ACTION.equals(intent.getAction())) {
                    MovieBean bean = intent.getParcelableExtra(BEAN);
                    if (bean != null) movieStatusCallBack(bean);
                } else {
                    if (handlingMovieBean != null) {
                        int isDone = intent.getIntExtra("is_done", handlingMovieBean.getDone());
                        if (handlingMovieBean.getDone() != isDone) {
                            handlingMovieBean.setDone(isDone);
                            movieListAdd(handlingMovieBean);
                        }
                    }
                    MovieListBean temp = intent.getParcelableExtra(BEAN);
                    if (temp != null) {
                        bean.setComments(temp.getComments());
                        comment.setText(Integer.toString(bean.getComments()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        customAnimation = getIntent().getBooleanExtra(ANIMATION, true);
        super.onCreate(savedInstanceState);
        if (Session.uid <= 0) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        bean = getIntent().getParcelableExtra(BEAN);
        if (bean == null || bean.getId() <= 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_movie_list_detail_layout);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RELOAD_ALL_ACTION);
        filter.addAction(RELOAD_SINGLE_ACTION);
        registerReceiver(broadcastReceiver, filter);
        ZhugeTool.INSTANCE.track("进入影单详情", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("影单名称", bean.getName())));

        if ("from_push".equals(getIntent().getAction())) {
            NetManager.getInstance().movieListPushOpens(bean.getId(), new NetRequestCallBack());
        }
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle("影单详情");
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
        initWebView();
        likes = (TextView) findViewById(R.id.likes);
        comment = (TextView) findViewById(R.id.comments);
        shares = (TextView) findViewById(R.id.shares);
        loadData();
    }

    private void initWebView() {
        webView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setSupportZoom(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String s) {
                webView.loadUrl("javascript:" +
                        "function click(text){" +
                        "    window.single.getData(text);" +
                        "}");
                if (lastScrollY > 0) {
                    webView.getView().scrollTo(0, lastScrollY);
                }
            }
        });
        webView.addJavascriptInterface(this, "single");
    }

    private void loadData() {
        NetRequestCallBack callBack = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                if (webView == null) return;
                if (!result.containsKey("movie_list")) return;
                bean = (MovieListBean) result.get("movie_list");
                if (!TextUtils.isEmpty(bean.getHtmlCode())) {
                    webView.loadDataWithBaseURL(null, bean.getHtmlCode(), "text/html", "utf-8", null);
                }
                likes.setText(Integer.toString(bean.getLikes()));
                if (bean.getLiked() == 1) {
                    likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_checked, 0, 0);
                } else {
                    likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_unchecked, 0, 0);
                }
                comment.setText(Integer.toString(bean.getComments()));
                shares.setText(Integer.toString(bean.getShares()));
            }
        };
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callBack);
        lastScrollY = webView.getView().getScrollY();
        NetManager.getInstance().getMovieListDetail(bean.getId(), callBack);
    }

    public void onClick(View view) {
        if (bean == null) return;
        if (view.getId() == R.id.likes) {
            if (bean.getLiked() == 1) {
                bean.setLiked(0);
                bean.setLikes(bean.getLikes() - 1);
                likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_unchecked, 0, 0);
            } else {
                bean.setLiked(1);
                bean.setLikes(bean.getLikes() + 1);
                likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_checked, 0, 0);
                ZhugeTool.INSTANCE.track("影单点赞", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("影单名称", bean.getName())));
            }
            likes.setText(Integer.toString(bean.getLikes()));
            NetManager.getInstance().likeMovieList(bean.getId(), bean.getLiked() == 1 ? 1 : 2,
                    new NetRequestCallBack() {
                        @Override
                        public void onSuccess(ArrayMap result) {
                            Intent intent = new Intent(RELOAD_DISCOVER_ACTION);
                            intent.putExtra(BEAN, bean);
                            sendBroadcast(intent);
                            Intent intent2 = new Intent(RELOAD_DISCOVER_ACTION);
                            intent2.putExtra(BEAN, bean);
                            intent2.putExtra("type", 2);
                            sendBroadcast(intent2);
                        }

                        @Override
                        public void onFail(ArrayMap result, int error_code) {
                            ThreadManager.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    if (bean.getLiked() == 1) {
                                        bean.setLiked(0);
                                        bean.setLikes(bean.getLikes() - 1);
                                        likes.setText(Integer.toString(bean.getLikes()));
                                        likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_unchecked, 0, 0);
                                    } else {
                                        bean.setLiked(1);
                                        bean.setLikes(bean.getLikes() + 1);
                                        likes.setText(Integer.toString(bean.getLikes()));
                                        likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.movie_list_detail_like_checked, 0, 0);
                                    }
                                }
                            });
                        }
                    });
        } else if (view.getId() == R.id.comments) {
            Intent intent = new Intent(this, MovieListCommentsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra(BEAN, bean);
            startActivity(intent);
        } else {
            if (UmengTool.checkPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                DialogTool.showShareDialog(this, new ShareListener());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DialogTool.showShareDialog(this, new ShareListener());
            } else {
                ToastTool.show("获取权限失败");
            }
        }
    }

    @JavascriptInterface
    public void getData(String text) {
        try {
            JSONObject data = new JSONObject(text);
            String type = JsonTool.optString(data, "clicktype");
            if (type.equals("href")) {
                Intent intent = new Intent(AppEngine.getContext(), WebActivity.class);
                intent.putExtra("url", JsonTool.optString(data, "url"));
                startActivity(intent);
            } else if (type.equals("img")) {
                handleImgClick(data);
            } else if (type.equals("leftbtn")) {
                handleLeftClick(data);
            } else if (type.equals("rightbtn")) {
                handleRightClick(data);
            } else if (type.equals("stage_photo")) {
                handleStagePhoto(data);
            } else if (type.equals("writerclick")) {
                startActivity(new Intent(this, WriterActivity.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogTool.d("movieList getData", text);
    }

    private void handleImgClick(JSONObject data) {
        Intent intent = new Intent(AppEngine.getContext(), MovieDetailActivity.class);
        if (data.optInt("is_done") == -1) {
            handlingMovieBean = new MovieBean();
            handlingMovieBean.setName(JsonTool.optString(data, "name"));
            handlingMovieBean.setDb_num(JsonTool.optString(data, "db_num"));
            intent.putExtra("type", 1);
        } else {
            handlingMovieBean = MovieDbManager.getInstance().get(data.optInt("movie_id"));
            if (handlingMovieBean == null) return;
        }
        intent.putExtra(BEAN, handlingMovieBean);
        startActivity(intent);
    }

    private void handleLeftClick(final JSONObject data) {
        handlingMovieBean = new MovieBean();
        List<NameValuePair> params = new ArrayList<>();
        int isDone = data.optInt("is_done");
        if (isDone == -1) {
            NetRequestCallBack callback = new NetRequestCallBack() {
                @Override
                public void onDefault() {
                    DialogTool.dismissWaitDialog();
                }

                @Override
                public void onSuccess(ArrayMap result) {
                    MovieBean temp = (MovieBean) result.get("bean");
                    handlingMovieBean.setId(temp.getId());
                    handlingMovieBean.setDb_rating(temp.getDb_rating());
                    handlingMovieBean.setPubdate(temp.getPubdate());
                    handlingMovieBean.setDuration(temp.getDuration());
                    handlingMovieBean.setMovieType(temp.getMovieType());
                    handlingMovieBean.setUpdate_time(temp.getUpdate_time());
                    handlingMovieBean.setPubdateTimestamp(temp.getPubdateTimestamp());
                    handlingMovieBean.setImage(JsonTool.optString(data, "img_url"));
                    MovieDbManager.getInstance().insert(handlingMovieBean);
                    sendBroadcast(new Intent(BaseActivity.RELOAD_ALL_ACTION));
                    movieStatusCallBack(handlingMovieBean);
                    movieListAdd(handlingMovieBean);
                }
            };
            DialogTool.showWaitDialog("请稍等", DialogTool.CANCEL_ON_BACK, callback);
            handlingMovieBean.setName(JsonTool.optString(data, "name"));
            handlingMovieBean.setDb_num(JsonTool.optString(data, "db_num"));
            params.add(new BasicNameValuePair("db_num", handlingMovieBean.getDb_num()));
            NetManager.getInstance().addMovie(params, callback);
        } else {
            handlingMovieBean.setId(data.optInt("movie_id"));
            handlingMovieBean.setDb_num(JsonTool.optString(data, "db_num"));
            handlingMovieBean.setDone(isDone);
            openMovieStatusView();
        }
    }

    private void handleRightClick(JSONObject data) {
        handlingMovieBean = new MovieBean();
        handlingMovieBean.setId(data.optInt("movie_id"));
        handlingMovieBean.setName(JsonTool.optString(data, "name"));
        handlingMovieBean.setImage(JsonTool.optString(data, "img_url"));
        handlingMovieBean.setDb_num(JsonTool.optString(data, "db_num"));
        handlingMovieBean.setDone(data.optInt("is_done"));
        openMovieStatusView();
    }

    private void handleStagePhoto(JSONObject data) {
        JSONArray jsonArray = data.optJSONArray("urls");
        if (jsonArray != null) {
            int position = 0;
            String picUrl = JsonTool.optString(data, "img_url");
            ArrayList<String> picList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String temp = jsonArray.optString(i);
                if (picUrl.equals(temp)) {
                    position = i;
                }
                picList.add(temp);
            }
            Intent intent = new Intent(this, WebPictureActivity.class);
            intent.putExtra("list", picList);
            intent.putExtra("position", position);
            startActivity(intent);
        }
    }

    private void movieStatusCallBack(final MovieBean bean) {
        ThreadManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (webView == null) return;
                webView.loadUrl(String.format("javascript:setMovieStatus(%d,%d)", bean.getId(), bean.getDone()));
            }
        });
    }

    private void openMovieStatusView() {
        if (handlingMovieBean == null) return;
        int type = 0;
        if (handlingMovieBean.getDone() == -1) {
            type = 1;
        } else {
            handlingMovieBean = MovieDbManager.getInstance().get(handlingMovieBean.getId());
            if (handlingMovieBean == null) return;
            type = 0;
        }
        final int finalType = type;
        ThreadManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                final int[] isDone = {handlingMovieBean.getDone()};
                MovieStatusView.INSTANCE.show(handlingMovieBean, finalType, null, null, new MovieStatusView.OnStatusChangeListener() {
                    @Override
                    public void onStatusChange(@NotNull MovieBean bean) {
                        if (bean.getId() > 0 && bean.getDone() != isDone[0]) {
                            movieListAdd(bean);
                            isDone[0] = bean.getDone();
                        }
                    }
                });
            }
        });
    }

    private void movieListAdd(@NotNull MovieBean bean) {
        NetManager.getInstance().movieListAdd(MovieListDetailActivity.this.bean.getId(),
                bean.getId(), bean.getDone(), new NetRequestCallBack());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        overridePendingTransition(R.anim.activity_right_in, R.anim.activity_full_left_out);
        activityStackSortChanged = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        TCAgent.onPageStart(this, "single" + getIntent().getIntExtra("id", 0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        TCAgent.onPageEnd(this, "single" + getIntent().getIntExtra("id", 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception ignore) {
            }
        }
        if (webView != null) {
            ViewParent parent = webView.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
                webView.destroy();
            }
        }
        webView = null;
    }

    @Override
    public void finish() {
        customAnimation = true;
        super.finish();
        if ("from_push".equals(getIntent().getAction()) && WWindowManager.getInstance().getWindows().isEmpty()) {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            intent.putExtra(ANIMATION, true);
            startActivity(intent);
        }
        if (activityStackSortChanged) {
            overridePendingTransition(R.anim.activity_full_left_in, R.anim.activity_right_out);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (UmengTool.isInited())
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private class ShareListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            final String imgUrl = StringTool.getQiniuScaledImgUrl(bean.getImg_url(), 300, 160);
            Glide.with(v.getContext())
                    .load(imgUrl)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            ToastTool.show("分享失败，请稍后再试");
                        }

                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            share(v, resource);
                        }
                    });
        }

        private void share(View v, Bitmap resource) {
            try {
                UmengTool.getInstance();
                Activity context = MovieListDetailActivity.this;
                ShareAction action = new ShareAction(context);
                final String shareUrl = "http://mark.intlime.com/singles/share/id/" + bean.getId();
                final String title = bean.getName();
                final String content = "这篇棒棒的影单来自Mark！";
                final UMImage umImage = new UMImage(context, resource);
                switch (v.getId()) {
                    case R.id.weixin:
                        action.setPlatform(SHARE_MEDIA.WEIXIN)
                                .withMedia(umImage)
                                .withTitle(title)
                                .withText(content)
                                .withTargetUrl(shareUrl)
                                .share();
                        movieListShare();
                        break;
                    case R.id.weixin_circle:
                        action.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE)
                                .withMedia(umImage)
                                .withTitle(title)
                                .withText(content)
                                .withTargetUrl(shareUrl)
                                .share();
                        movieListShare();
                        break;
                    case R.id.qq:
                        action.setPlatform(SHARE_MEDIA.QQ)
                                .withMedia(umImage)
                                .withTitle(title)
                                .withText(content)
                                .withTargetUrl(shareUrl)
                                .share();
                        movieListShare();
                        break;
                    case R.id.qq_zone:
                        action.setPlatform(SHARE_MEDIA.QZONE)
                                .withMedia(umImage)
                                .withTitle(title)
                                .withText(content)
                                .withTargetUrl(shareUrl)
                                .share();
                        movieListShare();
                        break;
                    case R.id.weibo:
                        action.setPlatform(SHARE_MEDIA.SINA)
                                .withMedia(umImage)
                                .withText(title + " \n" + shareUrl)
                                .share();
                        movieListShare();
                        break;
                    case R.id.copy_link:
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setText(shareUrl);
                        ToastTool.show("已复制到剪切板");
                        break;
                    case R.id.more:
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_TEXT, title + " " + content);
                        intent.setType("text/*");
                        context.startActivity(Intent.createChooser(intent, null));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                ToastTool.show("分享失败");
            }
        }

        private void movieListShare() {
            NetManager.getInstance().movieListShares(bean.getId(), new NetRequestCallBack() {
                @Override
                public void onSuccess(ArrayMap result) {
                    bean.setShares(bean.getShares() + 1);
                    ThreadManager.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            shares.setText(Integer.toString(bean.getShares()));
                        }
                    });
                }
            });
        }
    }
}