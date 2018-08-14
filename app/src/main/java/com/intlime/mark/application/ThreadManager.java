package com.intlime.mark.application;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI相关的线程池管理
 * 与网络和图片加载的线程相独立
 * Created by wtuadn on 15-6-5.
 */
public class ThreadManager {
    private static ThreadManager INSTANCE;
    // 线程池
    private ExecutorService executorService;
    //用于和主线交互
    private Handler handler;

    public static ThreadManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ThreadManager();
        }
        return INSTANCE;
    }

    private ThreadManager() {
        executorService = Executors.newCachedThreadPool();
        handler = new Handler(Looper.getMainLooper());
    }

    public static void shutDown() {
        if (INSTANCE != null) {
            INSTANCE.executorService.shutdown();
            INSTANCE.handler = null;
            INSTANCE = null;
        }
    }

    /**
     * 提交到后台线程
     * @param runnable
     */
    public void submit(Runnable runnable){
        executorService.submit(runnable);
    }

    /**
     * 提交到主线程
     * @param runnable
     */
    public void post(Runnable runnable){
        handler.post(runnable);
    }

    /**
     * 提交到主线程
     * @param runnable
     */
    public void postDelayed(Runnable runnable, long delayTime){
        handler.postDelayed(runnable, delayTime);
    }

    public Handler getHandler() {
        return handler;
    }
}
