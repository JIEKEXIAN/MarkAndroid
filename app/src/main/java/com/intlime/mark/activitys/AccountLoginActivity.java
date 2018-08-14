package com.intlime.mark.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.Session;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.CryptTool;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.db.MovieDbManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 15-12-3.
 */
public class AccountLoginActivity extends BaseActivity {
    private EditText accountEdit;
    private EditText passwdEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_login_layout);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle("登录");
        toolbar.setNavigationIcon(R.drawable.back_icon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void initOther() {
        accountEdit = (EditText) findViewById(R.id.account);
        passwdEdit = (EditText) findViewById(R.id.passwd);
//        if (Session.isDebug) {
//            accountEdit.setText("18084959136");
//            passwdEdit.setText("072794");
//        }
        WWindowManager.getInstance().showSoftInput(accountEdit);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login:
                final String account = accountEdit.getText().toString();
                String passwd = passwdEdit.getText().toString();

                if (TextUtils.isEmpty(account)) {
                    ToastTool.show("手机号不能为空");
                    break;
                }
                if (TextUtils.isEmpty(passwd)) {
                    ToastTool.show("密码不能为空");
                    break;
                }
                DialogTool.showWaitDialog("正在登录");
                final List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("account", account));
                params.add(new BasicNameValuePair("passwd", CryptTool.encrypt(passwd)));
                NetManager.getInstance().login(params, new NetRequestCallBack() {

                    @Override
                    public void onSuccess(ArrayMap result) {
                        syncData(new NetRequestCallBack() {
                            @Override
                            public void onSuccess(ArrayMap result) {
                                Intent intent = new Intent(AppEngine.getContext(), MainActivity.class);
                                intent.putExtra(ANIMATION, true);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onFail(ArrayMap result, int error_code) {
                        DialogTool.dismissWaitDialog();
                    }
                });
                break;
            case R.id.reset_passwd:
                Intent intent3 = new Intent(this, ResetPassActivity.class);
                startActivity(intent3);
                break;
        }
    }

    public static void syncData(final NetRequestCallBack callBack) {
        List<NameValuePair> params = new ArrayList<>();
        final boolean[] dialogBools = {false, false};//两个接口都回调后才取消等待框
        final boolean[] loginBools = {false, false};//两个接口都成功后才登陆成功
        NetManager.getInstance().syncUserData(params, new NetRequestCallBack() {
            @Override
            public void onDefault() {
                synchronized (dialogBools) {
                    dialogBools[0] = true;
                    if (dialogBools[1]) {
                        DialogTool.dismissWaitDialog();
                    }
                }
            }

            @Override
            public void onSuccess(ArrayMap result) {
                synchronized (loginBools) {
                    loginBools[0] = true;
                    if (loginBools[1]) {
                        callBack.onSuccess(result);
                    }
                }
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                Session.uid = 0;
                Session.mUid = "";
                SettingManager.getInstance().clear();
                MovieDbManager.getInstance().clear();
            }
        });
        NetManager.getInstance().getUserInfo(new NetRequestCallBack() {
            @Override
            public void onDefault() {
                synchronized (dialogBools) {
                    dialogBools[1] = true;
                    if (dialogBools[0]) {
                        DialogTool.dismissWaitDialog();
                    }
                }
            }

            @Override
            public void onSuccess(ArrayMap result) {
                synchronized (loginBools) {
                    loginBools[1] = true;
                    if (loginBools[0]) {
                        callBack.onSuccess(result);
                    }
                }
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                Session.uid = 0;
                Session.mUid = "";
                SettingManager.getInstance().clear();
                MovieDbManager.getInstance().clear();
            }
        });
    }
}
