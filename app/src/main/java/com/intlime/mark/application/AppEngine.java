package com.intlime.mark.application;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.intlime.mark.tools.LogTool;
import com.intlime.mark.tools.MResource;
import com.mcxiaoke.packer.helper.PackerNg;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.smtt.sdk.QbSdk;
import com.tendcloud.tenddata.TCAgent;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by wtu on 2015/05/14 014.
 */
public class AppEngine extends android.app.Application /*android.support.multidex.MultiDexApplication*/ {
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static String getMarket() {
        String market = PackerNg.getMarket(context);
        if (TextUtils.isEmpty(market)) market = "_default";
        return market;
    }

    @Nullable
    private static String getProcessName(Context context) {
        String processName = null;
        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        if (am.getRunningAppProcesses() != null) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == android.os.Process.myPid()) {
                    processName = info.processName;
                    break;
                }
            }
        }
        return processName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        CrashReport.initCrashReport(context, Session.isRelease ? "900015583" : "900029068", false);

        String processName = getProcessName(context);
        if (TextUtils.isEmpty(processName) || processName.equals(getPackageName())) {//主进程
            MResource.init(context);
            String market = getMarket();

            MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(
                    context, "5661a1b367e58ec5ec0038c1", market, MobclickAgent.EScenarioType.E_UM_NORMAL, false));

            TCAgent.LOG_ON = false;
            TCAgent.setPushDisabled();
            TCAgent.setReportUncaughtExceptions(false);
            TCAgent.init(context, "69932DB5790CF526F3348A72074E86C8", market);

//            if (Session.isBugtagsOn) {
//                BugtagsOptions options = new BugtagsOptions.Builder().
//                        trackingLocation(false).//是否获取位置，默认 true
//                        trackingCrashLog(false).//是否收集crash，默认 true
//                        trackingConsoleLog(true).//是否收集console log，默认 true
//                        trackingUserSteps(true).//是否收集用户操作步骤，默认 true
//                        trackingNetworkURLFilter("(.*)").//自定义网络请求跟踪的 url 规则，默认 null
//                        build();
//                Bugtags.start("4573a73e90e109cac57010455f15670c", this, Bugtags.BTGInvocationEventBubble, options);
//            }

            QbSdk.preInit(context);
        }

        LogTool.d("APP_LIFE", "AppEngine onCreate");
    }
}
