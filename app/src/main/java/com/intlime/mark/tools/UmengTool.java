package com.intlime.mark.tools;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import java.util.Map;

/**
 * Created by root on 16-2-29.
 */
public class UmengTool {
    private static UmengTool INSTANCE;
    public static String WXAPPID = "wxa9ba31550bc921db";
    public static int REQUEST_WRITE_EXTERNAL_STORAGE = 2459;
    public static int REQUEST_CAMERA = 2460;

    public static boolean isInited() {
        return INSTANCE != null;
    }

    public static UmengTool getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UmengTool();
        }
        return INSTANCE;
    }

    private UmengTool() {
        PlatformConfig.setWeixin("wxa9ba31550bc921db", "bce3ebe7752449ae95c2d833253c3b64");
        PlatformConfig.setQQZone("1105041716", "XzPJLuwfoUgUhP8W");
        PlatformConfig.setSinaWeibo("1337784884", "22c4a95da730d2bc2c2fe4a3348ca7af");
    }

    public void doThirdPartLogin(final Activity activity, final SHARE_MEDIA shareMedia, final UMAuthListener listener) {
        UMShareAPI.get(activity).doOauthVerify(activity, shareMedia, new UMAuthListener() {
            @Override
            public void onComplete(SHARE_MEDIA platform, int action, Map<String, String> data) {
                UMShareAPI.get(activity).getPlatformInfo(activity, shareMedia, new UMAuthListener() {
                    @Override
                    public void onComplete(SHARE_MEDIA share_media, int action, Map<String, String> map) {
                        listener.onComplete(share_media, action, map);
                        LogTool.d("map", map == null ? "null" : map.toString());
                    }

                    @Override
                    public void onError(SHARE_MEDIA share_media, int action, Throwable throwable) {
                        listener.onError(share_media, action, throwable);
                    }

                    @Override
                    public void onCancel(SHARE_MEDIA share_media, int i) {
                        listener.onCancel(share_media, i);
                    }
                });
                LogTool.d("data", data == null ? "null" : data.toString());
            }

            @Override
            public void onError(SHARE_MEDIA platform, int action, Throwable t) {
                listener.onError(platform, action, t);
            }

            @Override
            public void onCancel(SHARE_MEDIA platform, int action) {
                listener.onCancel(platform, action);
            }
        });
    }

    public static boolean checkPermission(Activity context, String permission, int requestCode) {
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, permission)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(context,
                    new String[]{permission}, requestCode);
            return false;
        }
    }
}
