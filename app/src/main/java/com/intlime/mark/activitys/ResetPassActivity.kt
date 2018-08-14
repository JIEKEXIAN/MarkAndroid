package com.intlime.mark.activitys

import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.widget.Toast
import com.intlime.mark.R
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.ToastTool
import kotlinx.android.synthetic.main.activity_register_layout.*
import org.jetbrains.anko.onClick
import org.jetbrains.anko.startActivity

/**
 * Created by root on 15-12-24.
 */
class ResetPassActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_layout)
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = "重设密码"
        toolbar.setNavigationIcon(R.drawable.back_icon)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initOther() {
        WWindowManager.getInstance().showSoftInput(account)
        next.onClick {
            WWindowManager.getInstance().hideInput()
            val accountStr = account!!.text.toString()
            val passwd = passwd!!.text.toString()

            if (TextUtils.isEmpty(accountStr)) {
                ToastTool.show("手机号不能为空", Toast.LENGTH_SHORT, 1)
                return@onClick
            }
            if (TextUtils.isEmpty(passwd)) {
                ToastTool.show("密码不能为空", Toast.LENGTH_SHORT, 1)
                return@onClick
            }
            NetManager.getInstance().getSMSCode(accountStr, 2,object : NetRequestCallBack() {
                override fun onSuccess(result: ArrayMap<*, *>?) {
                    startActivity<VCodeActivity>("account" to accountStr, "password" to passwd, "type" to 3)
                }
            })
        }
    }
}
