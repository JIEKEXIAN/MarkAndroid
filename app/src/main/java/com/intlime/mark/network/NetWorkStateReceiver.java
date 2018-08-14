package com.intlime.mark.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.intlime.mark.tools.LogTool;

/***
 * 
 * @author wtuadn
 * @version 1.0
 * @date 2015/3/26
 * 
 */
public class NetWorkStateReceiver extends BroadcastReceiver
{

	private static final String TAG = "NetWorkStateReceiver";
	private static final String ANDROID_NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	public static boolean networkAvailable = true;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equalsIgnoreCase(ANDROID_NET_CHANGE_ACTION))
		{
			LogTool.d(TAG, "网络状态发生了改变...");
			if (!NetDetector.isNetworkAvailable())
			{
				networkAvailable = false;
				LogTool.d(TAG, "网络连接断开...");
			}
			else
			{
				networkAvailable = true;
				LogTool.d(TAG, "网络连接成功...");
			}
			// 通知所有注册了的网络状态观察者
			NetDetector.getInstance().notifyNetState(networkAvailable);
		}
	}

}