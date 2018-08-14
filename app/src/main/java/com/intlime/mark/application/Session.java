package com.intlime.mark.application;

import android.support.v4.util.ArrayMap;

/**
 * 保存session
 * Created by wtu on 2015/04/11 011.
 */
public class Session {
    public static final boolean isRelease = true;//正式版还是测试版
    public static final boolean isDebug = false;//调戏模式
//    public static final boolean isBugtagsOn = true;//是否打开bugtags

    public static boolean isAnimating = false;
    public static boolean isScreenOn = true;

    public static int uid = SettingManager.getInstance().getUid();
    public static String mUid = SettingManager.getInstance().getMUid();

    public static ArrayMap map = new ArrayMap();//保存一些临时变量
}
