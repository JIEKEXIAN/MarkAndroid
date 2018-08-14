package com.intlime.mark.activitys;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.intlime.mark.R;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.tools.LogTool;
import com.intlime.mark.tools.ToastTool;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.TbsVideo;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebBackForwardList;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.net.URLEncoder;

//import android.webkit.CookieManager;
//import android.webkit.CookieSyncManager;
//import android.webkit.ValueCallback;
//import android.webkit.WebBackForwardList;
//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;

/**
 * Created by root on 15-12-21.
 */
public class WebActivity extends BaseActivity {
    private static int UPLOAD_CODE = 111;

    private WebView webView;
    private View mView;
    private WebChromeClient chromeClient;
    private ProgressBar progressBar;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;

    private String title;
    private String url;
    private int type;//0为普通；1为百度离线操作；2为百度登陆成功后重新离线的状态；3为可复制链接
    private final static String baiduYunUrl = Session.isRelease ? "http://mark.intlime.com/baidu/download" : "http://marktest.intlime.com/baidu/download";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            finish();
            ToastTool.show("链接有误");
            return;
        }
        title = getIntent().getStringExtra("title");
        type = getIntent().getIntExtra("type", 0);
        setContentView(R.layout.activity_web_layout);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(R.drawable.back_icon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (type == 3) {
            registerForContextMenu(progressBar);
            toolbar.getMenu().add("分享").setIcon(R.drawable.multi_share_icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    openContextMenu(progressBar);
                    return true;
                }
            });
        }
    }

    @Override
    protected void initOther() {
        webView = (WebView) findViewById(R.id.webView);
//        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings webSettings = webView.getSettings();

        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);

        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webSettings.setJavaScriptEnabled(true);

//        if (webSettings.getUserAgentString().contains("TBS")) {
//            android.webkit.WebView sysWebView = new android.webkit.WebView(AppEngine.getContext());
//            webSettings.setUserAgentString(sysWebView.getSettings().getUserAgentString());
//            sysWebView.destroy();
//        }

        webView.setWebViewClient(new MyWebViewClient());
        chromeClient = new MyWebChromeClient();
        webView.setWebChromeClient(chromeClient);

        if (type == 1) {
            webView.loadUrl(getBaiduUrl(url));
        } else {
            webView.loadUrl(url);
        }
    }

    @NonNull
    private String getBaiduUrl(String url) {
        return baiduYunUrl + "/durl/" + URLEncoder.encode(url) + "/cookie/"
                + URLEncoder.encode(SettingManager.getInstance().getBaiduYunCookie());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (menu == null || v == null) return;
        menu.add(0, 0, 0, "在浏览器中打开");
        menu.add(0, 1, 0, "复制链接");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            try {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                ToastTool.show("打开失败");
            }
        } else {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setText(url);
            ToastTool.show("已复制到剪切板");
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            ViewParent parent = webView.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
                webView.destroy();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mView != null) {
            chromeClient.onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(params);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            toolbar.setVisibility(View.GONE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {//竖屏
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(params);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        private String intentUrl;

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LogTool.d("shouldOverrideUrl", url);
            if (url.endsWith(".mp4") || url.endsWith(".3gp") || url.endsWith(".rmvb")) {
                try {
                    TbsVideo.openVideo(WebActivity.this, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            progressBar.setProgress(0);
            progressBar.setAlpha(1);
            if (intentUrl != null && url.equals(intentUrl)) {
                view.goBack();
                return true;
            }
            if (url.startsWith("intent://")
                    || url.startsWith("douban://")
                    || url.startsWith("tenvideo2://")
                    || url.startsWith("youku://")
                    || url.startsWith("tudou://")
                    || url.startsWith("qiyimobile://")
                    || url.startsWith("letvclient://")
                    || url.startsWith("sohuvideo://")
                    || url.startsWith("bilibili://")
                    || url.startsWith("acfun://")
                    || url.startsWith("imgotv://")
                    || url.startsWith("pptv://")) {
                try {
                    intentUrl = url;
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ("http://pan.baidu.com/wap/home".equals(url)) {
                CookieSyncManager.createInstance(getApplicationContext());
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setCookie(url, SettingManager.getInstance().getBaiduYunCookie());
                CookieSyncManager.getInstance().sync();
            }
            return false;
        }


        @Override
        public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
            if (type == 1 && "http://pan.baidu.com/wap/home".equals(s)) {
                type = 2;
                SettingManager.getInstance().setBaiduYunCookie(CookieManager.getInstance().getCookie(s));
                WebBackForwardList backForwardList = webView.copyBackForwardList();
                int cIndex = backForwardList.getCurrentIndex();
                for (int i = 0; i < cIndex; i++) {
                    if ("http://pan.baidu.com/".equals(backForwardList.getItemAtIndex(i).getUrl())) {
                        webView.loadUrl(getBaiduUrl(url));
                        return;
                    }
                }
            }
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        private View bgView;
        private IX5WebChromeClient.CustomViewCallback mCallback;

        @Override
        public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
            try {
                if (mCallback != null) {
                    mCallback.onCustomViewHidden();
                    mCallback = null;
                    return;
                }

                ViewGroup viewGroup = (ViewGroup) rootView;
                bgView = new View(viewGroup.getContext());
                bgView.setClickable(true);
                bgView.setBackgroundColor(Color.BLACK);
                viewGroup.addView(bgView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                viewGroup.removeView(webView);
                viewGroup.addView(view);
                ((RelativeLayout.LayoutParams) view.getLayoutParams()).addRule(RelativeLayout.CENTER_IN_PARENT);
                mView = view;
                mCallback = callback;
            } catch (Exception ignore) {
            }
        }

        @Override
        public void onHideCustomView() {
            try {
                if (mView != null) {
                    if (mCallback != null) {
                        mCallback.onCustomViewHidden();
                        mCallback = null;
                    }

                    ViewGroup viewGroup = (ViewGroup) rootView;
                    viewGroup.removeView(bgView);
                    viewGroup.removeView(mView);
                    viewGroup.addView(webView);
                    mView = null;
                    bgView = null;
                }
            } catch (Exception ignore) {
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (progressBar.getProgress() < newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.postInvalidate();
                if (progressBar.getProgress() == 100) {
                    ObjectAnimator.ofFloat(progressBar, "alpha", 0f).setDuration(500).start();
                }
            }
            super.onProgressChanged(view, newProgress);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
            }
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(
                    Intent.createChooser(intent, "完成操作需要使用"),
                    UPLOAD_CODE);

        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {
            if (mUploadCallbackAboveL != null) {
                mUploadCallbackAboveL.onReceiveValue(null);
            }
           mUploadCallbackAboveL=valueCallback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(
                    Intent.createChooser(intent, "完成操作需要使用"),
                    UPLOAD_CODE);
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == UPLOAD_CODE) {
            if (null == mUploadMessage && null==mUploadCallbackAboveL)
                return;
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            if (mUploadCallbackAboveL!=null){
                Uri[] results=null;
                if (resultCode == Activity.RESULT_OK){
                   String dataString=intent.getDataString();
                    ClipData clipData = intent.getClipData();
                    if (clipData!=null){
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0;i<clipData.getItemCount();i++){
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    if (dataString!=null){
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                mUploadCallbackAboveL.onReceiveValue(results);
                mUploadCallbackAboveL=null;
            }else if (mUploadMessage!=null) {
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }
}
