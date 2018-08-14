package com.intlime.mark.network;

import android.os.Handler;
import android.support.v4.util.ArrayMap;

import com.intlime.mark.R;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.tools.LogTool;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.ToastTool;

import org.apache.http.client.methods.HttpRequestBase;

/**
 * Created by wtuadn on 15-6-3.
 */
public class NetRequestCallBack {
    protected boolean isTimeOut = false;
    protected boolean isRequestCanceled = false;
    private boolean isTimeOutCanceled = false;
    private HttpRequestBase httpRequest;
    private Handler timeHandler;
    private Runnable timeOutRunnable;

    public NetRequestCallBack() {
        timeHandler = ThreadManager.getInstance().getHandler();
    }

    public final void abortHttpRequest() {
        isRequestCanceled = true;
        if (httpRequest != null) {
            cancelTimeOut();
            if (!httpRequest.isAborted())
                httpRequest.abort();
        }
    }

    public final void setHttpRequest(HttpRequestBase httpRequest) {
        this.httpRequest = httpRequest;
    }

    /**
     * 不管成功失败，默认的回调
     */
    public void onDefault() {
    }

    /**
     * 成功的回调
     */
    public void onSuccess(ArrayMap result) {
    }

    /**
     * 失败的回调
     *
     * @param error_code -2为没网络或网络超时（手动设置的），-1为网络错误或解析失败
     */
    public void onFail(ArrayMap result, int error_code) {
    }

    final protected void setTimeOut(int outTime, final String logTag) {
        timeOutRunnable = new Runnable() {
            @Override
            public void run() {
                onTimeOut(logTag);
            }
        };
        timeHandler.postDelayed(timeOutRunnable, outTime + 1000);
    }

    final public synchronized void onTimeOut(String logTag) {
        if (!isTimeOutCanceled) {
            isTimeOutCanceled = true;
            isRequestCanceled = true;
            isTimeOut = true;
            ToastTool.show(MResource.getString(R.string.net_time_out));
            onFail(null, -2);
            onDefault();
            LogTool.d(logTag, "fail timeout");
            if (httpRequest != null) {
                ThreadManager.getInstance().submit(new Runnable() {
                    @Override
                    public void run() {
                        if (!httpRequest.isAborted())
                            httpRequest.abort();
                    }
                });
            }
        }
    }

    final protected void cancelTimeOut() {
        isTimeOutCanceled = true;
        if (timeHandler != null && timeOutRunnable != null) {
            timeHandler.removeCallbacks(timeOutRunnable);
        }
    }
}