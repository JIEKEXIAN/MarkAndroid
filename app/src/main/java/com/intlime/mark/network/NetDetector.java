package com.intlime.mark.network;

import android.content.Context;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.intlime.mark.application.AppEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/***
 * @author wtuadn
 * @version 1.0
 * @date 2015/3/26
 */
public class NetDetector {
    private List<NetObserver> mObservers = new ArrayList<NetObserver>();
    private NetWorkStateReceiver netWorkStateReceiver;
    private static NetDetector INSTANCE;

    private static Timer timer;
    private static boolean isTimeOut = false;

    private NetDetector() {
    }

    public static boolean isTimeOut() {
        return isTimeOut;
    }

    public static void setIsTimeOut(boolean isTimeOut) {
        NetDetector.isTimeOut = isTimeOut;
    }

    public void addNetObserver(NetObserver observer) {
        mObservers.add(observer);
    }

    public void removeNetObserver(NetObserver observer) {
        mObservers.remove(observer);
    }

    public void notifyNetState(boolean is) {
        for (NetObserver netObserver : mObservers) {
            netObserver.onNetChange(is);
        }
    }

    public static NetDetector getInstance() {
        return (INSTANCE != null) ? INSTANCE : (INSTANCE = new NetDetector());
    }

    public void startDetector() {
        if (netWorkStateReceiver == null) {
            Context context = AppEngine.getContext();
            netWorkStateReceiver = new NetWorkStateReceiver();
            IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            context.registerReceiver(netWorkStateReceiver, intentFilter);
        }
    }

    public void stopDetector() {
        if (netWorkStateReceiver != null) {
            try {
                Context context = AppEngine.getContext();
                context.unregisterReceiver(netWorkStateReceiver);
                netWorkStateReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) AppEngine.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null)
                return false;
            return info.isAvailable();
        }
        return false;
    }

    /***
     * @param context
     * @return
     */
    public static boolean isGpsEnabled(Context context) {
        LocationManager lm = ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE));
        List<String> accessibleProviders = lm.getProviders(true);
        return accessibleProviders != null && accessibleProviders.size() > 0;
    }

    /****
     * @param context
     * @return
     */
    public static boolean isWifiEnabled(Context context) {
        ConnectivityManager mgrConn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mgrTel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
    }

    /****
     * @param context
     * @return
     */
    public static boolean is3rd(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkINfo = cm.getActiveNetworkInfo();
        return networkINfo != null && networkINfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    /****
     * @param context
     * @return
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkINfo = cm.getActiveNetworkInfo();
        return networkINfo != null && networkINfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
