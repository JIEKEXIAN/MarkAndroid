package com.intlime.mark.tools.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.intlime.mark.R;
import com.intlime.mark.activitys.BaseActivity;
import com.intlime.mark.activitys.MovieListDetailActivity;
import com.intlime.mark.activitys.MyNewsActivity;
import com.intlime.mark.activitys.SplashActivity;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.tools.JsonTool;
import com.intlime.mark.tools.LogTool;

import org.json.JSONObject;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 处理push的进程
 *
 * @author wtuadn
 * @version 1.0
 */
class PushProcess implements Runnable {
    private ArrayBlockingQueue<PushMessage> pushMessages = new ArrayBlockingQueue<PushMessage>(5);

    private static ExecutorService singlePool;
    private static PushProcess pushProcess = null;

    private PushProcess() {
    }

    /**
     * 添加消息
     */
    public static synchronized void addPushMessage(PushMessage message) {
        if (pushProcess == null)
            startPushProcess();
        if (pushProcess != null)
            try {
                pushProcess.pushMessages.put(message);
                singlePool.execute(pushProcess);
            } catch (Exception e) {
                e.printStackTrace();
            }
        else {
            throw new IllegalArgumentException("did you called startPushProcess");
        }
    }

    /**
     * 开启push业务处理流程
     */
    public static void startPushProcess() {
        pushProcess = new PushProcess();
        singlePool = Executors.newSingleThreadExecutor();
        LogTool.d("PushProcess", "start push process");
    }

    /**
     * 停止push业务处理流程
     */
    public static void stopPushProcess() {
        if (pushProcess != null) {
            singlePool.shutdown();
            singlePool = null;
            pushProcess = null;
        }
    }

    @Override
    public void run() {
        try {
            PushMessage message = pushMessages.take();
            processPushMessage(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 所有的push业务逻辑都在这里不要到处用
     */
    private void processPushMessage(PushMessage message) {
        try {
            String json = message.getData();
            JSONObject jsonObject = new JSONObject(json);
            String pushType = JsonTool.optString(jsonObject, "push_type");
            if (pushType.equals("single")) {
                handleSingle(jsonObject);
            } else if (pushType.equals("single_comment_reply")) {
                handleCommentReply(jsonObject);
            } else if (pushType.equals("single_comment_like")) {
                handleCommentLike();
            } else {
                handleOther(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSingle(JSONObject jsonObject) {
        int id = jsonObject.optInt("id");
        String title = JsonTool.optString(jsonObject, "title");
        String content = JsonTool.optString(jsonObject, "content");

        Intent intent = new Intent(AppEngine.getContext(), MovieListDetailActivity.class);
        intent.putExtra(BaseActivity.ANIMATION, false);
        MovieListBean bean = new MovieListBean();
        bean.setId(id);
        intent.putExtra(BaseActivity.BEAN, bean);
        intent.setAction("from_push");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                AppEngine.getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        showNotify(id, title, content, pendingIntent);
    }

    private void handleCommentReply(JSONObject jsonObject) {
        SettingManager.getInstance().setCommentsCount(SettingManager.getInstance().getCommentsCount() + 1);
        AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.NOTIFY_COUNT_ACTION));

        String content = JsonTool.optString(jsonObject, "content");
        Intent intent = new Intent(AppEngine.getContext(), MyNewsActivity.class);
        intent.putExtra(BaseActivity.ANIMATION, false);
        intent.setAction("from_push");
        int id = (int) (Math.random() * 99999);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                AppEngine.getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        showNotify(id, "收到一个回复", content, pendingIntent);
    }

    private void handleCommentLike() {
        SettingManager.getInstance().setNotifyCount(SettingManager.getInstance().getNotifyCount() + 1);
        AppEngine.getContext().sendBroadcast(new Intent(BaseActivity.NOTIFY_COUNT_ACTION));
    }

    private void handleOther(JSONObject jsonObject) {
        String title = JsonTool.optString(jsonObject, "title");
        String content = JsonTool.optString(jsonObject, "content");
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) return;
        Intent intent = new Intent(AppEngine.getContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        int id = (int) (Math.random() * 99999);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                AppEngine.getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        showNotify(id, title, content, pendingIntent);
    }

    private void showNotify(int id, String title, String content, PendingIntent pendingIntent) {
        if (WWindowManager.getInstance().isAppOnForeground()) return;
        if (!SettingManager.getInstance().getNotifySwitch()) return;
        NotificationManager notificationManager = (NotificationManager)
                AppEngine.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(AppEngine.getContext())
                .setContentTitle(title)//设置通知栏标题
                .setContentText(content) //设置通知栏显示内容
                .setContentIntent(pendingIntent) //设置通知栏点击意图
                .setTicker(title) //通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知自动取消
                .setSmallIcon(R.mipmap.push)//设置通知小ICON
                .build();
        notificationManager.notify(id, notification);
    }
}
