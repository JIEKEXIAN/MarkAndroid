package com.intlime.mark.activitys;

import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.igexin.sdk.PushManager;
import com.intlime.mark.R;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.LogTool;
import com.intlime.mark.tools.ZhugeTool;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by root on 15-11-5.
 */
public class BaseActivity extends AppCompatActivity {
    private static final String APP_LIFE = "APP_LIFE";

    public static final String RELOAD_ALL_ACTION = "RELOAD_ALL_ACTION";
    public static final String RELOAD_DISCOVER_ACTION = "RELOAD_DISCOVER_ACTION";
    public static final String RELOAD_SINGLE_ACTION = "RELOAD_SINGLE_ACTION";
    public static final String NOTIFY_COUNT_ACTION = "NOTIFY_COUNT_ACTION";
    public static final String RELOAD_USER_CENTER_ACTION = "RELOAD_USER_CENTER_ACTION";
//    public static final String SYNC_LIKED_CARDS_ACTION = "SYNC_LIKED_CARDS_ACTION";

    public static final String BEAN = "BEAN";
    public static final String ANIMATION = "ANIMATION";

    protected boolean customAnimation = true;
    protected View rootView;
    protected Toolbar toolbar;

    public View getRootView() {
        return rootView;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WWindowManager.getInstance().addWindow(this);
        if (customAnimation) {
            overridePendingTransition(R.anim.activity_right_in, R.anim.activity_left_out);
        }
        LogTool.d("123456789", getClass().getSimpleName() + " onCreate");
    }

    @Override
    public void setContentView(int layoutResID) {
        rootView = View.inflate(this, layoutResID, null);
        super.setContentView(rootView);
        initToolbar();
        initOther();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initToolbar();
        initOther();
    }

    /**
     * 当不需要toolbar时子类直接作空实现，不要调用super.initToolbar()
     */
    protected void initToolbar() {
        View view = findViewById(R.id.toolbar);
        if (view != null) {
            toolbar = (Toolbar) view;
        }
    }

    protected void initOther() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (Session.isBugtagsOn) {
//            Bugtags.onResume(this);
//        }
        MobclickAgent.onResume(this);
        ZhugeTool.INSTANCE.onResume();
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (Session.isBugtagsOn) {
//            Bugtags.onPause(this);
//        }
        MobclickAgent.onPause(this);
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Session.uid > 0) {
            final String pushId = PushManager.getInstance().getClientid(getApplicationContext());
            if (!TextUtils.isEmpty(pushId) && !pushId.equals(SettingManager.getInstance().getPushId())) {
                NetManager.getInstance().updatePushId(pushId, new NetRequestCallBack(){
                    @Override
                    public void onSuccess(ArrayMap result) {
                        SettingManager.getInstance().setPushId(pushId);
                    }
                });
            }
        }
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WWindowManager.getInstance().removeActivity(this);
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " onDestroy");
    }

    @Override
    public void finish() {
        super.finish();
        if (customAnimation) {
            overridePendingTransition(R.anim.activity_left_in, R.anim.activity_right_out);
        }
        WWindowManager.getInstance().removeActivity(this);
        LogTool.d(APP_LIFE, getClass().getSimpleName() + " finish");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (Session.isBugtagsOn) {
//            Bugtags.onDispatchTouchEvent(this, ev);
//        }
        if (Session.isAnimating) {
            return true;
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    @Override
    public void onBackPressed() {
        if (!Session.isAnimating) {
            super.onBackPressed();
        }
    }
}
