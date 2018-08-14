package com.intlime.mark.activitys

import android.graphics.Color
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import com.intlime.mark.R
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.CryptTool
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.MResource
import com.intlime.mark.tools.ToastTool
import kotlinx.android.synthetic.main.activity_v_code_layout.*
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.onClick
import org.jetbrains.anko.startActivity
import java.util.*

/**
 * Created by root on 16-1-4.
 */
class VCodeActivity : BaseActivity() {
    private var type: Int = 0 //1为注册，2为第三方登录后绑定手机，3为重设密码
    private var accountStr: String? = null
    private var passwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountStr = intent.getStringExtra("account")
        type = intent.getIntExtra("type", 0)
        passwd = intent.getStringExtra("password")
        if (accountStr == null || type <= 0) {
            finish()
            return
        }
        setContentView(R.layout.activity_v_code_layout)
        WWindowManager.getInstance().showSoftInput(v_code)
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = "手机号验证"
        toolbar.setNavigationIcon(R.drawable.back_icon)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initOther() {
        account.append(accountStr)
        get_code.onClick {
            getSMSCode(get_code, accountStr!!, true)
        }
        getSMSCode(get_code, accountStr!!)
        if (type == 2) {
            button.text = "下一步"
        }
        button.onClick {
            val vCode = v_code.text.toString()
            if (TextUtils.isEmpty(vCode)) {
                ToastTool.show("验证码不能为空", Toast.LENGTH_SHORT, 1);
                return@onClick;
            }
            when (type) {
                1 -> {
                    DialogTool.showWaitDialog("注册中")
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("phonenumber", CryptTool.encrypt(accountStr)))
                    params.add(BasicNameValuePair("passwd", CryptTool.encrypt(passwd!!)))
                    params.add(BasicNameValuePair("verify_code", vCode))
                    params.add(BasicNameValuePair("type", "1"))
                    NetManager.getInstance().updatePasswd(params, object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<Any, Any>) {
                            ToastTool.show("注册成功")
                            SettingManager.getInstance().account = accountStr
                            startActivity<MainActivity>(ANIMATION to true)
                        }
                    })
                }
                2 -> {
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<Any, Any>) {
                            startActivity<SetPassActivity>("account" to accountStr!!)
                        }
                    }
                    DialogTool.showWaitDialog("验证中", DialogTool.CANCEL_ON_BACK, callback)
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("phonenumber", CryptTool.encrypt(accountStr)))
                    params.add(BasicNameValuePair("verify_code", vCode))
                    NetManager.getInstance().bindPhoneNumber(params, callback)
                }
                3 -> {
                    val callback = object : NetRequestCallBack() {
                        override fun onSuccess(result: ArrayMap<Any, Any>) {
                            ToastTool.show("重设成功")
                            if (Session.uid > 0) {
                                DialogTool.dismissWaitDialog()
                                finish()
                                WWindowManager.getInstance().currentActivity?.finish()
                            } else {
                                AccountLoginActivity.syncData(object : NetRequestCallBack() {
                                    override fun onSuccess(result: ArrayMap<Any, Any>) {
                                        startActivity<MainActivity>(ANIMATION to true)
                                    }
                                })
                            }
                        }

                        override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                            DialogTool.dismissWaitDialog()
                        }
                    }
                    DialogTool.showWaitDialog("重设中", DialogTool.CANCEL_ON_BACK, callback)
                    val params = ArrayList<NameValuePair>()
                    params.add(BasicNameValuePair("phonenumber", CryptTool.encrypt(accountStr)))
                    params.add(BasicNameValuePair("passwd", CryptTool.encrypt(passwd!!)))
                    params.add(BasicNameValuePair("verify_code", vCode))
                    params.add(BasicNameValuePair("type", "2"))
                    NetManager.getInstance().updatePasswd(params, callback)
                }
            }
        }
    }

    private fun getSMSCode(textView: TextView, account: String, requset: Boolean = false) {
        textView.isEnabled = false
        textView.setTextColor(MResource.getColor(R.color.gray_text_color))
        textView.text = "重新发送(60s)"
        textView.setTextColor(Color.parseColor("#818c91"))
        textView.setBackgroundResource(R.drawable.button_sms_counting_bg)
        val timeCount = intArrayOf(60)
        textView.postDelayed(object : Runnable {
            override fun run() {
                synchronized (timeCount) {
                    timeCount[0]--
                    if (timeCount[0] <= 0) {
                        textView.text = "获取验证码"
                        textView.setTextColor(Color.WHITE)
                        textView.setBackgroundResource(R.drawable.button_sms_bg)
                        textView.isEnabled = true
                        return
                    }
                }
                textView.text = String.format("重新发送(%ds)", timeCount[0])
                textView.postDelayed(this, 1000)
            }
        }, 1000)
        if (requset) {
            val t = if (type == 3) 2 else 1
            NetManager.getInstance().getSMSCode(account, t, object : NetRequestCallBack() {
                override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                    synchronized (timeCount) {
                        timeCount[0] = 0
                    }
                }
            })
        }
    }
}
