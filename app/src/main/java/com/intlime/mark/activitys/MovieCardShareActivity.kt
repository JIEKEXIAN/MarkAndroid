package com.intlime.mark.activitys

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.util.ArrayMap
import android.support.v7.widget.Toolbar
import android.text.*
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.network.MyUploadManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.*
import com.intlime.mark.view.drawable.EmptyDrawable
import com.qiniu.android.storage.UpCompletionHandler
import com.tencent.mm.sdk.modelmsg.SendMessageToWX
import com.tencent.mm.sdk.modelmsg.WXImageObject
import com.tencent.mm.sdk.modelmsg.WXMediaMessage
import com.tencent.mm.sdk.openapi.WXAPIFactory
import com.umeng.socialize.ShareAction
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA
import com.umeng.socialize.media.UMImage
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by wtuadn on 16/04/14.
 */
class MovieCardShareActivity : BaseActivity(), View.OnClickListener {
    companion object {
        val WORD = "movie_card_word"
        val PHOTO = "movie_card_photo"
        val PHOTO_LIST = "movie_card_photo_list"
        val PHOTO_POSITION = "movie_card_photo_position"
    }

    private lateinit var imgView: MyImgView
    private lateinit var contentText: EditText
    private lateinit var titleText: TextView
    private var bean: MovieBean? = null
    private var photoList: ArrayList<Array<String>> = ArrayList()
    private var wordList: ArrayList<String>? = null
    private var cPhoto = ""
    private var cWord = ""
    private var lastWord = "_"
    private var isPhotoChanged = true;
    private var tempPath: String = ""
    private var tempFile: File? = null
    private var requstSavePic = false

