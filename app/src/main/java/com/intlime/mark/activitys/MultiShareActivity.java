package com.intlime.mark.activitys;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.DensityUtils;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ImageTool;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.UmengTool;
import com.intlime.mark.tools.ZhugeTool;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlin.Pair;

/**
 * Created by root on 16-1-15.
 */
public class MultiShareActivity extends BaseActivity {
    private String errorMsg = "存储空间不足";
    private String DCIM_PATH;//DCIM文件夹路径
    private String tempPath;
    private File tempFile;
    private WebView webView;
    private List<MovieBean> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            list = savedInstanceState.getParcelableArrayList("list");
        }
        setContentView(R.layout.activity_multi_share_layout);

        final GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
        int itemWidth = (WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(this, 30)) / 4;
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).getLayoutParams().width = itemWidth;
        }
        gridLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                gridLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                View view = findViewById(R.id.bottom_space);
                view.getLayoutParams().height = ((View) gridLayout.getParent())
                        .getMeasuredHeight() + DensityUtils.dp2px(getApplicationContext(), 10);
                return true;
            }
        });
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle("电影");
        toolbar.setNavigationIcon(R.drawable.back_icon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("list", getIntent().getParcelableArrayListExtra("list"));
    }

    @Override
    protected void initOther() {
        if (UmengTool.checkPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
            doPrepare();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doPrepare();
            } else {
                ToastTool.show("获取权限失败");
            }
        }
    }

    private void doPrepare() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            DCIM_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DCIM + "/Mark";
            File dir = new File(DCIM_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            tempPath = getExternalCacheDir().getAbsolutePath();
            dir = new File(tempPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            ToastTool.show(errorMsg, Toast.LENGTH_LONG, 0);
            finish();
            return;
        }
        webView = (WebView) findViewById(R.id.webView);
        prepare();
    }

    private void prepare() {
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onFail(ArrayMap result, int error_code) {
                finish();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                List<String[]> infos = (List<String[]>) result.get("movies");
                if (list.size() != infos.size()) {
                    ToastTool.show(errorMsg, Toast.LENGTH_LONG, 0);
                    finish();
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        MovieBean bean = list.get(i);
                        bean.setDirectors(infos.get(i)[0]);
                        bean.setSummary(infos.get(i)[1]);
                    }
                    drawBitmap(list);
                }
            }
        };
        DialogTool.showWaitDialog("请稍等", DialogTool.FINISH_ON_BACK, callback);
        if (list == null) {
            list = getIntent().getParcelableArrayListExtra("list");
        }
        NetManager.getInstance().getMovieShareInfo(list, callback);
    }

    public void onClick(View view) {
        if (view.getId() != R.id.cancel) {
            ZhugeTool.INSTANCE.track("点击电影分享", ZhugeTool.INSTANCE.getTrackArg(new Pair<>("操作", "多选")));
        }
        switch (view.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.weixin:
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                    intent.setType("image/*");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show("分享失败");
                }
                break;
            case R.id.weixin_circle:
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI"));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                    intent.setType("image/*");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show("分享失败");
                }
                break;
            case R.id.qq:
                try {
                    UmengTool.getInstance();
                    new ShareAction(this)
                            .setPlatform(SHARE_MEDIA.QQ)
                            .withMedia(new UMImage(this, tempFile))
                            .share();
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show("分享失败");
                }
                break;
            case R.id.weibo:
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.sina.weibo", "com.sina.weibo.composerinde.ComposerDispatchActivity"));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                    intent.setType("image/*");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show("分享失败");
                }
                break;
            case R.id.more:
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, "分享"));
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show("分享失败");
                }
                break;
            case R.id.save_pic:
                try {
                    File savedFile = new File(DCIM_PATH, tempFile.getName());
                    ImageTool.copyFile(tempFile, savedFile);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(savedFile)));
                    ToastTool.show("已成功保存到" + savedFile.getAbsolutePath(), 0, Toast.LENGTH_LONG);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastTool.show(errorMsg, Toast.LENGTH_LONG, 0);
                }
                break;
        }
    }

    private void drawBitmap(final List<MovieBean> list) {
        Paint paint = new Paint(); // 建立画笔
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.parseColor("#aaaaaa"));

        final float width = 960;
        float rightMargin = 38;
        float leftMargin = 38;

        float contentHeight = 0;
        for (MovieBean bean : list) {
            textPaint.setTextSize(46);
            StaticLayout titleLayout = new StaticLayout(bean.getName(),
                    textPaint, (int) (width - rightMargin - leftMargin), Layout.Alignment.ALIGN_CENTER, 1F, 0F, true);
            textPaint.setTextSize(33);
            StaticLayout directorLayout = new StaticLayout(bean.getDirectors(),
                    textPaint, (int) (width - rightMargin - leftMargin), Layout.Alignment.ALIGN_CENTER, 1F, 0F, true);
            contentHeight += 882.04004f;
            if (titleLayout.getLineCount() > 1) {
                contentHeight += (float) titleLayout.getHeight() - 64f;
            }
            if (directorLayout.getLineCount() > 1) {
                contentHeight += (float) directorLayout.getHeight() - 44f;
            }
        }
        float logoHeight = 138.24f;
        final float height = contentHeight + logoHeight;
        Bitmap toShareBitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(toShareBitmap);
        canvas.drawColor(Color.WHITE);

        //bottom logo
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inDensity = AppEngine.getContext().getResources().getDisplayMetrics().densityDpi;
        o.inScreenDensity = AppEngine.getContext().getResources().getDisplayMetrics().densityDpi;
        o.inTargetDensity = AppEngine.getContext().getResources().getDisplayMetrics().densityDpi;
        Bitmap logo = ImageTool.getBitmap(R.drawable.muti_share_logo, o);
        canvas.drawBitmap(logo, 38, contentHeight + 47, paint);
        textPaint.setTextSize(33.28f);
        canvas.drawText("Mark-我的电影清单", 122, contentHeight + 80.64f, textPaint);

        float imgWidth = 321;
        float imgHeight = 472.32f;
//        float imgRadius = 10.24f;
        int titleColor = Color.parseColor("#323232");
        int directorColor = Color.parseColor("#959595");
        int summaryColor = Color.parseColor("#262626");
        int lineColor = Color.parseColor("#e2e2e2");
        float baseY = 0;
        for (int i = 0; i < list.size(); i++) {
            MovieBean bean = list.get(i);

            //电影名
            textPaint.setColor(titleColor);
            textPaint.setTextSize(46);
            if (TextUtils.isEmpty(bean.getName())) {
                bean.setName("None");
            }
            baseY = drawText(bean.getName(), rightMargin, baseY + 84.84f,
                    width - rightMargin - leftMargin, Layout.Alignment.ALIGN_CENTER, 1f, canvas, textPaint);

            //导演名
            textPaint.setColor(directorColor);
            textPaint.setTextSize(33);
            if (TextUtils.isEmpty(bean.getDirectors())) {
                bean.setDirectors("None");
            }
            baseY = drawText(bean.getDirectors(), rightMargin, baseY + 23.04f,
                    width - rightMargin - leftMargin, Layout.Alignment.ALIGN_CENTER, 1f, canvas, textPaint);

            //内容
            float summaryWidth = 506;
            textPaint.setColor(summaryColor);
            textPaint.setTextSize(33);
            String toCutText = bean.getSummary();
            String summary = toCutText;
            int end = toCutText.length();
            int offset = 0;
            while (true) {
                StaticLayout layout = new StaticLayout(summary, textPaint,
                        (int) summaryWidth, Layout.Alignment.ALIGN_NORMAL, 0, 0f, true);
                if (layout.getLineCount() > 9) {
                    if (offset == 0) {
                        end = layout.getLineEnd(9);
                    }
                    offset++;
                    summary = String.format("%s...", toCutText.substring(0, end - offset));
                } else {
                    break;
                }
            }
            baseY += 83.2f;
            drawText(summary, 416, baseY,
                    summaryWidth, Layout.Alignment.ALIGN_NORMAL, 1.3f, canvas, textPaint);

            //图片
            try {
                Bitmap image = Glide.with(AppEngine.getContext())
                        .load(bean.getImage())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into((int) imgWidth, (int) imgHeight)
                        .get();
                if (image != null) {
                    paint.setColor(Color.parseColor("#ededed"));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1);
                    canvas.drawRect(new RectF(leftMargin - 1, baseY - 1,
                            leftMargin + imgWidth + 1, baseY + imgHeight + 1), paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(0);
                    image = ImageTool.getCropBitmap(null, image, (int) imgWidth, (int) imgHeight);
                    canvas.drawBitmap(image, leftMargin, baseY, paint);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            baseY += imgHeight;

            //分隔线
            textPaint.setColor(lineColor);
            textPaint.setStrokeWidth(2);
            baseY += 110.08f;
            canvas.drawLine(leftMargin, baseY, width - rightMargin, baseY, textPaint);
            textPaint.setStrokeWidth(0);
            baseY += 2.56f;
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String fileName = formatter.format(curDate);//用日期作文件名
            tempFile = new File(tempPath + "/" + fileName + ".png");
//            tempFile = new File(AppEngine.getContext().getCacheDir() + "/temp.png");
            FileOutputStream fOut = new FileOutputStream(tempFile);
            toShareBitmap.compress(Bitmap.CompressFormat.PNG, 80, fOut);
            fOut.flush();
            fOut.close();
            toShareBitmap.recycle();
            toShareBitmap = null;
            System.gc();
            ThreadManager.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    int webViewWidth = webView.getMeasuredWidth();
                    int webViewHeight = 0;
                    if (webViewWidth <= 0) {
                        webViewHeight = ViewGroup.LayoutParams.MATCH_PARENT;
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            webViewWidth *= 0.98;
                        }
                        float wScale = (float) webViewWidth / width;
                        webViewHeight = (int) (wScale * height);
                    }
                    webView.getLayoutParams().height = webViewHeight;
                    webView.setLayoutParams(webView.getLayoutParams());

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setSupportZoom(false);
                    webSettings.setUseWideViewPort(true);
                    webSettings.setLoadWithOverviewMode(true);
                    webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                    webView.loadUrl(Uri.fromFile(tempFile).toString());
                    DialogTool.dismissWaitDialog();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            ToastTool.show(errorMsg, Toast.LENGTH_LONG, 0);
            finish();
        }
    }

    /**
     * @return text的bottom
     */
    private static float drawText(String text, float dx, float dy,
                                  float lineWidth, Layout.Alignment alignment, float spacingmult, Canvas canvas, TextPaint paint) {
        StaticLayout layout = new StaticLayout(text,
                paint, (int) lineWidth, alignment, spacingmult, 0F, true);
        canvas.save();
        canvas.translate(dx, dy);//从dx, dy开始画
        layout.draw(canvas);
        canvas.restore();//别忘了restore
        return layout.getHeight() + dy;
    }

    @Override
    public void finish() {
        DialogTool.dismissWaitDialog();
        super.finish();
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
        if (!TextUtils.isEmpty(tempPath)) {
            File tempDir = new File(tempPath);
            if (tempDir.isDirectory()) {
                File[] files = tempDir.listFiles();
                if (files == null) return;
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (UmengTool.isInited())
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }
}
