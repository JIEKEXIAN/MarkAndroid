package com.intlime.mark.activitys;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.CryptTool;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ImageTool;
import com.intlime.mark.tools.JsonTool;
import com.intlime.mark.tools.UmengTool;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;

/**
 * Created by root on 16-2-29.
 */
public class LoginActivity extends BaseActivity implements PlatformActionListener{
    private ScrollView scrollView;
    private Animator currentAnimator;
    private int maxScrollY;
    private int scrollType = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        customAnimation = getIntent().getBooleanExtra(ANIMATION, false);
        super.onCreate(savedInstanceState);
        customAnimation = false;
        setContentView(R.layout.activity_login_layout);
    }

    @Override
    protected void initOther() {
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        final ImageView imageView = (ImageView) scrollView.findViewById(R.id.scrolling_bg);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        imageView.setColorFilter(Color.parseColor("#a3000000"));
        try {
            Bitmap bitmap = ImageTool.getBitmap(R.drawable.login_bg, options);
            int width = WWindowManager.getInstance().getWidth();
            float scale = (float) width / bitmap.getWidth();
            imageView.setMinimumHeight((int) (bitmap.getHeight() * scale));
            imageView.setImageBitmap(bitmap);
            scrollView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    scrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                    maxScrollY = imageView.getMeasuredHeight() - scrollView.getMeasuredHeight();
                    ThreadManager.getInstance().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollDown();
                        }
                    }, 500);
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        TextView weibo = (TextView) findViewById(R.id.weibo);
        TextView qq = (TextView) findViewById(R.id.qq);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scrollType != -1) {
            ThreadManager.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (scrollType == 0) {
                        scrollDown();
                    } else if (scrollType == 1) {
                        scrollUp();
                    }
                }
            }, 400);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }

    private void scrollDown() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(scrollView.getScrollY(), maxScrollY)
                .setDuration((long) ((maxScrollY - scrollView.getScrollY()) / (float) maxScrollY * 30000));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                if (scrollView.getScrollY() != value) {
                    scrollView.setScrollY(value);
                }
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            boolean isCanceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                isCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCanceled) {
                    scrollUp();
                }
            }
        });
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
        currentAnimator = valueAnimator;
        scrollType = 0;
    }

    private void scrollUp() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(scrollView.getScrollY(), 0)
                .setDuration((long) ((scrollView.getScrollY()) / (float) maxScrollY * 30000));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                if (scrollView.getScrollY() != value) {
                    scrollView.setScrollY(value);
                }
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            boolean isCanceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                isCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCanceled) {
                    scrollDown();
                }
            }
        });
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
        currentAnimator = valueAnimator;
        scrollType = 1;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close:
                finish();
                break;
            case R.id.register:
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
            case R.id.account_login:
                intent = new Intent(this, AccountLoginActivity.class);
                startActivity(intent);
                break;
            case R.id.weixin:
                doWeixinLogin();
                break;
            case R.id.qq:
                doQQLogin();
                break;
            case R.id.weibo:
                doWeiboLogin();
                break;
        }
    }

    private void doWeixinLogin() {
        UmengTool.getInstance().doThirdPartLogin(this, SHARE_MEDIA.WEIXIN, new UMAuthListener() {
            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                if (map != null && map.containsKey("unionid") && map.containsKey("nickname") && map.containsKey("headimgurl")) {
                    String udid = map.get("unionid");
                    final String nickname = map.get("nickname");
                    final String headImgUrl = map.get("headimgurl");
                    doLogin(udid, nickname, headImgUrl, "1");
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
            }
        });
    }

    private void doQQLogin() {
        Platform platform=ShareSDK.getPlatform(QQ.NAME);
        if (platform.isAuthValid()){
            String token = platform.getDb().getToken();
            String userId = platform.getDb().getUserId();
            String name = platform.getDb().getUserName();
            String gender = platform.getDb().getUserGender();
            String headImageUrl = platform.getDb().getUserIcon();
        }
        platform.SSOSetting(false);
        platform.setPlatformActionListener(LoginActivity.this);
        platform.showUser(null);
    }

    private void doWeiboLogin() {
        UmengTool.getInstance().doThirdPartLogin(this, SHARE_MEDIA.SINA, new UMAuthListener() {

            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                if (map != null && map.containsKey("result")) {
                    try {
                        JSONObject object = new JSONObject(map.get("result"));
                        String udid = JsonTool.optString(object, "id");
                        final String nickname = JsonTool.optString(object, "screen_name");
                        final String headImgUrl = JsonTool.optString(object, "profile_image_url");
                        if (TextUtils.isEmpty(udid)) return;
                        doLogin(udid, nickname, headImgUrl, "3");
                    } catch (Exception ignore) {
                    }
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
            }
        });
    }

    private void doLogin(String udid, String nickname, String headImgUrl, String type) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("udid", CryptTool.encrypt(udid)));
        params.add(new BasicNameValuePair("nickname", nickname));
        params.add(new BasicNameValuePair("img_url", headImgUrl));
        params.add(new BasicNameValuePair("type", type));
        DialogTool.showWaitDialog("正在登录");
        NetManager.getInstance().thirdPartLogin(params, new NetRequestCallBack() {
            @Override
            public void onSuccess(ArrayMap result) {
                AccountLoginActivity.syncData(new NetRequestCallBack() {
                    @Override
                    public void onSuccess(ArrayMap result) {
                        Intent intent = new Intent(AppEngine.getContext(), MainActivity.class);
                        intent.putExtra(ANIMATION, true);
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                DialogTool.dismissWaitDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (UmengTool.isInited())
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        String headImageUrl = null;//头像
        String userId;//userId
        String token;//token
        String gender;//性别
        String name = null;//用户名
        if (i==Platform.ACTION_USER_INFOR){
            if (platform.getName().equals(QQ.NAME)){
                token=platform.getDb().getToken();
                userId=platform.getDb().getUserId();
                name = platform.getDb().getUserName();
                headImageUrl = platform.getDb().getUserIcon();
                doLogin(userId,name,headImageUrl,"2");
            }
        }
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {

    }

    @Override
    public void onCancel(Platform platform, int i) {

    }
}
