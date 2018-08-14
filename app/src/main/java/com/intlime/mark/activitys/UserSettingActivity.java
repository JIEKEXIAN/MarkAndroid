package com.intlime.mark.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.intlime.mark.R;
import com.intlime.mark.application.AppEngine;
import com.intlime.mark.application.SettingManager;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.network.MyUploadManager;
import com.intlime.mark.network.NetManager;
import com.intlime.mark.network.NetRequestCallBack;
import com.intlime.mark.tools.CryptTool;
import com.intlime.mark.tools.DialogTool;
import com.intlime.mark.tools.ImageTool;
import com.intlime.mark.tools.JsonTool;
import com.intlime.mark.tools.ToastTool;
import com.intlime.mark.tools.UmengTool;
import com.intlime.mark.tools.glide.CircleTransform;
import com.intlime.mark.view.HeadImgClipView;
import com.intlime.mark.view.widget.SwitchButton;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;

/**
 * Created by root on 16-1-4.
 */
public class UserSettingActivity extends BaseActivity implements PlatformActionListener {
    private final static int PIC = 1177;
    private final static int TAKE = 1178;
    private ImageView headImg;
    private TextView nickname;
    private TextView account;
    private SwitchButton weixinSwitch;
    private SwitchButton qqSwitch;
    private SwitchButton weiboSwitch;
    private boolean canHandleSwitth = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting_layout);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        toolbar.setTitle("账号管理");
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
        headImg = (ImageView) findViewById(R.id.head);
        registerForContextMenu(((ViewGroup) headImg.getParent()).getChildAt(0));
        nickname = (TextView) findViewById(R.id.nickname);
        account = (TextView) findViewById(R.id.account);
        weixinSwitch = (SwitchButton) findViewById(R.id.weixin_switch);
        qqSwitch = (SwitchButton) findViewById(R.id.qq_switch);
        weiboSwitch = (SwitchButton) findViewById(R.id.weibo_switch);

        weixinSwitch.setChecked(SettingManager.getInstance().getIsWeixinBind());
        weixinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (canHandleSwitth) {
                    if (isChecked) {
                        doWeixinLogin();
                    } else {
                        unbindWeixin();
                    }
                }
                canHandleSwitth = true;
            }
        });
        qqSwitch.setChecked(SettingManager.getInstance().getIsQQBind());
        qqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (canHandleSwitth) {
                    if (isChecked) {
                        doQQLogin();
                    } else {
                        unbindQQ();
                    }
                }
                canHandleSwitth = true;
            }
        });
        weiboSwitch.setChecked(SettingManager.getInstance().getIsWeiboBind());
        weiboSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (canHandleSwitth) {
                    if (isChecked) {
                        doWeiboLogin();
                    } else {
                        unbindWeibo();
                    }
                }
                canHandleSwitth = true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String nicknameStr = SettingManager.getInstance().getNickname();
        if (TextUtils.isEmpty(nicknameStr)) {
            nicknameStr = "设置昵称";
        }
        nickname.setText(nicknameStr);
        String headUrl = SettingManager.getInstance().getUserHeadImgUrl();
        if (TextUtils.isEmpty(headUrl)) {
            headImg.setImageResource(R.drawable.setting_header_icon);
        } else {
            Glide.with(this)
                    .load(headUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transform(new CircleTransform(this))
                    .into(headImg);
        }
        weixinSwitch.setEnabled(true);
        qqSwitch.setEnabled(true);
        weiboSwitch.setEnabled(true);
        String accountStr = SettingManager.getInstance().getAccount();
        if (!TextUtils.isEmpty(accountStr)) {
            account.setText(String.format("%s****%s", accountStr.substring(0, 3), accountStr.substring(7, 11)));
        } else {
            account.setText("未绑定");
            boolean isWeixinBind = SettingManager.getInstance().getIsWeixinBind();
            boolean isQQBind = SettingManager.getInstance().getIsQQBind();
            boolean isWeiboBind = SettingManager.getInstance().getIsWeiboBind();
            if (isWeixinBind && !isQQBind && !isWeiboBind)
                weixinSwitch.setEnabled(false);
            if (!isWeixinBind && isQQBind && !isWeiboBind)
                qqSwitch.setEnabled(false);
            if (!isWeixinBind && !isQQBind && isWeiboBind)
                weiboSwitch.setEnabled(false);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bind_phone:
                startActivity(new Intent(this, BindPhoneActivity.class));
                break;
            case R.id.change_head:
                openContextMenu(((ViewGroup) headImg.getParent()).getChildAt(0));
                if (TextUtils.isEmpty(MyUploadManager.getToken())) {
                    NetManager.getInstance().getFileToken(new NetRequestCallBack() {
                        @Override
                        public void onSuccess(ArrayMap result) {
                            MyUploadManager.setToken((String) result.get("upload_token"));
                        }
                    });
                }
                break;
            case R.id.change_nickname:
                startActivity(new Intent(this, ChangeNicknameActivity.class));
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, "拍照");
        menu.add(0, 1, 0, "从手机相册选择");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (item.getItemId() == 0) {
                if (UmengTool.checkPermission(this,
                        Manifest.permission.CAMERA, UmengTool.REQUEST_CAMERA)) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = new File(getExternalCacheDir(), "take_photo_temp.jpg");
                    Uri imageUri = Uri.fromFile(file);
                    //指定照片保存路径（SD卡
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(cameraIntent, TAKE);
                }
            } else {
                if (UmengTool.checkPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, PIC);
                }
            }
        } else {
            ToastTool.show("存储空间不足");
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PIC);
            } else {
                ToastTool.show("获取权限失败");
            }
        } else if (requestCode == UmengTool.REQUEST_CAMERA) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(getExternalCacheDir(), "take_photo_temp.jpg");
            Uri imageUri = Uri.fromFile(file);
            //指定照片保存路径（SD卡
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, TAKE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE && resultCode == RESULT_OK) {
            String imgPath = getExternalCacheDir() + "/take_photo_temp.jpg";
            new HeadImgClipView(this, imgPath);
        } else if (requestCode == PIC && resultCode == RESULT_OK) {
            String imgPath = ImageTool.getPath(AppEngine.getContext(), data.getData());
            if (TextUtils.isEmpty(imgPath)) {
                ToastTool.show("出错啦");
                return;
            }
            new HeadImgClipView(this, imgPath);
        } else if (UmengTool.isInited()) {
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onChangeSuccess(String url) {
        Glide.with(headImg.getContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .transform(new CircleTransform(AppEngine.getContext()))
                .into(headImg);
        sendBroadcast(new Intent(RELOAD_USER_CENTER_ACTION));
    }

    private void unbindWeixin() {
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                ToastTool.show("解绑成功");
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        weixinSwitch.setChecked(false);
                        SettingManager.getInstance().setIsWeixinBind(false);
                    }
                });
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                canHandleSwitth = false;
                weixinSwitch.setChecked(true);
            }
        };
        DialogTool.showWaitDialog("正在解绑", DialogTool.CANCEL_ON_BACK, callback);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("type", "1"));
        NetManager.getInstance().unbindThirdPart(params, callback);
    }

    private void unbindQQ() {
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                ToastTool.show("解绑成功");
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        qqSwitch.setChecked(false);
                        SettingManager.getInstance().setIsQQBind(false);
                    }
                });
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                canHandleSwitth = false;
                qqSwitch.setChecked(true);
            }
        };
        DialogTool.showWaitDialog("正在解绑", DialogTool.CANCEL_ON_BACK, callback);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("type", "2"));
        NetManager.getInstance().unbindThirdPart(params, callback);
    }

    private void unbindWeibo() {
        NetRequestCallBack callback = new NetRequestCallBack() {
            @Override
            public void onDefault() {
                DialogTool.dismissWaitDialog();
            }

            @Override
            public void onSuccess(ArrayMap result) {
                ToastTool.show("解绑成功");
                ThreadManager.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        weiboSwitch.setChecked(false);
                        SettingManager.getInstance().setIsWeiboBind(false);
                    }
                });
            }

            @Override
            public void onFail(ArrayMap result, int error_code) {
                canHandleSwitth = false;
                weiboSwitch.setChecked(true);
            }
        };
        DialogTool.showWaitDialog("正在解绑", DialogTool.CANCEL_ON_BACK, callback);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("type", "3"));
        NetManager.getInstance().unbindThirdPart(params, callback);
    }

    private void doWeixinLogin() {
        if (!WWindowManager.getInstance().isPkgInstalled("com.tencent.mm")) {
            ThreadManager.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ToastTool.show("还没有安装微信");
                    canHandleSwitth = false;
                    weixinSwitch.setChecked(false);
                }
            }, 300);
            return;
        }
        UmengTool.getInstance().doThirdPartLogin(this, SHARE_MEDIA.WEIXIN, new UMAuthListener() {
            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                if (map != null && map.containsKey("unionid") && map.containsKey("nickname") && map.containsKey("headimgurl")) {
                    String udid = map.get("unionid");
                    final String nickname = map.get("nickname");
                    final String headImgUrl = map.get("headimgurl");
                    NetRequestCallBack callback = new NetRequestCallBack() {
                        @Override
                        public void onDefault() {
                            DialogTool.dismissWaitDialog();
                        }

                        @Override
                        public void onSuccess(ArrayMap result) {
                            ToastTool.show("绑定成功");
                            SettingManager.getInstance().setIsWeixinBind(true);
                            sendBroadcast(new Intent(RELOAD_USER_CENTER_ACTION));
                        }

                        @Override
                        public void onFail(ArrayMap result, int error_code) {
                            canHandleSwitth = false;
                            weixinSwitch.setChecked(false);
                        }
                    };
                    DialogTool.showWaitDialog("正在绑定", DialogTool.CANCEL_ON_BACK, callback);
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("udid", CryptTool.encrypt(udid)));
                    params.add(new BasicNameValuePair("nickname", nickname));
                    params.add(new BasicNameValuePair("img_url", headImgUrl));
                    params.add(new BasicNameValuePair("type", "1"));
                    NetManager.getInstance().bindThirdPart(params, callback);
                } else {
                    onError(null, 0, null);
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                canHandleSwitth = false;
                weixinSwitch.setChecked(false);
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
                canHandleSwitth = false;
                weixinSwitch.setChecked(false);
            }
        });
    }

    private void doQQLogin() {
//        if (!WWindowManager.getInstance().isPkgInstalled("com.tencent.mobileqq")) {
//            ThreadManager.getInstance().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    ToastTool.show("还没有安装QQ");
//                    canHandleSwitth = false;
//                    qqSwitch.setChecked(false);
//                }
//            }, 300);
//            return;
//        }
        Platform platform= ShareSDK.getPlatform(QQ.NAME);
        if (platform.isAuthValid()){
            String token = platform.getDb().getToken();
            String userId = platform.getDb().getUserId();
            String name = platform.getDb().getUserName();
            String gender = platform.getDb().getUserGender();
            String headImageUrl = platform.getDb().getUserIcon();
        }
        platform.SSOSetting(false);
        platform.setPlatformActionListener(UserSettingActivity.this);
        platform.showUser(null);

//        UmengTool.getInstance().doThirdPartLogin(this, SHARE_MEDIA.QQ, new UMAuthListener() {
//            @Override
//            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
//                if (map != null && map.containsKey("openid") && map.containsKey("screen_name") && map.containsKey("profile_image_url")) {
//                    String udid = map.get("openid");
//                    final String nickname = map.get("screen_name");
//                    final String headImgUrl = map.get("profile_image_url");
//                    NetRequestCallBack callback = new NetRequestCallBack() {
//                        @Override
//                        public void onDefault() {
//                            DialogTool.dismissWaitDialog();
//                        }
//
//                        @Override
//                        public void onSuccess(ArrayMap result) {
//                            ToastTool.show("绑定成功");
//                            SettingManager.getInstance().setIsQQBind(true);
//                            sendBroadcast(new Intent(RELOAD_USER_CENTER_ACTION));
//                        }
//
//                        @Override
//                        public void onFail(ArrayMap result, int error_code) {
//                            canHandleSwitth = false;
//                            qqSwitch.setChecked(false);
//                        }
//                    };
//                    DialogTool.showWaitDialog("正在绑定", DialogTool.CANCEL_ON_BACK, callback);
//                    List<NameValuePair> params = new ArrayList<>();
//                    params.add(new BasicNameValuePair("udid", CryptTool.encrypt(udid)));
//                    params.add(new BasicNameValuePair("nickname", nickname));
//                    params.add(new BasicNameValuePair("img_url", headImgUrl));
//                    params.add(new BasicNameValuePair("type", "2"));
//                    NetManager.getInstance().bindThirdPart(params, callback);
//                } else {
//                    onError(null, 0, null);
//                }
//            }
//
//            @Override
//            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
//                canHandleSwitth = false;
//                qqSwitch.setChecked(false);
//            }
//
//            @Override
//            public void onCancel(SHARE_MEDIA share_media, int i) {
//                canHandleSwitth = false;
//                qqSwitch.setChecked(false);
//            }
//        });
    }

    private void doWeiboLogin() {
        if (!WWindowManager.getInstance().isPkgInstalled("com.sina.weibo")) {
            ThreadManager.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ToastTool.show("还没有安装微博");
                    canHandleSwitth = false;
                    qqSwitch.setChecked(false);
                }
            }, 300);
            return;
        }
        UmengTool.getInstance().doThirdPartLogin(this, SHARE_MEDIA.SINA, new UMAuthListener() {
            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                if (map != null && map.containsKey("result")) {
                    try {
                        JSONObject object = new JSONObject(map.get("result"));
                        String udid = JsonTool.optString(object, "id");
                        final String nickname = JsonTool.optString(object, "screen_name");
                        final String headImgUrl = JsonTool.optString(object, "profile_image_url");
                        if (TextUtils.isEmpty(udid)) {
                            onError(null, 0, null);
                            return;
                        }
                        NetRequestCallBack callback = new NetRequestCallBack() {
                            @Override
                            public void onDefault() {
                                DialogTool.dismissWaitDialog();
                            }

                            @Override
                            public void onSuccess(ArrayMap result) {
                                ToastTool.show("绑定成功");
                                SettingManager.getInstance().setIsWeiboBind(true);
                                sendBroadcast(new Intent(RELOAD_USER_CENTER_ACTION));
                            }

                            @Override
                            public void onFail(ArrayMap result, int error_code) {
                                canHandleSwitth = false;
                                weiboSwitch.setChecked(false);
                            }
                        };
                        DialogTool.showWaitDialog("正在绑定", DialogTool.CANCEL_ON_BACK, callback);
                        List<NameValuePair> params = new ArrayList<>();
                        params.add(new BasicNameValuePair("udid", CryptTool.encrypt(udid)));
                        params.add(new BasicNameValuePair("nickname", nickname));
                        params.add(new BasicNameValuePair("img_url", headImgUrl));
                        params.add(new BasicNameValuePair("type", "3"));
                        NetManager.getInstance().bindThirdPart(params, callback);
                    } catch (Exception ignore) {
                    }
                } else {
                    onError(null, 0, null);
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                canHandleSwitth = false;
                weiboSwitch.setChecked(false);
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
                canHandleSwitth = false;
                weiboSwitch.setChecked(false);
            }
        });
    }
    @Override
    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        String headImageUrl = null;//头像
        String userId;//userId
        String token;//token
        String gender;//性别
        String name = null;//用户名
        if (i==Platform.ACTION_USER_INFOR){
            if (platform.getName().equals(QQ.NAME)){
                token=platform.getDb().getToken();
                userId=platform.getDb().getUserId();
                name = platform.getDb().getUserName();
                headImageUrl = platform.getDb().getUserIcon();
                NetRequestCallBack callBack=new NetRequestCallBack(){
                    @Override
                    public void onDefault() {
                        DialogTool.dismissWaitDialog();
                    }

                    @Override
                    public void onFail(ArrayMap result, int error_code) {
                        canHandleSwitth=false;
                        qqSwitch.setChecked(false);
                    }

                    @Override
                    public void onSuccess(ArrayMap result) {
                        ToastTool.show("绑定成功");
                        SettingManager.getInstance().setIsQQBind(true);
                        sendBroadcast(new Intent(RELOAD_USER_CENTER_ACTION));
                    }
                };
                DialogTool.showWaitDialog("正在绑定",DialogTool.CANCEL_ON_BACK,callBack);
                List<NameValuePair> params=new ArrayList<>();
                params.add(new BasicNameValuePair("udid",CryptTool.encrypt(userId)));
                params.add(new BasicNameValuePair("nickname",name));
                params.add(new BasicNameValuePair("img_url",headImageUrl));
                params.add(new BasicNameValuePair("type","2"));
                NetManager.getInstance().bindThirdPart(params,callBack);
            }
        }
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {

    }

    @Override
    public void onCancel(Platform platform, int i) {

    }
}
