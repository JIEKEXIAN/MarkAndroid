package com.intlime.mark.view

import android.app.Dialog
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.support.v4.util.ArrayMap
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.intlime.mark.R
import com.intlime.mark.activitys.UserSettingActivity
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.network.MyUploadManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.*
import com.qiniu.android.storage.UpCompletionHandler
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableImageView
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Created by wtuadn on 16/04/25.
 */
class HeadImgClipView(val activity: UserSettingActivity, imgPath: String) {
    private lateinit var dialog: Dialog
    private var isCliped = false
    private lateinit var imageView: ImageView
    private lateinit var circleView: View

    init {
        dialog = Dialog(activity, R.style.mydialog)
        dialog.setContentView(applyDialog(imgPath))
        dialog.setCanceledOnTouchOutside(false)
        val dialogWindow = dialog.window
        val lp = dialogWindow.attributes
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT
        lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT
        dialogWindow.setBackgroundDrawableResource(R.color.black)
        dialogWindow.attributes = lp
        dialog.show()
    }

    private fun applyDialog(imgPath: String): View {
        return FrameLayout(activity).apply {
            val lp = FrameLayout.LayoutParams(matchParent, matchParent)
            lp.topMargin = dip(49)
            lp.bottomMargin = dip(43)

            imageView = object : ImageView(AppEngine.getContext()) {
                private val attacher by lazy {
                    val attacher = PhotoViewAttacher(this)
                    attacher.setAllowParentInterceptOnEdge(false)
                    attacher.scaleType = ImageView.ScaleType.FIT_CENTER
                    attacher.mediumScale = 1.5f
                    attacher.minimumScale = 0.8f
                    attacher.setOverEdge(true)
                    return@lazy attacher
                }
                private var tempHeight = 0

                init {
                    val drawable = BitmapDrawable(ImageTool.decodeFile(File(imgPath), false, 480, 854))
                    setImageDrawable(drawable)
                }

                override fun setImageDrawable(drawable: Drawable?) {
                    super.setImageDrawable(drawable)
                    attacher.update()
                }

                override fun onDraw(canvas: Canvas) {
                    if (isCliped) {
                        val size = (measuredWidth * 0.8).toInt()
                        val rect = Rect((measuredWidth - size) / 2, (measuredHeight - size) / 2,
                                (measuredWidth + size) / 2, (measuredHeight + size) / 2)
                        canvas.clipRect(rect)
                    } else if (tempHeight != measuredHeight) {
                        tempHeight = measuredHeight
                        val result = Bitmap.createBitmap(measuredWidth / 2, measuredHeight / 2, Bitmap.Config.ARGB_8888)
                        val canvas2 = Canvas(result)
                        val paint = Paint()
                        paint.isAntiAlias = true
                        paint.color = Color.WHITE
                        canvas2.drawCircle((measuredWidth / 4).toFloat(), (measuredHeight / 4).toFloat(), measuredWidth * 0.2f, paint)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
                        paint.color = Color.parseColor("#75000000")
                        canvas2.drawRect(Rect(0, 0, measuredWidth / 2, measuredHeight / 2), paint)
                        paint.color = Color.WHITE
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = DensityUtils.dp2px(AppEngine.getContext(), 1f) / 2f
                        paint.xfermode = null
                        canvas2.drawCircle((measuredWidth / 4).toFloat(), (measuredHeight / 4).toFloat(), measuredWidth * 0.2f, paint)
                        circleView.backgroundDrawable = BitmapDrawable(result)
                    }
                    super.onDraw(canvas)
                }
            }
            addView(imageView, lp)

            circleView = view {
                layoutParams = lp
            }

            relativeLayout {
                lparams(matchParent, dip(49))
                pressableImageView {
                    lparams(dip(49), matchParent)
                    imageResource = R.drawable.back_icon_white
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.white_pressed_color), true, 0, -1f)
                    onClick {
                        dialog.dismiss()
                    }
                }
                textView {
                    text = "移动和缩放"
                    textSize = 18f
                    textColor = Color.WHITE
                }.lparams {
                    centerInParent()
                }
            }
            frameLayout {
                lparams(matchParent, dip(43)) {
                    gravity = Gravity.BOTTOM
                }
                backgroundColor = Color.parseColor("#d8d8d8")
                pressableTextView {
                    text = "确定"
                    textSize = 15f
                    textColor = Color.WHITE
                    gravity = Gravity.CENTER
                    val bg = GradientDrawable()
                    bg.setColor(resources.getColor(R.color.dark_blue))
                    bg.setCornerRadius(dip(4).toFloat())
                    backgroundDrawable = bg
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    onClick {
                        isCliped = true
                        var bitmap = ImageTool.getMagicDrawingCache(imageView)
                        isCliped = false
                        val size = (imageView.measuredWidth * 0.8).toInt()
                        bitmap = Bitmap.createBitmap(bitmap, (imageView.measuredWidth * 0.1).toInt(),
                                imageView.measuredHeight / 2 - size / 2, size, size, null, true)
                        DialogTool.showWaitDialog("修改头像中")
                        upload(bitmap, false)
                        //                        DialogTool.showImgWithDrawable(BitmapDrawable(bitmap));
                    }
                }.lparams(dip(72.5f), dip(30.5f)) {
                    rightMargin = dip(10)
                    gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                }
            }
        }
    }

    private fun upload(bitmap: Bitmap, isRetry: Boolean) {
        val key = "portrait_" + System.nanoTime()
        val bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bs)
        MyUploadManager.getInstance().put(bs.toByteArray(), key, MyUploadManager.getToken(),
                UpCompletionHandler { key, info, response ->
                    if (info != null) {
                        val error = info.error
                        LogTool.d("qiniu_info", info.toString())
                        LogTool.d("qiniu_info", "error " + error)
                        if (isRetry) {
                            if (info.toString().contains("expired token") || info.toString().contains("no token")) {
                                //token失效，重新获取
                                getTokenAgain(bitmap)
                                return@UpCompletionHandler
                            }
                        } else {
                            DialogTool.dismissWaitDialog()
                        }
                    }
                    if (response != null) {
                        LogTool.d("qiniu_response", response.toString())

                        NetManager.getInstance().changeHeadImg(key, object : NetRequestCallBack() {
                            override fun onDefault() {
                                DialogTool.dismissWaitDialog()
                            }

                            override fun onSuccess(result: ArrayMap<Any, Any>) {
                                var url: String = ""
                                if (result.containsKey("img_url"))
                                    url = result["img_url"] as String
                                SettingManager.getInstance().userHeadImgUrl = url
                                ThreadManager.getInstance().post {
                                    dialog.dismiss()
                                    activity.onChangeSuccess(url)
                                }
                                ToastTool.show("修改成功")
                            }
                        })
                    } else {
                        ToastTool.show("修改失败，请稍后再试")
                        DialogTool.dismissWaitDialog()
                    }
                }, null)
    }

    private fun getTokenAgain(bitmap: Bitmap) {
        NetManager.getInstance().getFileToken(object : NetRequestCallBack() {
            override fun onSuccess(result: ArrayMap<Any, Any>) {
                MyUploadManager.setToken(result["upload_token"] as String)
                upload(bitmap, true)
            }

            override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                ToastTool.show("修改失败，请稍后再试")
            }
        })
    }
}