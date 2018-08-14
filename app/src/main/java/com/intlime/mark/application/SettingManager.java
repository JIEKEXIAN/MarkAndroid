package com.intlime.mark.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置管理
 *
 * @author wtuadn
 * @version 1.0
 * @date 2015-3-17
 */
public class SettingManager {
    private static SettingManager INSTANCE;
    private SharedPreferences preference;
    private final String PUBLICSETTING = "publicsetting";

    public static SettingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingManager();
        }
        return INSTANCE;
    }

    private SettingManager() {
        initIfNeed();
    }

    private void initIfNeed() {
        if (preference == null) {
            preference = AppEngine.getContext().getSharedPreferences(PUBLICSETTING, Context.MODE_PRIVATE);
        }
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        boolean bool = hasToShowGuide();
        preference.edit().clear().apply();
        setHasToShowGuide(bool);
    }

    /**
     * 获取当前app版本code
     */
    public int getVersionCode() {
        int versionCode = 0;
        try {
            PackageManager manager = AppEngine.getContext().getPackageManager();
            PackageInfo info = manager.getPackageInfo(AppEngine.getContext().getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取当前app版本名
     */
    public String getVersionName() {
        String versionName = "1.0";
        try {
            PackageManager manager = AppEngine.getContext().getPackageManager();
            PackageInfo info = manager.getPackageInfo(AppEngine.getContext().getPackageName(), 0);
            versionName = info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 保存账号
     */
    public void setAccount(String lastAccount) {
        preference.edit().putString("LastAccount", lastAccount).apply();
    }

    /**
     * 获取账号
     */
    public String getAccount() {
        return preference.getString("LastAccount", "");
    }

    public int getUid() {
        return preference.getInt("uid", 0);
    }

    public void setUid(int uid) {
        preference.edit().putInt("uid", uid).apply();
    }

    public String getMUid() {
        return preference.getString("muid", "");
    }

    public void setMUid(String muid) {
        preference.edit().putString("muid", muid).apply();
    }

    public String getNickname() {
        return preference.getString("nickname", "");
    }

    public void setNickname(String nickname) {
        preference.edit().putString("nickname", nickname).apply();
    }

    public String getUserHeadImgUrl() {
        return preference.getString("UserHeadImgUrl", "");
    }

    public void setUserHeadImgUrl(String userHeadImgUrl) {
        preference.edit().putString("UserHeadImgUrl", userHeadImgUrl).apply();
    }

    public void setIsWeixinBind(boolean isWeixinBind) {
        preference.edit().putBoolean("isWeixinBind", isWeixinBind).apply();
    }

    public boolean getIsWeixinBind() {
        return preference.getBoolean("isWeixinBind", false);
    }

    public void setIsQQBind(boolean isQQBind) {
        preference.edit().putBoolean("IsQQBind", isQQBind).apply();
    }

    public boolean getIsQQBind() {
        return preference.getBoolean("IsQQBind", false);
    }

    public void setIsWeiboBind(boolean isWeiboBind) {
        preference.edit().putBoolean("IsWeiboBind", isWeiboBind).apply();
    }

    public boolean getIsWeiboBind() {
        return preference.getBoolean("IsWeiboBind", false);
    }

    public void setMovieWord(String movieName, String word) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(movieName);
        jsonArray.put(word);
        preference.edit().putString("MovieWord", jsonArray.toString()).apply();
    }

    public String[] getMovieWord() {
        String[] strings = {"", ""};
        try {
            JSONArray jsonArray = new JSONArray(preference.getString("MovieWord", "[\"\",\"\"]"));
            strings[0] = jsonArray.optString(0);
            strings[1] = jsonArray.optString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return strings;
    }

    /**
     * 获取已读影单
     */
    public List<Integer> getReadedMovieList() {
        List<Integer> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(preference.getString("ReadedMovieList", "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getInt(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void setReadedMovieList(List<Integer> list) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            jsonArray.put(list.get(i));
        }
        preference.edit().putString("ReadedMovieList", jsonArray.toString()).apply();
    }

    public void setBaiduYunCookie(String baiduYunCookie) {
        preference.edit().putString("baiduYunCookie", baiduYunCookie).apply();
    }

    public String getBaiduYunCookie() {
        return preference.getString("baiduYunCookie", " ");
    }

    public void setTodoViewMode(int TodoViewMode) {
        preference.edit().putInt("TodoViewMode", TodoViewMode).apply();
    }

    public int getTodoViewMode() {
        return preference.getInt("TodoViewMode", 0);
    }

    public void setDoneViewMode(int DoneViewMode) {
        preference.edit().putInt("DoneViewMode", DoneViewMode).apply();
    }

    public int getDoneViewMode() {
        return preference.getInt("DoneViewMode", 0);
    }

    public boolean needUpdateData() {
        return preference.getBoolean("needUpdateData", false);
    }

    public void setNeedUpdateData(boolean needUpdateData) {
        preference.edit().putBoolean("needUpdateData", needUpdateData).apply();
    }

    /**
     * 是否显示引导页
     */
    public boolean hasToShowGuide() {
        return preference.getBoolean("hasToShowGuide", true);
    }

    /**
     * 是否显示引导页
     */
    public void setHasToShowGuide(boolean hasToShowGuide) {
        preference.edit().putBoolean("hasToShowGuide", hasToShowGuide).apply();
    }

    /**
     * 是否需要显示电影卡片编辑台词提示
     */
    public boolean hasToShowMovieWordEdtiHint() {
        return preference.getBoolean("hasToShowMovieWordEditHint", true);
    }

    /**
     * 是否需要显示电影卡片编辑台词提示
     */
    public void setHasToShowMovieWordEdtiHint(boolean hasToBindPhoto) {
        preference.edit().putBoolean("hasToShowMovieWordEditHint", hasToBindPhoto).apply();
    }

    public void setNotifySwitch(boolean NotifySwitch) {
        preference.edit().putBoolean("NotifySwitch", NotifySwitch).apply();
    }

    public boolean getNotifySwitch() {
        return preference.getBoolean("NotifySwitch", true);
    }

    /**
     * 长按提示
     */
    public boolean canShowLongPressHint() {
        return preference.getBoolean("canShowLongPressHint", true);
    }

    public void setCanShowLongPressHint(boolean canShowLongPressHint) {
        preference.edit().putBoolean("canShowLongPressHint", canShowLongPressHint).apply();
    }

    /**
     * 电影详情分享提示
     */
    public boolean canShowMovieDetailShareHint() {
        return preference.getBoolean("canShowMovieDetailShareHint", true);
    }

    public void setCanShowMovieDetailShareHint(boolean canShowMovieDetailShareHint) {
        preference.edit().putBoolean("canShowMovieDetailShareHint", canShowMovieDetailShareHint).apply();
    }

    public String getPushId() {
        return preference.getString("pushId", "");
    }

    public void setPushId(String pushId) {
        preference.edit().putString("pushId", pushId).apply();
    }

    public int getCommentsCount(){
        return  preference.getInt("CommentsCount",0);
    }

    public void setCommentsCount(int count){
        preference.edit().putInt("CommentsCount",count).apply();
    }

    public int getNotifyCount(){
        return  preference.getInt("NotifyCount",0);
    }

    public void setNotifyCount(int count){
        preference.edit().putInt("NotifyCount",count).apply();
    }
}
