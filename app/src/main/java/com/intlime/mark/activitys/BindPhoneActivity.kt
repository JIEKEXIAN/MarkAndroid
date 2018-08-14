package com.intlime.mark.activitys

import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.view.View
import com.intlime.mark.R
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.ToastTool
import kotlinx.android.synthetic.main.activity_bind_phone_layout.*
import org.jetbrains.anko.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor

/**
 * Created by root on 16-1-14.
 */
class BindPhoneActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind_phone_layout)
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = "绑定手机号"
        toolbar.setNavigationIcon(R.drawable.back_icon)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun initOther() {
        val accountStr = SettingManager.getInstance().account
        if (TextUtils.isEmpty(accountStr)) {
            next.onClick {
                WWindowManager.getInstance().hideInput()
                if (TextUtils.isEmpty(account.text)) {
                    ToastTool.show("手机号不能为空")
                    return@onClick
                }
                NetManager.getInstance().getSMSCode(account.text.toString(), 1, object : NetRequestCallBack() {
                    override fun onSuccess(result: ArrayMap<*, *>?) {
                        startActivity<VCodeActivity>("account" to account.text.toString(), "type" to 2)
                    }
                })
            }
        } else {
            account.visibility = View.GONE
            bind_phone.text = accountStr.replaceRange(3, 7, "****")
            bind_phone.textColor = resources.getColor(R.color.a_main_text_color)
            next.text = "重设密码"
            next.onClick {
                startActivity<ResetPassActivity>()
            }
        }
        WWindowManager.getInstance().showSoftInput(account)
    }
}
