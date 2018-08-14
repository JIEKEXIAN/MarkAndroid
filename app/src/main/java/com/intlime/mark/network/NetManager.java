package com.intlime.mark.network;

import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.intlime.mark.R;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.tools.CryptTool;
import com.intlime.mark.tools.JsonTool;
import com.intlime.mark.tools.MResource;
import com.intlime.mark.tools.ToastTool;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wtuadn on 15-6-3.
 */
public class NetManager {
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_DATA = "error_data";
    public static final String DOMAIN = Session.isRelease ? "http://api.markapp.cn/v160" : "http://api.markapp.cn/v000";

    private static final String DOUBAN_URL = "https://api.douban.com/v2/";
    private static final String APIKEY = "06ef16d16fd8d49414cd44744f8a7dbc";
    private static final String APIKEY_EXPERT = "0df993c66c0c636e29ecbb5344252a4a";

    private static NetManager INSTANCE;
    // 线程池
    private ExecutorService executorService;
    //主线程
    private Handler mainHandler;

    public static NetManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NetManager();
        }
        return INSTANCE;
    }

    private NetManager() {
        executorService = Executors.newCachedThreadPool();
        mainHandler = ThreadManager.getInstance().getHandler();
    }

    public static void shutDown() {
        if (INSTANCE != null) {
            INSTANCE.executorService.shutdown();
            INSTANCE.mainHandler = null;
            INSTANCE = null;
        }
    }

    private boolean checkNetwork(NetRequestCallBack callBack) {
        if (!NetDetector.isNetworkAvailable()) {
            ToastTool.show(MResource.getString(R.string.no_network));
            callBack.onDefault();
            callBack.onFail(null, -2);
            return true;
        }
        return false;
    }

    /**
     * 搜电影
     */
    public void searchMovie(final String key, final NetRequestCallBack callBack) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOUBAN_URL + "movie/search?q=" + URLEncoder.encode(key)
                        + "&count=12&apikey=" + APIKEY, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.searchMovie(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    public void searchMovie(final String key, final int start, final NetRequestCallBack callBack) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOUBAN_URL + "movie/search?q=" + URLEncoder.encode(key)
                        + "&count=12&start=" + start + "&apikey=" + APIKEY, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.searchMovie(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    public void getMovieDetail(final String db_num, final String imgUrl, final String rate, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String url = DOMAIN + "/movies/" + db_num;
                if (!TextUtils.isEmpty(imgUrl)) {
                    url += "/img_url/" + CryptTool.base64Encode(CryptTool.encrypt(imgUrl));
                }
                if (!TextUtils.isEmpty(rate)) {
                    url += "/dbrating/" + CryptTool.base64Encode(CryptTool.encrypt(rate));
                }
                String result = Http.getInstance().doGet(url, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getMovieDetail(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    public void getMovieShareInfo(final List<MovieBean> list, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String movie_ids = "";
                for (int i = 0; i < list.size(); i++) {
                    MovieBean bean = list.get(i);
                    movie_ids += bean.getId();
                    if (i != list.size() - 1) {
                        movie_ids += ",";
                    }
                }
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("movie_ids", CryptTool.encrypt(movie_ids)));
                String result = Http.getInstance().doPost(DOMAIN + "/movies/share", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getMovieShareInfo(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取验证码
     */
    public void getSMSCode(final String account, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("phonenumber", CryptTool.encrypt(account)));
                params.add(new BasicNameValuePair("type", Integer.toString(type)));
                String result = Http.getInstance().doPost(DOMAIN + "/user/getSMSCode", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getSMSCode(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 登录
     */
    public void login(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doPost(DOMAIN + "/user/login", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.login(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 第三方登录
     */
    public void thirdPartLogin(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doPost(DOMAIN + "/user/otherLogin", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.login(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 绑定第三方账户
     */
    public void bindThirdPart(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPut(DOMAIN + "/user/bindOtherLogin", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "bindThirdPart");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 解绑第三方
     */
    public void unbindThirdPart(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPut(DOMAIN + "/user/unbind", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "unbindThirdPart");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 更新推送id
     */
    public void updatePushId(final String pushId, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("pushid", CryptTool.encrypt(pushId)));
                params.add(new BasicNameValuePair("device", "2"));//设备类型 1-ios 2-安卓
                String result = Http.getInstance().doPut(DOMAIN + "/user/updatePushid", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "updatePushId", false);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 重设密码
     */
    public void updatePasswd(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doPut(DOMAIN + "/user/updatePasswd", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.updatePasswd(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 第三方登录后设置密码
     */
    public void setPasswd(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPut(DOMAIN + "/user/setPasswd", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "setPasswd");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 第三方绑定后验证手机号
     */
    public void bindPhoneNumber(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/user/bindPhoneNumber", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "bindPhoneNumber");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 同步数据
     */
    public void syncUserData(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPut(DOMAIN + "/userdatas/syncUserData", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.syncUserData(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    public void getUserInfo(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/user/getUserinfo", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getUserInfo(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    public void getUserUnreadCount(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/usermessage/unreadcount", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getUserUnreadCount(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 添加电影
     */
    public void addMovie(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPost(DOMAIN + "/movies", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.addMovie(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 完成电影
     */
    public void doneMovie(final String ids, final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + ids, params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.doneMovie(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 删除电影
     */
    public void deleteMovie(final String ids, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                params.add(new BasicNameValuePair("type", "3"));
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + ids, params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.deleteMovie(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 收藏电影
     */
    public void favoriteMovie(final String ids, final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + ids, params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.favoriteMovie(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 评分电影
     */
    public void rateMovie(final int id, final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + id + "/rating", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.rateMovie(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 观影感受
     */
    public void noteMovie(final int id, final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + id + "/watchsay", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "noteMovie");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 观影日期
     */
    public void movieWatchTime(final int id, final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPut(DOMAIN + "/movies/" + id + "/watchdate", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "movieWatchTime");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 电影台词
     */
    public void getMovieWord(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/resources/lines", callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getMovieWord(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 影单专题列表
     */
    public void getMovieListSet(final int id, final int start, final int count, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", Integer.toString(count)));
                params.add(new BasicNameValuePair("id", Integer.toString(id)));
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPost(DOMAIN + "/singles/catlist", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMovieListSet(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 从推送打开影单的统计
     */
    public void movieListPushOpens(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/singles/" + id + "/pushopens", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "movieListPushOpens", false);
            }
        });
    }

    /**
     * 影单详情
     */
    public void getMovieListDetail(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("id", Integer.toString(id)));
                String url;
                if (Session.isRelease) {
                    url = "http://api.markapp.cn/mark_web/singles/detail";
                } else {
                    url = "http://api.markapp.cn/mark_web_test/singles/detail";
                }
                String result = Http.getInstance().doPost(url, params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMovieListDetail(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 获取影单评论列表
     */
    public void getMovieListComment(final int single_id, final int comment_id, final int create_time, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("single_id", Integer.toString(single_id)));
                params.add(new BasicNameValuePair("comment_id", Integer.toString(comment_id)));
                params.add(new BasicNameValuePair("create_time", Integer.toString(create_time)));
                String result = Http.getInstance().doPost(DOMAIN + "/singlecomment/listcomments", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMovieListComment(result, create_time == 0 ? false : true);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 评论影单
     */
    public void commentMovieList(final int single_id, final String content, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("single_id", Integer.toString(single_id)));
                params.add(new BasicNameValuePair("content", content));
                String result = Http.getInstance().doPost(DOMAIN + "/singlecomment/comment", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.commentMovieList(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 回复影单的评论
     */
    public void replyComment(final int comment_id, final String content, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("comment_id", Integer.toString(comment_id)));
                params.add(new BasicNameValuePair("content", content));
                String result = Http.getInstance().doPost(DOMAIN + "/singlecomment/reply", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.commentMovieList(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 影单评论点赞
     */
    public void movieListCommentLike(final int comment_id, final boolean isLike, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("comment_id", Integer.toString(comment_id)));
                String result;
                if (isLike) {
                    result = Http.getInstance().doPost(DOMAIN + "/singlecomment/like", params, callBack);
                } else {
                    result = Http.getInstance().doPost(DOMAIN + "/singlecomment/cancle", params, callBack);
                }
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.emptyParse(result, "movieListCommentLike " + isLike);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 删除自己的评论
     */
    public void deleteSelfComment(final int comment_id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("comment_id", Integer.toString(comment_id)));
                String result = Http.getInstance().doPost(DOMAIN + "/singlecomment/delete", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.emptyParse(result, "deleteSelfComment");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 举报评论
     */
    public void reportComment(final int comment_id, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("comment_id", Integer.toString(comment_id)));
                params.add(new BasicNameValuePair("type", Integer.toString(type)));
                String result = Http.getInstance().doPost(DOMAIN + "/singlecomment/report", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.emptyParse(result, "reportComment");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取用户评论列表
     */
    public void getUserComments(final int create_time, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("create_time", Integer.toString(create_time)));
                String result = Http.getInstance().doPost(DOMAIN + "/usermessage/comments", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getUserComments(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 获取用户通知列表
     */
    public void getUserNotify(final int create_time, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("create_time", Integer.toString(create_time)));
                String result = Http.getInstance().doPost(DOMAIN + "/usermessage/notifications", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getUserNotify(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 发现页面的banner
     */
    public void getBanner(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/singles/banner", callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getBanner(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 发现页面的影单列表
     */
    public void getDiscoverList(final int start, final int count, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", Integer.toString(count)));
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                String result = Http.getInstance().doPost(DOMAIN + "/singles/list", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getDiscoverList(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 喜欢影单
     */
    public void likeMovieList(final int id, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                params.add(new BasicNameValuePair("type", Integer.toString(type)));
                String result = Http.getInstance().doPost(DOMAIN + "/singles/" + id + "/likes", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "likeMovieList");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    public void getGroupClassify(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/singles/groupcat", callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getGroupClassify(result);
                final int error_code = (int) map.get(ERROR_CODE);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                        callBack.onDefault();
                    }
                });
            }
        });
    }

    /**
     * 喜欢影单的统计
     */
    public void movieListShares(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String url;
                if (Session.isRelease) {
                    url = "http://114.215.104.21/mark_web/singles/" + id + "/shares";
                } else {
                    url = "http://114.215.104.21/mark_web_test/singles/" + id + "/shares";
                }
                String result = Http.getInstance().doPost(url, params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "movieListShares", false);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 从影单添加电影的统计
     */
    public void movieListAdd(final int singleId, final int movieId, final int isDone, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("is_done", Integer.toString(isDone)));
                params.add(new BasicNameValuePair("movie_id", Integer.toString(movieId)));
                String url;
                if (Session.isRelease) {
                    url = "http://114.215.104.21/mark_web/singles/" + singleId + "/addmovies";
                } else {
                    url = "http://114.215.104.21/mark_web_test/singles/" + singleId + "/addmovies";
                }
                String result = Http.getInstance().doPost(url, params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "movieListAdd", false);
            }
        });
    }

    public void getMovieSource(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/movies/" + id + "/sourse", callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMovieSource(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    public void getLikedMovieList(final int start, final int count, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", Integer.toString(count)));
                String result = Http.getInstance().doPost(DOMAIN + "/userdatas/likes", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getLikedMovieList(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onDefault();
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                    }
                });
            }
        });
    }

    /**
     * 获取电影的剧照
     */
    public void getMoviePhotos(final int id, final int start, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/movies/" + id + "/photos/start/" + start + "/count/10", callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMoviePhotos(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取电影的台词
     */
    public void getMovieLines(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doGet(DOMAIN + "/movies/" + id + "/lines", callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMovieLines(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    public void getCinemaMovie(final int start, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (type == 1) {
                    result = Http.getInstance().doGet(DOMAIN + "/movies/intheaters/start/" + start + "/count/10", callBack);
                } else {
                    result = Http.getInstance().doGet(DOUBAN_URL + "movie/coming_soon?start=" + start + "&count=10&apikey=" + APIKEY_EXPERT, callBack);
                }
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getCinemaMovie(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取上传token
     */
    public void getFileToken(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/file/token", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getFileToken(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 更新头像
     */
    public void changeHeadImg(final String imgUrl, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("img_name", CryptTool.encrypt(imgUrl)));

                String result = Http.getInstance().doPut(DOMAIN + "/user/imgurl", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.changeHeadImg(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 更新昵称
     */
    public void changeNickname(final String nickname, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("nickname", CryptTool.encrypt(nickname)));

                String result = Http.getInstance().doPut(DOMAIN + "/user/nickname", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "changeNickname");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 创建影单
     */
    public void newSingle(final String name, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("name", name));
                String result = Http.getInstance().doPost(DOMAIN + "/usersingles", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.newSingle(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 修改影单名字
     */
    public void changeSingleName(final int id, final String name, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("name", name));
                String result = Http.getInstance().doPut(DOMAIN + "/usersingles/" + id + "/singlename", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "changeSingleName");
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 删除影单
     */
    public void deleteSingle(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String result = Http.getInstance().doDelete(DOMAIN + "/usersingles/" + id + "?uid=" + Session.uid + "&muid=" + URLEncoder.encode(Session.mUid), callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "deleteSingle");
                int error_code = (int) map.get(ERROR_CODE);
                callBack.onDefault();
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 修改影单中电影状态
     */
    public void addSingleAccess(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/usersingles/moivetosingles", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.addSingleAccess(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 获取每日电影卡片推荐
     */
    public void getDailyCard(final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/everyday", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getDailyCard(result);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 每日电影卡片分享
     */
    public void dailyCardShare(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/" + id + "/shares", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "dailyCardShare", false);
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 自定义卡片分享
     */
    public void movieCardShare(final List<NameValuePair> params, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/usershares", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "movieCardShare");
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
                callBack.onDefault();
            }
        });
    }

    /**
     * 喜欢电影卡片
     */
    public void likeMovieCard(final int id, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                if (Session.uid > 0) {
                    params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                    params.add(new BasicNameValuePair("muid", Session.mUid));
                }
                params.add(new BasicNameValuePair("type", Integer.toString(type)));
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/" + id + "/likes", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "likeMovieCard");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取喜欢电影卡片列表
     */
    public void getLikedMovieCards(final int start, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", "10"));
                String result = Http.getInstance().doPost(DOMAIN + "/userdatas/likedCards", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getLikedMovieCards(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                        callBack.onDefault();
                    }
                });
            }
        });
    }

    /**
     * 获取更多电影卡片列表
     */
    public void getMoreMovieCards(final int start, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", "10"));
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/more", params, callBack);
                if (callBack.isRequestCanceled) return;
                final ArrayMap map = JsonTool.getMoreMovieCards(result);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int error_code = (int) map.get(ERROR_CODE);
                        if (error_code == 1) {
                            callBack.onSuccess(map);
                        } else {
                            callBack.onFail(map, error_code);
                        }
                        callBack.onDefault();
                    }
                });
            }
        });
    }

    /**
     * 获取电影卡片详情
     */
    public void getMovieCardDetail(final int id, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                String result = Http.getInstance().doPost(DOMAIN + "/moviepics/" + id + "/detail", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getMovieCardDetail(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 获取撰稿人信息
     */
    public void getWriterInfo(final int start, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("start", Integer.toString(start)));
                params.add(new BasicNameValuePair("count", "10"));
                String result = Http.getInstance().doPost(DOMAIN + "/writer/writerinfo", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.getWriterInfo(result);
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }

    /**
     * 播放源反馈
     */
    public void sourceFeedback(final int id, final String name, final int type, final NetRequestCallBack callBack) {
        if (checkNetwork(callBack)) return;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("uid", Integer.toString(Session.uid)));
                params.add(new BasicNameValuePair("muid", Session.mUid));
                params.add(new BasicNameValuePair("movie_id", Integer.toString(id)));
                params.add(new BasicNameValuePair("movie_name", name));
                params.add(new BasicNameValuePair("type", Integer.toString(type)));
                String result = Http.getInstance().doPost(DOMAIN + "/source/feedback", params, callBack);
                if (callBack.isRequestCanceled) return;
                ArrayMap map = JsonTool.emptyParse(result, "sourceFeedback");
                callBack.onDefault();
                int error_code = (int) map.get(ERROR_CODE);
                if (error_code == 1) {
                    callBack.onSuccess(map);
                } else {
                    callBack.onFail(map, error_code);
                }
            }
        });
    }
}
