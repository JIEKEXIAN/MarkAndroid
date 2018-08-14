package com.intlime.mark.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.widget.Toast;

import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.WWindowManager;

/***
 * @author wtuadn, wtuadn
 * @version 2.0
 * @date 2015/3/31
 */
public class ToastTool extends Handler {
    private static ToastTool toastTool = null;
    private static Toast mToast;

    @Override
    public void dispatchMessage(Message msg) {
        super.dispatchMessage(msg);
        if (mToast == null) {
            mToast = Toast.makeText(AppEngine.getContext(), msg.obj.toString(), msg.what);
        }
        mToast.setDuration(msg.what);
        mToast.setText(msg.obj.toString());
        switch (msg.arg1) {
            case 0:
                mToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, WWindowManager.getInstance().getHeight() / 9);
                break;
            case 1:
                mToast.setGravity(Gravity.CENTER, 0, 0);
                break;
            case 2:
                mToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, WWindowManager.getInstance().getHeight() / 9);
                break;
        }
        mToast.show();
    }

    private ToastTool(Looper looper) {
        super(looper);
    }

    public static void show(String msg) {
        show(msg, Toast.LENGTH_SHORT);
    }

    public static void show(String msg, int duration) {
        show(msg, duration, 0);
    }

    /**
     * 显示 toast
     *
     * @param msg         要显示的消息
     * @param duration    时间
     * @param gravityType 类型 0为默认， 1为居中， 2为顶部
     */
    public static void show(String msg, int duration, int gravityType) {
        if (toastTool == null) {
            toastTool = new ToastTool(Looper.getMainLooper());
        }
        toastTool.sendMessage(toastTool.obtainMessage(duration, gravityType, 0, msg));
    }
}
