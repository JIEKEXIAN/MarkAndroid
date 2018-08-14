package com.intlime.mark.tools.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.igexin.sdk.PushConsts;
import com.intlime.mark.tools.LogTool;

/**
 * push接收
 *
 * @author wtuadn
 * @verion v1.1
 * @date 2015/3/24
 */
public class PushReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            if (bundle.getInt(PushConsts.CMD_ACTION) == PushConsts.GET_MSG_DATA) {
                // 获取透传（payload）数据
                byte[] payload = bundle.getByteArray("payload");
                if (payload != null) {
                    String data = new String(payload);
                    LogTool.d("push", "push_data:" + data);
                    if (!TextUtils.isEmpty(data))
                        MyPushManager.getInstance().addPushMessageEnQueue(new PushMessage(data));
                }
            }
        }
    }
}
