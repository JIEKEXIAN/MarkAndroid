package com.intlime.mark.activitys

import android.content.Intent
import android.os.Bundle
import android.support.v4.util.ArrayMap
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.CryptTool
import com.intlime.mark.tools.DialogTool
import kotlinx.android.synthetic.main.activity_set_passwd_layout.*
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.onClick
import java.util.*

/**
 * Created by root on 15-12-24.
 */
class SetPassActivity : BaseActivity() {
    private var accountStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountStr = intent.getStringExtra("account")
        if (accountStr == null) {
            finish()
            return
        }
        setContentView(R.layout.activity_set_passwd_layout)
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = "设置密码"
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.back_icon)
    }

    override fun initOther() {
        WWindowManager.getInstance().showSoftInput(passwd)
        button.onClick {
            val callback = object : NetRequestCallBack() {
                override fun onDefault() {
                    DialogTool.dismissWaitDialog()
                }

                override fun onSuccess(result: ArrayMap<Any, Any>) {
                    val intent = Intent(AppEngine.getContext(), UserSettingActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    SettingManager.getInstance().account = accountStr
                }
            }
            DialogTool.showWaitDialog("设置中", DialogTool.CANCEL_ON_BACK, callback)
            val params = ArrayList<NameValuePair>()
            params.add(BasicNameValuePair("passwd", CryptTool.encrypt(passwd.text.toString())))
            NetManager.getInstance().setPasswd(params, callback)
        }
    }
}
