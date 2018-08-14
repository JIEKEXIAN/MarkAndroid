package com.intlime.mark.network;

import com.intlime.mark.tools.LogTool;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import java.net.SocketTimeoutException;
import java.util.List;

/****
 * 网络操作
 *
 * @author wtuadn
 */
public class Http {
    public final static int OUT_TIME = 30000;//默认的超时时间

    private static Http mhttp;
    private HttpClient client = null;

    private Http() {
        HttpParams httpParams = new BasicHttpParams();
        //设置http的参数
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(httpParams, true);

        //设置支持http 和https的两种方式
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        //启用安全多线程实现多任务
        ClientConnectionManager manager = new ThreadSafeClientConnManager(httpParams, registry);
        //完成创建
        client = new DefaultHttpClient(manager, httpParams);

        //设置连接超时时间
        ConnManagerParams.setTimeout(client.getParams(), OUT_TIME / 6);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), OUT_TIME / 4);
        HttpConnectionParams.setSoTimeout(client.getParams(), OUT_TIME);
    }

    public synchronized static Http getInstance() {
        if (mhttp == null) {
            mhttp = new Http();
        }
        return mhttp;
    }

    /**
     * 销毁实例
     */
    public static void shutdown() {
        if (mhttp != null) {
            if (mhttp.client != null && mhttp.client.getConnectionManager() != null) {
                mhttp.client.getConnectionManager().shutdown();
            }
            mhttp.client = null;
            mhttp = null;
        }
    }

    public String doPost(String url, List<NameValuePair> params) {
        return doPost(url, params, "UTF-8", null);
    }

    public String doPost(String url, List<NameValuePair> params, NetRequestCallBack callBack) {
        return doPost(url, params, "UTF-8", callBack);
    }

    public String doPost(String url, List<NameValuePair> params, String code, NetRequestCallBack callBack) {
        LogTool.d("http_post", url + "\n" + params.toString());
        HttpPost post = new HttpPost(url);
        try {
            HttpEntity entity = new UrlEncodedFormEntity(params, code);
            post.setEntity(entity);
            return doRequest(url, post, callBack);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String doGet(String url) {
        return doGet(url, "utf-8", null);
    }

    public String doGet(String url, NetRequestCallBack callBack) {
        return doGet(url, "utf-8", callBack);
    }

    public String doGet(String url, String code, NetRequestCallBack callBack) {
        LogTool.d("http_get", url);
        HttpGet get = new HttpGet(url);
        return doRequest(url, get, callBack);
    }

    public String doPut(String url, List<NameValuePair> params) {
        return doPut(url, params, "utf-8", null);
    }

    public String doPut(String url, List<NameValuePair> params, NetRequestCallBack callBack) {
        return doPut(url, params, "utf-8", callBack);
    }

    public String doPut(String url, List<NameValuePair> params, String code, NetRequestCallBack callBack) {
        LogTool.d("http_put", url + "\n" + params.toString());
        HttpPut put = new HttpPut(url);
        try {
            HttpEntity entity = new UrlEncodedFormEntity(params, code);
            put.setEntity(entity);
            return doRequest(url, put, callBack);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String doDelete(String url) {
        return doDelete(url, "utf-8", null);
    }

    public String doDelete(String url, NetRequestCallBack callBack) {
        return doDelete(url, "utf-8", callBack);
    }

    public String doDelete(String url, String code, NetRequestCallBack callBack) {
        LogTool.d("http_delete", url);
        HttpDelete delete = new HttpDelete(url);
        return doRequest(url, delete, callBack);
    }

    private String doRequest(String url, HttpRequestBase request, NetRequestCallBack callBack) {
        String result = null;
        try {
            if (callBack != null) {
                callBack.setHttpRequest(request);
                callBack.setTimeOut(OUT_TIME, url);
            }
            HttpResponse httpResponse = client.execute(request);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(httpResponse.getEntity());
            }
        } catch (NoHttpResponseException e) { // 无服务器响应
            timeOutCallBack(callBack, url);
            LogTool.d("time out", "无服务器响应");
        } catch (ConnectTimeoutException e) { // 捕获ConnectionTimeout
            timeOutCallBack(callBack, url);
            LogTool.d("time out", "与服务器建立连接超时");
        } catch (SocketTimeoutException e) {
            timeOutCallBack(callBack, url);
            LogTool.d("time out", "读数据超时");
        } catch (Exception e) {
            timeOutCallBack(callBack, url);
            e.printStackTrace();
        } finally {
            if (callBack != null) {
                callBack.cancelTimeOut();
            }
        }
        return result;
    }

    private void timeOutCallBack(NetRequestCallBack callBack, String url) {
        if (callBack != null) {
            callBack.onTimeOut(url);
        }
    }
}