    private val api by lazy {
        val foo = WXAPIFactory.createWXAPI(this, UmengTool.WXAPPID, true)
        foo.registerApp(UmengTool.WXAPPID)
        return@lazy foo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bean = intent.getParcelableExtra(BEAN)
        if (bean == null) {
            finish()
            return
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("1")) {
                val foo1 = savedInstanceState.get("1") as? ArrayList<Array<String>>
                wordList = savedInstanceState.get("2") as? ArrayList<String>
                val foo3 = savedInstanceState.get("3") as? String
                val foo4 = savedInstanceState.get("4") as? String
                if (foo1 != null) {
                    photoList.addAll(foo1)
                }
                if (foo3 != null) {
                    cPhoto = foo3
                }
                if (foo4 != null) {
                    cWord = foo4
                }
            }
        }
        applyUI()
        applyData()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable("1", photoList)
        outState?.putStringArrayList("2", wordList)
        outState?.putString("3", cPhoto)
        outState?.putString("4", cWord)
    }

    override fun onResume() {
        super.onResume()
        if (Session.map.containsKey(WORD)) {
            cWord = Session.map.remove(WORD) as String
            contentText.setText(cWord)
        }
        if (Session.map.containsKey(PHOTO)) {
            cPhoto = Session.map.remove(PHOTO) as String
            loadImg()
        }
        if (Session.map.containsKey(PHOTO_LIST)) {
            photoList.clear()
            photoList.addAll(Session.map.remove(PHOTO_LIST) as ArrayList<Array<String>>)
        }
        contentText.setSelection(contentText.length())
    }

    private fun loadImg() {
        if (TextUtils.isEmpty(cPhoto)) return
        val placeholder = ColorDrawable(Color.parseColor("#e1e1e1"))
        val error = EmptyDrawable(68f, 68f)
        Glide.with(imgView.context)
                .load(cPhoto)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .placeholder(placeholder)
                .error(error)
                .into(imgView)
    }

    private fun showWordEditHint() {
        if (SettingManager.getInstance().hasToShowMovieWordEdtiHint()) {
            SettingManager.getInstance().setHasToShowMovieWordEdtiHint(false)
            contentText.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    contentText.viewTreeObserver.removeOnPreDrawListener(this)
                    val position = intArrayOf(0, 0)
                    contentText.getLocationOnScreen(position)
                    val dialog = Dialog(this@MovieCardShareActivity, R.style.mydialog)
                    val view = TextView(this@MovieCardShareActivity).apply {
                        text = "点这里编辑台词"
                        textSize = 14f
                        textColor = Color.WHITE
                        gravity = Gravity.CENTER
                        backgroundResource = R.drawable.movie_word_hint_bg
                        setPadding(dip(16), dip(19.7f), dip(16), dip(13.7f))
                        onClick {
                            dialog.dismiss()
                        }
                    }
                    dialog.setContentView(view)
                    val window = dialog.window
                    val lp = window.attributes
                    lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    lp.y = position[1] + contentText.measuredHeight - WWindowManager.getInstance().statusBarHeight
                    lp.dimAmount = 0f
                    window.attributes = lp
                    dialog.show()
                    ThreadManager.getInstance().postDelayed({
                        try {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        } catch (ignore: Exception) {
                        }
                    }, 3000)
                    return true
                }
            })
        }
    }

    private fun applyData() {
        if (wordList != null) {
            loadImg()
            contentText.setText(cWord)
            return
        }
        if (bean!!.stagePhoto != null && bean!!.stagePhoto!!.isNotEmpty()) {
            cPhoto = bean!!.stagePhoto!![0]
        } else {
            cPhoto = bean!!.image ?: ""
        }
        loadImg()
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                wordList = result["list"]  as ArrayList<String>
                if (wordList!!.isNotEmpty()) {
                    cWord = wordList!![0]
                    runOnUiThread {
                        contentText.setText(cWord)
                        contentText.setSelection(contentText.length())
                        showWordEditHint()
                    }
                } else {
                    runOnUiThread {
                        showWordEditHint()
                    }
                }
            }
        }
        DialogTool.showWaitDialog("请稍等", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getMovieLines(bean!!.id, callback)
    }

    private fun applyUI() {
        rootView = relativeLayout {
            val top_line = 1
            val center_line = 2
            backgroundColor = resources.getColor(R.color.bg)
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "电影卡片制作"
                menu.add("卡片投稿").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    startActivity<WebActivity>("title" to "卡片投稿",  "url" to "http://form.mikecrm.com/6BqhGv")
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            scrollView {
                horizontalPadding = dip(6)
                isFillViewport = true
                verticalLayout {
                    topPadding = dip(6)
                    verticalLayout {
                        backgroundResource = R.drawable.share_img_bg
                        frameLayout {
                            val height = ((WWindowManager.getInstance().width - dip(12)) * 0.7).toInt()
                            imgView = MyImgView(context).lparams(matchParent, height) {
                                topMargin = dip(6)
                                horizontalMargin = dip(6)
                            }
                            addView(imgView)
                            imageView {
                                imageResource = R.drawable.movie_card_choose_img
                            }.lparams {
                                rightMargin = dip(17.5f)
                                bottomMargin = dip(8.5f)
                                gravity = Gravity.RIGHT or Gravity.BOTTOM
                            }
                        }
                        contentText = editText {
                            backgroundColor = Color.TRANSPARENT
                            setPadding(0, 0, 0, 0)
                            hint = "点击输入电影台词或者你想说的话"
                            hintTextColor = Color.parseColor("#9ea7ab")
                            textSize = 14f
                            textColor = resources.getColor(R.color.a_main_text_color)
                            setLineSpacing(0f, 1.2f);
                            addTextChangedListener(object : TextWatcher {
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                }

                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                                }

                                override fun afterTextChanged(s: Editable?) {
                                    if (s != null && !s.toString().equals(cWord)) {
                                        s.replace(0, s.length, cWord)
                                    }
                                }
                            })
                            setOnTouchListener { view, motionEvent ->
                                if (motionEvent.action == MotionEvent.ACTION_UP) {
                                    wordList ?: return@setOnTouchListener true
                                    startActivity<MovieWordActivity>("list" to wordList!!, "word" to contentText.text.toString())
                                }
                                return@setOnTouchListener true
                            }
                            try {
                                val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                                f.isAccessible = true
                                f.set(this, R.drawable.edittext_cursor)
                            } catch (ignored: Exception) {
                            }
                        }.lparams(matchParent, wrapContent) {
                            topMargin = dip(17)
                            horizontalMargin = dip(18.5f)
                        }
                        titleText = textView { //电影名
                            text = "━━《 ${bean?.name} 》"
                            textSize = 14f
                            textColor = resources.getColor(R.color.a_main_text_color)
                            gravity = Gravity.RIGHT
                        }.lparams(matchParent, wrapContent) {
                            topMargin = dip(40)
                            horizontalMargin = dip(18.5f)
                        }
                        textView {
                            text = "来自Mark－我的电影清单"
                            textSize = 11f
                            textColor = Color.parseColor("#818c91")
                            gravity = Gravity.CENTER_VERTICAL
                            compoundDrawablePadding = dip(3.2f)
                            setCompoundDrawablesWithIntrinsicBounds(R.drawable.movie_card_logo, 0, 0, 0)
                        }.lparams(matchParent, wrapContent) {
                            topMargin = dip(30)
                            bottomMargin = dip(15)
                            horizontalMargin = dip(18)
                        }
                    }
                    textView {
                        text = "Tips：你可以通过点击图片或文字进入编辑状态"
                        textSize = 13f
                        textColor = Color.parseColor("#9ea7ab")
                        gravity = Gravity.CENTER
                    }.lparams(matchParent, wrapContent) {
                        topMargin = dip(16)
                        bottomMargin = dip(30)
                        horizontalMargin = dip(13)
                    }
                }
            }.lparams(matchParent, matchParent) {
                below(R.id.toolbar)
                above(top_line)
            }
            view {
                id = top_line
                backgroundColor = Color.parseColor("#e1e1e1")
                lparams(matchParent, dip(1)) {
                    alignParentBottom()
                    bottomMargin = dip(45)
                }
            }
            view {
                id = center_line
                backgroundColor = Color.parseColor("#e1e1e1")
                lparams(dip(1), dip(25)) {
                    alignParentBottom()
                    centerHorizontally()
                    bottomMargin = dip(10)
                }
            }
            pressableRelativeLayout {
                lparams(matchParent, dip(45)) {
                    alignParentBottom()
                    leftOf(center_line)
                }
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                textView {
                    text = "分享"
                    textSize = 14f
                    textColor = Color.parseColor("#496069")
                    gravity = Gravity.CENTER_VERTICAL
                    compoundDrawablePadding = dip(7)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.multi_share_icon, 0, 0, 0)
                }.lparams(wrapContent, wrapContent) {
                    centerInParent()
                }
                onClick {
                    if (lastWord.equals(cWord) && !isPhotoChanged && tempFile != null) {
                        openShareDialog()
                    } else {
                        requstSavePic = false
                        if (UmengTool.checkPermission(this@MovieCardShareActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                            doDrawCard(false)
                        }
                    }
                    ZhugeTool.track("点击电影分享", ZhugeTool.getTrackArg(Pair("操作", "卡片")))
                }
            }
            pressableRelativeLayout {
                lparams(matchParent, dip(45)) {
                    alignParentBottom()
                    rightOf(center_line)
                }
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                textView {
                    text = "保存"
                    textSize = 14f
                    textColor = Color.parseColor("#496069")
                    gravity = Gravity.CENTER_VERTICAL
                    compoundDrawablePadding = dip(7)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.movie_card_save_icon, 0, 0, 0)
                }.lparams(wrapContent, wrapContent) {
                    centerInParent()
                }
                onClick {
                    if (lastWord.equals(cWord) && !isPhotoChanged && tempFile != null) {
                        savePic()
                    } else {
                        requstSavePic = true
                        if (UmengTool.checkPermission(this@MovieCardShareActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                            doDrawCard(true)
                        }
                    }
                }
            }
        }
    }

    private fun doDrawCard(savePic: Boolean) {
        ThreadManager.getInstance().submit {
            DialogTool.showWaitDialog("图片生成中")
            drawCard(!savePic)
            if (savePic) savePic()
            DialogTool.dismissWaitDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDrawCard(requstSavePic)
            } else {
                ToastTool.show("获取权限失败")
            }
        }
    }

    private fun drawCard(openDialog: Boolean = true) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            tempPath = externalCacheDir.absolutePath
            val dir = File(tempPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } else {
            ToastTool.show("存储空间不足", Toast.LENGTH_LONG, 0)
            return
        }
        val paint = Paint()
        paint.isDither = true
        paint.isFilterBitmap = true
        paint.isAntiAlias = true
        val textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.color = Color.parseColor("#10181e")

        val width = 850f
        var height: Float
        textPaint.textSize = 33.85f
        val contentLayout = StaticLayout(contentText.text,
                textPaint, (width - 43.524).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, true)
        val titleLayout = StaticLayout(titleText.text,
                textPaint, 695, Layout.Alignment.ALIGN_OPPOSITE, 1.2f, 0f, true)
        val contentHeight = if (contentText.text.isNullOrEmpty()) -50 else contentLayout.height

        val matrix = imgView.imageMatrix
        matrix.postScale(width / imgView.width, width / imgView.width)
        val bitmap = Bitmap.createBitmap(width.toInt(), (width / imgView.width * imgView.height).toInt(), Bitmap.Config.RGB_565)
        val c = Canvas(bitmap)
        c.drawBitmap((imgView.drawable as BitmapDrawable).bitmap, matrix, paint)

        val imgHeight = bitmap.height
        height = imgHeight + 54f + contentHeight + 83.018f + titleLayout.height + 131.781f

        val result = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val m = Matrix()
        m.setScale(width / bitmap.width, width / bitmap.width)
        canvas.drawBitmap(bitmap, m, paint)

        canvas.translate(27f, imgHeight + 54f)
        contentLayout.draw(canvas)
        canvas.translate(104f, contentHeight + 83.018f)
        titleLayout.draw(canvas)
        canvas.translate(-104f, titleLayout.height + 75f)
        val o = BitmapFactory.Options()
        o.inScaled = false
        o.inDensity = displayMetrics.densityDpi
        o.inScreenDensity = displayMetrics.densityDpi
        o.inTargetDensity = displayMetrics.densityDpi
        canvas.drawBitmap(ImageTool.getBitmap(R.drawable.movie_card_logo, o), 0f, 0f, paint)
        canvas.translate(46f, 24f)
        textPaint.color = Color.parseColor("#818c91")
        textPaint.textSize = 24.18f
        canvas.drawText("来自Mark-我的电影清单", 0f, 0f, textPaint)
        val isFirstDraw = tempFile == null
        try {
            lastWord = cWord;
            isPhotoChanged = false
            val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
            val curDate = Date(System.currentTimeMillis())//获取当前时间
            val fileName = formatter.format(curDate)//用日期作文件名
            tempFile = File("$tempPath/$fileName.png")
            val fOut = FileOutputStream(tempFile)
            result.compress(Bitmap.CompressFormat.PNG, 80, fOut)
            fOut.flush()
            fOut.close()
            result.recycle()
            System.gc()
        } catch(e: Exception) {
            ToastTool.show("存储空间不足", Toast.LENGTH_LONG, 0)
            return
        }
        if (openDialog) {
            runOnUiThread {
                openShareDialog()
                //                DialogTool.showImgWithDrawable(BitmapDrawable(result))
            }
        }
        if (isFirstDraw && bean != null && !bean!!.db_num.isNullOrEmpty()) {
            if (TextUtils.isEmpty(MyUploadManager.getToken())) {
                getTokenAndUpload(bitmap, bean!!.db_num!!, bean!!.name!!, contentText.text.toString())
            } else {
                upload(bitmap, bean!!.db_num!!, bean!!.name!!, contentText.text.toString())
            }
        }
    }

    private fun getTokenAndUpload(bitmap: Bitmap, db_num: String, name: String, content: String) {
        NetManager.getInstance().getFileToken(object : NetRequestCallBack() {
            override fun onSuccess(result: ArrayMap<Any, Any>) {
                MyUploadManager.setToken(result["upload_token"] as String)
                upload(bitmap, db_num, name, content)
            }
        })
    }

    private fun upload(bitmap: Bitmap, db_num: String, name: String, content: String) {
        val key = "usersharemovie_" + System.nanoTime()
        val bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bs)
        MyUploadManager.getInstance().put(bs.toByteArray(), key, MyUploadManager.getToken(),
                UpCompletionHandler { key, info, response ->
                    if (info != null) {
                        val error = info.error
                        LogTool.d("qiniu_info", info.toString())
                        LogTool.d("qiniu_info", "error " + error)
                        if (info.toString().contains("expired token") || info.toString().contains("no token")) {
                            //token失效，重新获取
                            getTokenAndUpload(bitmap, db_num, name, content)
                            return@UpCompletionHandler
                        }
                    }
                    if (response != null) {
                        LogTool.d("qiniu_response", response.toString())
                        val params = arrayListOf<NameValuePair>(BasicNameValuePair("db_num", db_num),
                                BasicNameValuePair("img_name", CryptTool.encrypt(key)),
                                BasicNameValuePair("name", name),
                                BasicNameValuePair("content", content))
                        NetManager.getInstance().movieCardShare(params, object : NetRequestCallBack() {})
                    }
                }, null)
    }

    private fun openShareDialog() {
        val dialog = DialogTool.showShareDialog(this, this)
        val save_pic = dialog.findViewById(R.id.copy_link) as TextView
        save_pic.id = R.id.save_pic
        save_pic.text = "保存图片"
        save_pic.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.save_pic_icon, 0, 0)
        val qzone = dialog.findViewById(R.id.qq_zone)
        (qzone.parent as ViewGroup).removeView(qzone)
        dialog.findViewById(R.id.weibo).layoutParams = qzone.layoutParams
    }

    override fun onStop() {
        super.onStop()
        if (tempFile != null) DialogTool.dismissWaitDialog()
    }

    override fun onStart() {
        super.onStart()
        if (tempFile != null) DialogTool.dismissWaitDialog()
    }

    override fun onClick(v: View?) {
        try {
            UmengTool.getInstance()
            val umImage = UMImage(this, tempFile)
            when (v?.id) {
                R.id.weixin -> {
                    DialogTool.showWaitDialog("请稍等")
                    ThreadManager.getInstance().submit {
                        try {
                            val imgOb = WXImageObject()
                            imgOb.imagePath = tempFile!!.absolutePath
                            val msg = WXMediaMessage(imgOb)
                            setThumb(msg)
                            val req = SendMessageToWX.Req()
                            req.message = msg
                            req.scene = SendMessageToWX.Req.WXSceneSession
                            api.sendReq(req)
                        } catch(e: Exception) {
                            e.printStackTrace()
                            DialogTool.dismissWaitDialog()
                        }
                    }
                }
                R.id.weixin_circle -> {
                    DialogTool.showWaitDialog("请稍等")
                    ThreadManager.getInstance().submit {
                        try {
                            val imgOb = WXImageObject()
                            imgOb.imagePath = tempFile!!.absolutePath
                            val msg = WXMediaMessage(imgOb)
                            val req = SendMessageToWX.Req()
                            req.message = msg
                            req.scene = SendMessageToWX.Req.WXSceneTimeline
                            api.sendReq(req)
                        } catch(e: Exception) {
                            e.printStackTrace()
                            DialogTool.dismissWaitDialog()
                        }
                    }
                }
                R.id.qq -> {
                    ShareAction(this)
                            .setPlatform(SHARE_MEDIA.QQ)
                            .withMedia(umImage)
                            .share()
                }
                R.id.weibo -> {
                    ShareAction(this)
                            .setPlatform(SHARE_MEDIA.SINA)
                            .withMedia(umImage)
                            .withText(" ")
                            .share()
                }
                R.id.save_pic -> {
                    savePic()
                }
                R.id.more -> {
                    val intent = Intent()
                    intent.action = Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile))
                    intent.type = "image/*"
                    startActivity(Intent.createChooser(intent, null))
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
            ToastTool.show("分享失败")
            DialogTool.dismissWaitDialog()
        }
    }

    private fun savePic() {
        try {
            val path = Environment.getExternalStorageDirectory().absolutePath + "/" + Environment.DIRECTORY_DCIM + "/Mark"
            var dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val savedFile = File(path, tempFile!!.name)
            ImageTool.copyFile(tempFile, savedFile)
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(savedFile)))
            ToastTool.show("已成功保存到" + savedFile.absolutePath, 0, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastTool.show("存储空间不足", Toast.LENGTH_LONG, 0)
        }
    }

    private fun setThumb(msg: WXMediaMessage) {
        var inSampleSize = 2
        val maxLength = 32768
        val o = BitmapFactory.Options()
        o.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bs = ByteArrayOutputStream()
        loop@ for (i in 0..2) {
            o.inSampleSize = inSampleSize
            val thumb = ImageTool.getBitmapFromFile(tempFile!!.absolutePath, o)
            var quality = 71;
            for (j in 0..8) {
                thumb.compress(Bitmap.CompressFormat.JPEG, quality, bs)
                msg.thumbData = bs.toByteArray()
                if (msg.thumbData.size <= maxLength) {
                    break@loop
                }
                quality -= 10
                bs.reset()
                if (quality <= 0) {
                    break
                }
            }
            inSampleSize *= 2
            thumb.recycle()
        }
        bs.close()
        if (msg.thumbData.size > maxLength) {
            msg.thumbData = null
        }
        System.gc()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (UmengTool.isInited())
            UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!TextUtils.isEmpty(tempPath)) {
            FileTool.cleanDirectory(File(tempPath))
        }
        Session.map.clear()
    }

    private inner class MyImgView(context: Context) : ImageView(context) {
        private val path by lazy {
            ImageTool.getRoundedRectPath(left.toFloat() - dip(6), top.toFloat() - dip(6),
                    right.toFloat() - dip(6), bottom.toFloat() - dip(6), dip(6).toFloat(), dip(6).toFloat(), true)
        }
        private lateinit var mAttacher: PhotoViewAttacher
        private var isCliped = true

        init {
            mAttacher = PhotoViewAttacher(this)
            mAttacher.scaleType = ImageView.ScaleType.CENTER_CROP
            mAttacher.onViewTapListener = PhotoViewAttacher.OnViewTapListener { view, x, y ->
                if (bean != null) {
                    startActivity<MoviePhotoActivity>("list" to photoList, "id" to bean!!.id)
                }
            }
            onClick {
                if (bean != null) {
                    startActivity<MoviePhotoActivity>("list" to photoList, "id" to bean!!.id)
                }
            }
            mAttacher.setOnMatrixChangeListener {
                isPhotoChanged = true
            }
        }

        override fun dispatchDraw(canvas: Canvas?) {
            parent.requestDisallowInterceptTouchEvent(true)
            super.dispatchDraw(canvas)
        }

        override fun setImageDrawable(drawable: Drawable?) {
            super.setImageDrawable(drawable)
            mAttacher.update()
        }

        override fun onDraw(canvas: Canvas?) {
            if (isCliped) canvas?.clipPath(path)
            super.onDraw(canvas)
        }
    }
}