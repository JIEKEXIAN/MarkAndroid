package com.intlime.mark.tools;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.TextView;

import com.intlime.mark.R;
import com.intlime.mark.application.ThreadManager;
import com.intlime.mark.application.WWindowManager;
import com.intlime.mark.network.NetRequestCallBack;

/**
 * Created by wtu on 2015/04/23 023.
 */
public class DialogTool extends Handler {
    private static DialogTool dialogTool = null;
    private static WaitDialog waitDialog;

    private static final int DISMISS = -111;
    public static final int FINISH_ON_BACK = -112;
    public static final int CANCEL_ON_BACK = -113;

    public DialogTool() {
        super(Looper.getMainLooper());
    }

    public static class WaitDialog extends Dialog {
        public TextView textView;

        public WaitDialog(final Activity context, final int type, final NetRequestCallBack callBack) {
            super(context, R.style.mydialog);
            View view = View.inflate(context, R.layout.dialog_wait_layout, null);
            setContentView(view);
            textView = (TextView) view.findViewById(R.id.text1);
            setCanceledOnTouchOutside(false);
            setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    try {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (type == FINISH_ON_BACK) {
                                abort();
                                dialog.dismiss();
                                context.finish();
                            } else if (type == CANCEL_ON_BACK) {
                                abort();
                                dialog.dismiss();
                            }
                        }
                    }catch (Exception ignore){
                    }
                    return keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK;
                }

                private void abort() {
                    ThreadManager.getInstance().submit(new Runnable() {
                        @Override
                        public void run() {
                            callBack.abortHttpRequest();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void dispatchMessage(Message msg) {
        super.dispatchMessage(msg);
        try {
            if (msg.what == DISMISS) {
                if (waitDialog != null && waitDialog.isShowing()) {
                    waitDialog.dismiss();
                }
                return;
            }
            waitDialog = new WaitDialog(WWindowManager.getInstance().getCurrentActivity(),
                    msg.what, (NetRequestCallBack) ((Object[]) msg.obj)[1]);
            if (msg.obj == null || TextUtils.isEmpty((String) ((Object[]) msg.obj)[0])) {
                waitDialog.textView.setVisibility(View.GONE);
            } else {
                waitDialog.textView.setVisibility(View.VISIBLE);
                waitDialog.textView.setText((String) ((Object[]) msg.obj)[0]);
            }
            waitDialog.show();
        } catch (Exception e) {
        }
    }

    /**
     * 显示等待框
     */
    public static void showWaitDialog(String message) {
        showWaitDialog(message, 0, null);
    }

    public static void showWaitDialog(String message, int type, NetRequestCallBack callBack) {
        if (dialogTool == null) {
            dialogTool = new DialogTool();
        }
        dialogTool.sendMessage(dialogTool.obtainMessage(type, new Object[]{message, callBack}));
    }

    /**
     * 隐藏等待框并取消超时
     */
    public static void dismissWaitDialog() {
        if (dialogTool == null) {
            dialogTool = new DialogTool();
        }
        dialogTool.sendEmptyMessage(DISMISS);
    }

    /**
     * 确认对话框
     */
    public static ConfirmDialog getConfirmDialog(CharSequence confirmText, CharSequence agreeText, CharSequence disagreeText) {
        final ConfirmDialog confirmDialog = new ConfirmDialog(WWindowManager.getInstance().getCurrentActivity());
        if (!TextUtils.isEmpty(confirmText)) {
            confirmDialog.confirm_text.setText(confirmText);
        }
        if (!TextUtils.isEmpty(agreeText)) {
            confirmDialog.confirm_agree.setText(agreeText);
        }
        if (!TextUtils.isEmpty(disagreeText)) {
            confirmDialog.confirm_disagree.setText(disagreeText);
        }
        View.OnClickListener defaultListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        };
        confirmDialog.confirm_agree.setOnClickListener(defaultListener);
        confirmDialog.confirm_disagree.setOnClickListener(defaultListener);
        confirmDialog.setCanceledOnTouchOutside(true);
        confirmDialog.setDisableBackPress(false);
        return confirmDialog;
    }

    public static class ConfirmDialog extends Dialog {
        public TextView confirm_text;
        public TextView confirm_agree;
        public TextView confirm_disagree;
        boolean disableBackPress = false;

        public void setDisableBackPress(boolean disableBackPress) {
            this.disableBackPress = disableBackPress;
        }

        public ConfirmDialog(Context context) {
            super(context, R.style.mydialog);
            View view = View.inflate(context, R.layout.dailog_confirm_layout, null);
            setContentView(view);
            confirm_text = (TextView) view.findViewById(R.id.confirm_text);
            confirm_agree = (TextView) view.findViewById(R.id.confirm_agree);
            confirm_disagree = (TextView) view.findViewById(R.id.confirm_disagree);
            int width = WWindowManager.getInstance().getWidth();
            Window dialogWindow = this.getWindow();
            android.view.WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = (int) (width / 1.2); // 宽度
            dialogWindow.setAttributes(lp);
            setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    //默认返回 false，这里false不能屏蔽返回键，改成true就可以了
                    return disableBackPress;
                }
            });
        }
    }

    public static Dialog showShareDialog(final Activity context, final View.OnClickListener onClickListener) {
        final Dialog dialog = new Dialog(context, R.style.mydialog);
        dialog.setContentView(R.layout.share_dialog_layout);
        final GridLayout gridLayout = (GridLayout) dialog.findViewById(R.id.grid_layout);
        int itemWidth = (WWindowManager.getInstance().getWidth() - DensityUtils.dp2px(context, 30)) / 4;
        View.OnClickListener wrapperListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onClick(v);
                }
                dialog.dismiss();
            }
        };
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).getLayoutParams().width = itemWidth;
            gridLayout.getChildAt(i).setOnClickListener(wrapperListener);
        }
        Window window = dialog.getWindow();
        android.view.WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.dimAmount = 0f;
        lp.windowAnimations = R.style.shareDialogAnim;
        window.setAttributes(lp);
        dialog.show();
        dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    /*public static void showImgWithDrawable(Drawable drawable) {
        final Dialog dialog = new Dialog(WWindowManager.getInstance().getCurrentActivity(), R.style.mydialog);
        ImageView imageView = new ImageView(AppEngine.getContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageDrawable(drawable);
//        new PhotoViewAttacher(imageView).setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
//            @Override
//            public void onViewTap(View view, float x, float y) {
//                dialog.dismiss();
//            }
//        });
        dialog.setContentView(imageView);
        Window dialogWindow = dialog.getWindow();
        android.view.WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        dialogWindow.setBackgroundDrawableResource(R.color.black);
        dialogWindow.setAttributes(lp);
        dialog.show();
    }*/
}