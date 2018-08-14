package com.intlime.mark.network;

import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadManager;

/**
 * Created by wtuadn on 15-6-1.
 */
public class MyUploadManager {
    private static UploadManager INSTANCE;

    public static UploadManager getInstance() {
        if (INSTANCE == null) {
            Configuration config = new Configuration.Builder()
                    .chunkSize(256 * 1024)  //分片上传时，每片的大小。 默认 256K
                    .putThreshhold(512 * 1024)  // 启用分片上传阀值。默认 512K
                    .connectTimeout(10) // 链接超时。默认 10秒
                    .responseTimeout(60) // 服务器响应超时。默认 60秒
//                    .recorder(recorder)  // recorder 分片上传时，已上传片记录器。默认 null
//                    .recorder(recorder, keyGen)  // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
                    .build();
            INSTANCE = new UploadManager(config);
        }
        return INSTANCE;
    }


    private static String token = "";
    private static long time = System.currentTimeMillis();

    public static String getToken() {
        if (System.currentTimeMillis() - time > 1000 * 60 * 60) {
            token = "";
        }
        return token;
    }

    public static void setToken(String token) {
        MyUploadManager.token = token;
        time = System.currentTimeMillis();
    }
}
