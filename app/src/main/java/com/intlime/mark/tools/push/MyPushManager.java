package com.intlime.mark.tools.push;

import java.util.ArrayList;

/**
 * push管理
 *
 * @author wtuadn
 * @version 1.0
 * @date 2015/3/24
 */
public class MyPushManager {

    private ArrayList notifyIds;

    private static MyPushManager INSTANCE;

    public ArrayList getNotifyIds() {
        return notifyIds;
    }

    private MyPushManager() {
        notifyIds = new ArrayList();
    }

    public static MyPushManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MyPushManager();
        }
        return INSTANCE;
    }

    public synchronized void addPushMessageEnQueue(PushMessage message) {
        PushProcess.addPushMessage(message);
    }

    public void startPush() {
        PushProcess.startPushProcess();
    }

    public void stopPush() {
        PushProcess.stopPushProcess();
    }
}
