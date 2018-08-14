package com.intlime.mark.tools

import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.tencent.bugly.crashreport.CrashReport
import com.zhuge.analysis.stat.ZhugeSDK
import org.json.JSONObject

/**
 * Created by wtuadn on 16/05/06.
 */
object ZhugeTool {
    private val appkey = if (Session.isRelease) "9400e10c39104e96b1fb817b9c262e9b" else "cf9bda977aeb42c998ede96d9befbf97"
    private var market: String? = null
    private var isUserIdentified = false

    fun getMarket(): String? {
        if (market.isNullOrEmpty()) {
            market = AppEngine.getMarket()
        }
        return market
    }

    init {
        try {
//            ZhugeSDK.getInstance().openDebug()
//            ZhugeSDK.getInstance().openLog()
            ZhugeSDK.getInstance().init(AppEngine.getContext(), appkey, getMarket())
            identifyUser()
        } catch(e: Exception) {
        }
    }

    private fun identifyUser() {
        if (Session.uid <= 0) {
            isUserIdentified = false
            return
        }
        if (isUserIdentified) return
        isUserIdentified = true
        try {
            val name = SettingManager.getInstance().nickname
            val kv = JSONObject()
            kv.put("avatar", SettingManager.getInstance().userHeadImgUrl)
            kv.put("name", if (name.isNullOrEmpty()) SettingManager.getInstance().account else name)
            ZhugeSDK.getInstance().identify(AppEngine.getContext(), Session.uid.toString(), kv)
            CrashReport.setUserId(Session.uid.toString())
        } catch(e: Exception) {
        }
    }

    fun onResume() {
        identifyUser()
        try {
            ZhugeSDK.getInstance().init(AppEngine.getContext(), appkey, getMarket())
        } catch(e: Exception) {
        }
    }

    fun onDestroy() {
        try {
            ZhugeSDK.getInstance().flush(AppEngine.getContext())
        } catch(e: Exception) {
        }
    }

    /**
     * @param event 事件名
     * @param jsonObject 参数
     */
    fun track(event: String, jsonObject: JSONObject? = null) {
        try {
            if (jsonObject == null) {
                ZhugeSDK.getInstance().track(AppEngine.getContext(), event);
            } else {
                ZhugeSDK.getInstance().track(AppEngine.getContext(), event, jsonObject);
            }
        } catch(e: Exception) {
        }
    }

    fun getTrackArg(vararg pair: Pair<String?, String?>): JSONObject {
        val jsonObject = JSONObject()
        pair.forEach {
            try {
                jsonObject.put(it.first ?: "", it.second ?: "")
            } catch (ignore: Exception) {
            }
        }
        return jsonObject
    }
}