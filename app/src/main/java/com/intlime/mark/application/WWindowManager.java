package com.intlime.mark.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.intlime.mark.activitys.BaseActivity;

import java.util.List;
import java.util.Stack;

public class WWindowManager {
    private static WWindowManager INSTANCE;
    private Stack<BaseActivity> windows;
    private int statusBarHeight;

    private WWindowManager() {
        windows = new Stack<>();
    }

    public Stack<BaseActivity> getWindows() {
        return windows;
    }

    public static WWindowManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WWindowManager();
        }
        return INSTANCE;
    }

    public BaseActivity getActivity(String activityName) {
        for (BaseActivity activity : windows) {
            if (activity.getClass().getSimpleName().equals(activityName)) {
                return activity;
            }
        }
        return null;
    }

    public BaseActivity getCurrentActivity() {
        return windows.empty() ? null : windows.peek();
    }

    public void removeActivity(Activity activity) {
        if (!windows.empty()) {
            windows.remove(activity);
        }
    }

    public void addWindow(BaseActivity activity) {
        windows.push(activity);
    }

    public int getWidth() {
        return AppEngine.getContext().getResources().getDisplayMetrics().widthPixels;
    }

    public int getHeight() {
        return AppEngine.getContext().getResources().getDisplayMetrics().heightPixels;
    }

    public int getActivityHeight() {
        return getHeight() - getStatusBarHeight();
    }

    public int getStatusBarHeight() {
        if (statusBarHeight <= 0) {
            Rect frame = new Rect();
            getCurrentActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            statusBarHeight = frame.top;
        }
        return statusBarHeight;
    }

    /**
     * 判断是横屏还是竖屏
     *
     * @return
     */
    public boolean isPortrait() {
        return getWidth() < getHeight();
    }

    public void hideInput() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                IBinder iBinder = view.getWindowToken();
                if (iBinder != null) {
                    ((InputMethodManager) AppEngine.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(iBinder, InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }
    }

    public void showSoftInput(final EditText editText) {
        editText.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                editText.getViewTreeObserver().removeOnPreDrawListener(this);
                ThreadManager.getInstance().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editText.requestFocus();
                        InputMethodManager inputManager = (InputMethodManager) editText.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(editText, 0);
                    }
                }, 50);
                return true;
            }
        });
    }

    //判断是否在前台运行
    public boolean isAppOnForeground() {
        if (!Session.isScreenOn) return false;
        ActivityManager am = (ActivityManager) AppEngine.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (topActivity.getPackageName().equals(AppEngine.getContext().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPkgInstalled(String pkgName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = AppEngine.getContext().getPackageManager().getPackageInfo(pkgName, 0);
        } catch (Exception e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if (packageInfo == null) {
            return false;
        } else {
            return true;
        }
    }
}
