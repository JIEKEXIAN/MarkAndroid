package com.intlime.mark.tools;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.widget.Toast;

import com.intlime.mark.R;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.bean.ClassifyBean;
import com.intlime.mark.bean.CommentBean;
import com.intlime.mark.bean.MovieBean;
import com.intlime.mark.bean.MovieCardBean;
import com.intlime.mark.bean.MovieListBean;
import com.intlime.mark.bean.SingleAccessBean;
import com.intlime.mark.bean.SingleBean;
import com.intlime.mark.bean.WriterBean;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.tools.db.MovieDbManager;
import com.intlime.mark.tools.db.MovieSingleDbManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wtu on 2015/05/19 019.
 */
public class JsonTool {
    private interface JsonI {
        void defaultHandle(ArrayMap map);

        void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException;

        void failedHandle(ArrayMap map);
    }

    private static class EmptyJsonI implements JsonI {
        @Override
        public void defaultHandle(ArrayMap map) {
        }

        @Override
        public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
        }

        @Override
        public void failedHandle(ArrayMap map) {
        }
    }

    public static String optString(JSONObject jsonObject, String key) {
        return optString(jsonObject, key, "");
    }

    public static String optString(JSONObject jsonObject, String key, String defaultStr) {
        try {
            return jsonObject.isNull(key) ? defaultStr : jsonObject.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultStr;
    }

    private static ArrayMap parseJson(String json, String logTag, JsonI jsonI) {
        return parseJson(json, "status", logTag, true, jsonI);
    }

    private static ArrayMap parseJson(String json, String logTag, boolean showHint, JsonI jsonI) {
        return parseJson(json, "status", logTag, showHint, jsonI);
    }

    private static ArrayMap parseJson(String json, String errorTag, String logTag, boolean showHint, JsonI jsonI) {
        ArrayMap map = new ArrayMap();
        map.put(NetManager.ERROR_CODE, -1);
        if (TextUtils.isEmpty(json)) {
            if (showHint) {
                ToastTool.show(MResource.getString(R.string.net_error));
            }
            LogTool.d(logTag, "null");
            return map;
        }
        LogTool.d(logTag + " json", json);
        jsonI.defaultHandle(map);
        try {
            JSONObject jsonObject = new JSONObject(json);
            int errorCode = jsonObject.optInt(errorTag, 1);
            map.put(NetManager.ERROR_CODE, errorCode);
            if (errorCode == 1) {// 获取成功
                jsonI.successHandle(map, jsonObject);
            } else {
                String errorStr = optString(jsonObject, errorTag.equals("code") ? "msg" : "data", MResource.getString(R.string.net_error));
                map.put(NetManager.ERROR_DATA, errorStr);
                if (showHint) {
                    ToastTool.show(errorStr);
                }
                LogTool.d(logTag + " status " + errorCode, errorStr);
                jsonI.failedHandle(map);
            }
        } catch (Exception e) {
            jsonI.failedHandle(map);
            e.printStackTrace();
        }
        return map;
    }

    public static ArrayMap emptyParse(String json, String tag) {
        return parseJson(json, tag, new EmptyJsonI());
    }

    public static ArrayMap emptyParse(String json, String tag, boolean showHint) {
        return parseJson(json, tag, showHint, new EmptyJsonI());
    }

    private static MovieBean parseMovieFromDouBan(JSONObject object) throws JSONException {
        MovieBean bean = new MovieBean();
        bean.setDb_num(optString(object, "id"));
        bean.setName(optString(object, "title"));
        JSONObject images = object.optJSONObject("images");
        if (images != null) {
            bean.setImage(optString(images, "large"));
        }
        JSONObject rate = object.optJSONObject("rating");
        if (rate != null) {
            bean.setDb_rating((float) rate.optDouble("average"));
        }
        JSONArray genres = object.optJSONArray("genres");
        String temp = "";
        if (genres != null) {
            for (int i = 0; i < genres.length(); i++) {
                if (i != 0) {
                    temp += "/";
                }
                temp += genres.optString(i);
            }
        }
        bean.setMovieType(temp);
        bean.setPubdate(optString(object, "mainland_pubdate"));
        JSONArray durations = object.optJSONArray("durations");
        if (durations != null && durations.length() > 0) {
            bean.setDuration(durations.optString(0));
        }
        return bean;
    }

    public static ArrayMap searchMovie(String json) {
        final List<MovieBean> movies = new ArrayList<>();
        return parseJson(json, "code", "searchMovie", true, new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("movies", movies);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("subjects");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        MovieBean bean = parseMovieFromDouBan(object);
                        movies.add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getMovieDetail(String json) {
        return parseJson(json, "getMovieDetail", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                MovieBean bean = parseMovie(data);
                map.put("movie", bean);

                List<MovieListBean> movieListBeanList = new ArrayList<MovieListBean>();
                JSONArray relate_singles = data.optJSONArray("relate_singles");
                for (int i = 0; i < relate_singles.length(); i++) {
                    movieListBeanList.add(parseMovieList(relate_singles.optJSONObject(i)));
                }
                map.put("movieLists", movieListBeanList);
            }
        });
    }


    public static ArrayMap getMovieShareInfo(String json) {
        final List<String[]> infos = new ArrayList<>();
        return parseJson(json, "getMovieShareInfo", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("movies", infos);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        String[] strings = {optString(object, "directors"), optString(object, "sub_summary")};
                        infos.add(strings);
                    }
                }
            }
        });
    }

    public static MovieBean parseMovie(JSONObject object) {
        MovieBean bean = new MovieBean();
        bean.setId(object.optInt("id"));
        bean.setDb_num(optString(object, "db_num"));
        bean.setDone(object.optInt("is_done"));
        bean.setName(optString(object, "name"));
        bean.setEnglish_name(optString(object, "alt_name"));
        bean.setImage(optString(object, "img_url"));
        bean.setDirectors(optString(object, "directors"));
        bean.setPubdate(optString(object, "pubdate"));
        bean.setPubdateTimestamp(object.optLong("exhibit_pubdate", -2498320000L));
        bean.setYear(optString(object, "year"));
        bean.setCountries(optString(object, "countries"));
        bean.setDb_rating((float) object.optDouble("dbrating", 0));
        bean.setCasts(optString(object, "casts"));
        bean.setSummary(optString(object, "summary"));
        bean.setMovieType(optString(object, "genres"));
        bean.setDuration(optString(object, "duration"));
        JSONArray links = object.optJSONArray("links");
        if (links != null && links.length() != 0) {
            ArrayMap<String, String> urls = new ArrayMap<>();
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.optJSONObject(i);
                urls.put(optString(link, "name"), optString(link, "link"));
            }
            bean.setUrls(urls);
        }
        bean.setUpdate_time(object.optLong("update_time"));
        JSONArray photos = object.optJSONArray("photos");
        ArrayList<String> stagePhotos = new ArrayList<>();
        if (photos != null) {
            for (int i = 0; i < photos.length(); i++) {
                stagePhotos.add(photos.optString(i));
            }
        }
        bean.setStagePhoto(stagePhotos);
        bean.setMark_rating((float) object.optDouble("rating", 0));
        bean.setScriptWriter(optString(object, "writers"));
        bean.setWatchTime(object.optLong("watchdate"));
        bean.setNote(optString(object, "watchsay"));
        bean.setTrailer(optString(object, "trailer_urls"));
        return bean;
    }

    public static MovieListBean parseMovieList(JSONObject object) {
        MovieListBean bean = new MovieListBean();
        bean.setId(object.optInt("id"));
        bean.setName(optString(object, "name"));
        bean.setImg_url(optString(object, "img_url"));
        bean.setLikes(object.optInt("likes"));
        bean.setLiked(object.optInt("is_liked"));
        bean.setPublish_time(object.optInt("publish_time"));
        bean.setType(object.optInt("type", 1));
        bean.setShares(object.optInt("shares"));
        bean.setComments(object.optInt("comments"));
        bean.setHtmlCode(optString(object, "content"));
        bean.setCat_name(optString(object, "cat_name"));
        return bean;
    }

    public static CommentBean parseCommentBean(JSONObject object) {
        CommentBean bean = new CommentBean();
        bean.setId(object.optInt("id"));
        bean.setUid(object.optInt("uid"));
        bean.setSingleId(object.optInt("single_id"));
        bean.setName(optString(object, "nickname"));
        bean.setImgUrl(optString(object, "img_url"));
        bean.setLikes(object.optInt("likes"));
        bean.setLiked(object.optInt("is_liked"));
        bean.setContent(optString(object, "content"));
        bean.setPreContent(optString(object, "pre_content"));
        bean.setPreName(optString(object, "pre_nickname"));
        bean.setTimestamp(object.optInt("create_time"));
        return bean;
    }

    public static SingleBean parseSingleBean(JSONObject object) {
        SingleBean bean = new SingleBean();
        bean.setId(object.optInt("id"));
        bean.setName(optString(object, "name"));
        return bean;
    }

    public static SingleAccessBean parseSingleAccessBean(JSONObject object) {
        SingleAccessBean bean = new SingleAccessBean();
        bean.setSingle_id(object.optInt("usingle_id"));
        bean.setMovie_id(object.optInt("movie_id"));
        bean.setUpdate_time(object.optLong("create_time"));
        return bean;
    }

    public static ClassifyBean parseClassify(JSONObject object) {
        ClassifyBean bean = new ClassifyBean();
        bean.setId(object.optInt("id"));
        bean.setName(optString(object, "name"));
        bean.setImgUrl(optString(object, "img_url"));
        bean.setType(object.optInt("type", 2));
        return bean;
    }

    private static MovieCardBean parseDailyCardBean(JSONObject object) {
        MovieCardBean bean = new MovieCardBean();
        bean.setId(object.optInt("id"));
        bean.setImgUrl(optString(object, "img_url"));
        bean.setContent(optString(object, "content"));
        bean.setName(optString(object, "name"));
        bean.setDb_num(optString(object, "db_num"));
        bean.setLikes(object.optInt("likes"));
        bean.setLiked(object.optInt("is_liked"));
        bean.setShares(object.optInt("shares"));
        return bean;
    }

    public static ArrayMap getSMSCode(String json) {
        return parseJson(json, "getSMSCode", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                if (Session.isRelease) {
                    ToastTool.show("发送成功 ", Toast.LENGTH_LONG, 1);
                } else {
                    final String data = jsonObject.optString("data");
                    ToastTool.show("发送成功 " + data, Toast.LENGTH_LONG, 1);
                    ThreadManager.getInstance().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ToastTool.show("发送成功 " + data, Toast.LENGTH_LONG, 1);
                        }
                    }, 1100);
                }
            }
        });
    }

    public static ArrayMap login(String json) {
        return parseJson(json, "login", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                Session.uid = data.optInt("uid");
                Session.mUid = optString(data, "muid");
                SettingManager.getInstance().setUid(Session.uid);
                SettingManager.getInstance().setMUid(Session.mUid);
            }
        });
    }

    public static ArrayMap getUserInfo(String json) {
        return parseJson(json, "getUserInfo", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                SettingManager.getInstance().setAccount(optString(data, "account"));
                SettingManager.getInstance().setNickname(optString(data, "nickname"));
                SettingManager.getInstance().setUserHeadImgUrl(optString(data, "img_url"));
                SettingManager.getInstance().setIsWeixinBind(data.optInt("is_weixinbind") == 1);
                SettingManager.getInstance().setIsQQBind(data.optInt("is_qqbind") == 1);
                SettingManager.getInstance().setIsWeiboBind(data.optInt("is_weibobind") == 1);
            }
        });
    }

    public static ArrayMap getUserUnreadCount(String json) {
        return parseJson(json, "getUserUnreadCount", false, new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                SettingManager.getInstance().setCommentsCount(data.optInt("comments"));
                SettingManager.getInstance().setNotifyCount(data.optInt("notifications"));
            }
        });
    }

    public static ArrayMap updatePasswd(String json) {
        return parseJson(json, "updatePasswd", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                Session.uid = data.optInt("uid");
                Session.mUid = optString(data, "muid");
                SettingManager.getInstance().setUid(Session.uid);
                SettingManager.getInstance().setMUid(Session.mUid);
            }
        });
    }

    public static ArrayMap syncUserData(String json) {
        return parseJson(json, "syncUserData", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    List<MovieBean> movieBeans = new ArrayList<>();
                    JSONArray movieArray = data.optJSONArray("movies");
                    if (movieArray != null) {
                        for (int i = 0; i < movieArray.length(); i++) {
                            JSONObject object = movieArray.getJSONObject(i);
                            movieBeans.add(0, parseMovie(object));
                        }
                    }
                    MovieDbManager.getInstance().clear();
                    MovieDbManager.getInstance().insert(movieBeans);

                    List<SingleBean> singleBeans = new ArrayList<>();
                    JSONArray singleArray = data.optJSONArray("usersingles");
                    if (singleArray != null) {
                        for (int i = 0; i < singleArray.length(); i++) {
                            JSONObject object = singleArray.getJSONObject(i);
                            singleBeans.add(0, parseSingleBean(object));
                        }
                    }
                    MovieSingleDbManager.INSTANCE.clearSingle();
                    MovieSingleDbManager.INSTANCE.insertSingles(singleBeans);

                    List<SingleAccessBean> accessBeans = new ArrayList<>();
                    JSONArray accessArray = data.optJSONArray("usersinglemovies");
                    if (accessArray != null) {
                        for (int i = 0; i < accessArray.length(); i++) {
                            JSONObject object = accessArray.getJSONObject(i);
                            accessBeans.add(0, parseSingleAccessBean(object));
                        }
                    }
                    MovieSingleDbManager.INSTANCE.clearAccess();
                    MovieSingleDbManager.INSTANCE.insertAccess(accessBeans);
                }
            }
        });
    }

    public static ArrayMap addMovie(String json) {
        return parseJson(json, "addMovie", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                map.put("bean", parseMovie(data));
            }
        });
    }

    public static ArrayMap doneMovie(String json) {
        return parseJson(json, "doneMovie", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                long time = data.optLong("update_time");
                map.put("time", time);
            }
        });
    }

    public static ArrayMap deleteMovie(String json) {
        return parseJson(json, "deleteMovie", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                long time = data.optLong("update_time");
                map.put("time", time);
            }
        });
    }

    public static ArrayMap favoriteMovie(String json) {
        return parseJson(json, "favoriteMovie", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                long time = data.optLong("update_time");
                map.put("time", time);
            }
        });
    }

    public static ArrayMap rateMovie(String json) {
        return parseJson(json, "rateMovie", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                long time = data.optLong("update_time");
                map.put("time", time);
            }
        });
    }

    public static ArrayMap getMovieWord(String json) {
        return parseJson(json, "getMovieWord", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                SettingManager.getInstance().setMovieWord(optString(data, "title"), optString(data, "word"));
            }
        });
    }

    public static ArrayMap getMovieListSet(final String json) {
        final List<MovieListBean> movieLists = new ArrayList<>();
        return parseJson(json, "getMovieListSet", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", movieLists);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject jsonObject1 = data.getJSONObject(i);
                    MovieListBean bean = parseMovieList(jsonObject1);
                    movieLists.add(bean);
                }
            }
        });
    }

    public static ArrayMap getMovieListDetail(final String json) {
        return parseJson(json, "getMovieListDetail", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                map.put("movie_list", parseMovieList(data));
            }
        });
    }

    public static ArrayMap getMovieListComment(final String json, final boolean isLoad) {
        return parseJson(json, "getMovieListComment", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                MovieListBean movieListBean = parseMovieList(data);
                map.put("MovieListBean", movieListBean);
                ArrayList<CommentBean> list = new ArrayList<>();
                JSONArray present_comment = data.optJSONArray("present_comment");
                if (present_comment != null && present_comment.length() > 0) {
                    CommentBean cb = new CommentBean();
                    cb.setName("我的评论");
                    cb.setLocalType(1);
                    list.add(cb);
                    for (int i = 0; i < present_comment.length(); i++) {
                        cb = parseCommentBean(present_comment.optJSONObject(i));
                        cb.setSingleId(movieListBean.getId());
                        list.add(cb);
                    }
                }
                JSONArray hot_comments = data.optJSONArray("hot_comments");
                if (hot_comments != null && hot_comments.length() > 0) {
                    CommentBean cb = new CommentBean();
                    cb.setName("精彩评论");
                    cb.setLocalType(1);
                    list.add(cb);
                    for (int i = 0; i < hot_comments.length(); i++) {
                        cb = parseCommentBean(hot_comments.optJSONObject(i));
                        cb.setSingleId(movieListBean.getId());
                        list.add(cb);
                    }
                }
                if (!isLoad) {
                    CommentBean cb = new CommentBean();
                    cb.setName("最新评论");
                    cb.setLocalType(1);
                    list.add(cb);
                }
                JSONArray new_comments = data.optJSONArray("new_comments");
                if (new_comments != null) {
                    for (int i = 0; i < new_comments.length(); i++) {
                        CommentBean cb = parseCommentBean(new_comments.optJSONObject(i));
                        cb.setSingleId(movieListBean.getId());
                        list.add(cb);
                    }
                }
                map.put("list", list);
            }
        });
    }

    public static ArrayMap commentMovieList(final String json) {
        return parseJson(json, "commentMovieList", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                map.put("id", data.optInt("comment_id"));
            }
        });
    }

    public static ArrayMap getUserComments(final String json) {
        return parseJson(json, "getUserComments", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    ArrayList<CommentBean> list = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.optJSONObject(i);
                        CommentBean bean = new CommentBean();
                        bean.setId(object.optInt("new_comment_id"));
                        bean.setSingleId(object.optInt("single_id"));
                        bean.setName(optString(object, "new_nickname"));
                        bean.setImgUrl(optString(object, "new_img_url"));
                        bean.setContent(optString(object, "new_content"));
                        bean.setPreContent(optString(object, "content"));
                        bean.setType(object.optInt("type"));
                        bean.setTimestamp(object.optInt("create_time"));
                        list.add(bean);
                    }
                    map.put("list", list);
                }
            }
        });
    }

    public static ArrayMap getUserNotify(final String json) {
        return parseJson(json, "getUserNotify", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    ArrayList<CommentBean> list = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.optJSONObject(i);
                        CommentBean bean = new CommentBean();
                        bean.setId(object.optInt("comment_id"));
                        bean.setSingleId(object.optInt("single_id"));
                        bean.setName(optString(object, "new_nickname"));
                        bean.setImgUrl(optString(object, "new_img_url"));
                        bean.setContent(optString(object, "content"));
                        bean.setType(object.optInt("type"));
                        bean.setTimestamp(object.optInt("create_time"));
                        list.add(bean);
                    }
                    map.put("list", list);
                }
            }
        });
    }

    public static ArrayMap getBanner(String json) {
        final List<MovieListBean> movieLists = new ArrayList<>();
        return parseJson(json, "getBanner", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("banner", movieLists);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject jsonObject1 = data.getJSONObject(i);
                    MovieListBean bean = parseMovieList(jsonObject1);
                    movieLists.add(bean);
                }
            }
        });
    }

    public static ArrayMap getDiscoverList(String json) {
        final List<MovieListBean> movieLists = new ArrayList<>();
        return parseJson(json, "getDiscoverList", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", movieLists);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject jsonObject1 = data.getJSONObject(i);
                    MovieListBean bean = parseMovieList(jsonObject1);
                    movieLists.add(bean);
                }
            }
        });
    }

    public static ArrayMap getGroupClassify(String json) {
        final List<ClassifyBean> list = new ArrayList<>();
        return parseJson(json, "getGroupClassify", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject object1 = data.getJSONObject(i);
                    ClassifyBean groupBean = new ClassifyBean();
                    groupBean.setGroup(true);
                    groupBean.setName(optString(object1, "name"));
                    groupBean.setImgUrl(optString(object1, "img_url"));
                    list.add(groupBean);
                    groupBean.setList(new ArrayList<ClassifyBean>());
                    JSONArray cat = object1.getJSONArray("cat");
                    for (int j = 0; j < cat.length(); j++) {
                        JSONObject object2 = cat.getJSONObject(j);
                        ClassifyBean bean = parseClassify(object2);
                        groupBean.getList().add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getMovieSource(String json) {
        final List<JSONObject> videos = new ArrayList<>();
        final List<JSONObject> sources = new ArrayList<>();
        final List<JSONObject> others = new ArrayList<>();

        return parseJson(json, "getMovieSource", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("videos", videos);
                map.put("sources", sources);
                map.put("others", others);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                map.put("pre_url", optString(data, "pre_url"));
                JSONArray video_urls = data.optJSONArray("videos");
                for (int i = 0; i < video_urls.length(); i++) {
                    videos.add(video_urls.optJSONObject(i));
                }
                JSONArray sourse_urls = data.optJSONArray("sourses");
                for (int i = 0; i < sourse_urls.length(); i++) {
                    sources.add(sourse_urls.optJSONObject(i));
                }
                JSONArray otherArray = data.optJSONArray("others");
                for (int i = 0; i < otherArray.length(); i++) {
                    JSONObject object = otherArray.optJSONObject(i);
                    if (i == 0) {
                        object.put("is_other_head", true);
                    }
                    object.put("is_other", true);
                    others.add(object);
                }
            }
        });
    }

    public static ArrayMap getLikedMovieList(String json) {
        final List<MovieListBean> list = new ArrayList<>();
        return parseJson(json, "getLikedMovieList", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    list.add(parseMovieList(data.optJSONObject(i)));
                }
            }
        });
    }

    public static ArrayMap getMoviePhotos(String json) {
        final List<String[]> list = new ArrayList<>();
        return parseJson(json, "getMoviePhotos", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject object = data.optJSONObject(i);
                    if (object != null) {
                        list.add(new String[]{optString(object, "small"), optString(object, "large")});
                    }
                }
            }
        });
    }

    public static ArrayMap getMovieLines(String json) {
        final List<String> list = new ArrayList<>();
        return parseJson(json, "getMovieLines", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    list.add(data.optString(i).replace('\r', '\n'));
                }
            }
        });
    }

    public static ArrayMap getCinemaMovie(String json) {
        final List<MovieBean> movies = new ArrayList<>();
        return parseJson(json, "code", "getCinemaMovie", true, new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("movies", movies);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("subjects");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        MovieBean bean = parseMovieFromDouBan(object);
                        movies.add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getFileToken(String json) {
        return parseJson(json, "getFileToken", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("upload_token", "");
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.getJSONObject("data");
                map.put("upload_token", optString(data, "upload_token"));
            }
        });
    }

    public static ArrayMap changeHeadImg(String json) {
        return parseJson(json, "changeHeadImg", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.getJSONObject("data");
                map.put("img_url", optString(data, "img_url"));
            }
        });
    }

    public static ArrayMap newSingle(String json) {
        return parseJson(json, "newSingle", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.getJSONObject("data");
                map.put("id", data.optInt("id"));
            }
        });
    }

    public static ArrayMap addSingleAccess(String json) {
        return parseJson(json, "addSingleAccess", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.getJSONObject("data");
                map.put("time", data.optLong("update_time"));
                map.put("bean", parseMovie(data));
            }
        });
    }

    public static ArrayMap getDailyCard(String json) {
        final List<MovieCardBean> list = new ArrayList<>();
        return parseJson(json, "getDailyCard", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        MovieCardBean bean = parseDailyCardBean(object);
                        list.add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getLikedMovieCards(String json) {
        final List<MovieCardBean> list = new ArrayList<>();
        return parseJson(json, "getLikedMovieCards", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        MovieCardBean bean = parseDailyCardBean(object);
                        list.add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getMoreMovieCards(String json) {
        final List<MovieCardBean> list = new ArrayList<>();
        return parseJson(json, "getMoreMovieCards", new EmptyJsonI() {
            @Override
            public void defaultHandle(ArrayMap map) {
                map.put("list", list);
            }

            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONArray data = jsonObject.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        MovieCardBean bean = parseDailyCardBean(object);
                        list.add(bean);
                    }
                }
            }
        });
    }

    public static ArrayMap getMovieCardDetail(String json) {
        return parseJson(json, "getMovieCardDetail", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    MovieCardBean bean = parseDailyCardBean(data);
                    map.put("bean", bean);
                }
            }
        });
    }

    public static ArrayMap getWriterInfo(String json) {
        return parseJson(json, "getLikedMovieCards", new EmptyJsonI() {
            @Override
            public void successHandle(ArrayMap map, JSONObject jsonObject) throws JSONException {
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    JSONObject writer = data.optJSONObject("writer");
                    WriterBean writerBean = new WriterBean();
                    writerBean.setNickname(optString(writer, "nickname"));
                    writerBean.setImgUrl(optString(writer, "img_url"));
                    writerBean.setSaying(optString(writer, "writer_saying"));
                    writerBean.setSingleCount((writer.optInt("single_count")));
                    writerBean.setSingleLikes((writer.optInt("single_likes")));
                    map.put("writer", writerBean);

                    JSONArray singles = data.optJSONArray("singles");
                    List<MovieListBean> list = new ArrayList<>();
                    for (int i = 0; i < singles.length(); i++) {
                        JSONObject object = singles.getJSONObject(i);
                        list.add(parseMovieList(object));
                    }
                    map.put("list", list);
                }
            }
        });
    }
}
