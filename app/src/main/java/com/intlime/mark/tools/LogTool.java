package com.intlime.mark.tools;

import android.util.Log;

import com.intlime.mark.application.Session;

/**
 * Created by wtu on 2015/6/16.
 */
public class LogTool {
    private final static String TAG = "wtuadn_debug";

    public static void e(String tag, String msg) {
        if (Session.isDebug) {
            Log.e(TAG, String.format("%s    %s\n ", tag, msg));
        }
    }

    public static void d(String tag, String msg) {
        if (Session.isDebug) {
            Log.d(TAG, String.format("%s    %s\n ", tag, msg));
        }
    }

    public static void v(String tag, String msg) {
        if (Session.isDebug) {
            Log.v(TAG, String.format("%s    %s\n ", tag, msg));
        }
    }
}
