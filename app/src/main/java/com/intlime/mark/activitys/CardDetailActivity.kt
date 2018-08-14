package com.intlime.mark.activitys

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.util.ArrayMap
import android.support.v7.widget.Toolbar
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.application.Session
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieBean
import com.intlime.mark.bean.MovieCardBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.*
import com.intlime.mark.tools.db.MovieDbManager
import com.tencent.mm.sdk.modelmsg.SendMessageToWX
import com.tencent.mm.sdk.modelmsg.WXImageObject
import com.tencent.mm.sdk.modelmsg.WXMediaMessage
import com.tencent.mm.sdk.openapi.WXAPIFactory
import com.umeng.socialize.ShareAction
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA
import com.umeng.socialize.media.UMImage
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by wtuadn on 16/05/03.
 */
class CardDetailActivity : BaseActivity(), View.OnClickListener {
    private var bean: MovieCardBean? = null
    private lateinit var root: ViewGroup
    private lateinit var likes: TextView
    private lateinit var shares: TextView
    private var tempPath: String = ""
    private var tempFile: File? = null

    private val api by lazy {
        val foo = WXAPIFactory.createWXAPI(this, UmengTool.WXAPPID, true)
        foo.registerApp(UmengTool.WXAPPID)
        return@lazy foo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bean = intent.getParcelableExtra<MovieCardBean>(BEAN)
        if (bean == null) {
            finish()
            return
        }
        applyUI()
        applyData()
    }

    private fun applyUI() {
        root = relativeLayout {
            backgroundColor = resources.getColor(R.color.bg)
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "卡片详情"
            }.lparams(matchParent, dip(49))
            relativeLayout {
                id = 1
                pressableTextView {
                    text = "查看电影"
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    gravity = Gravity.CENTER_VERTICAL
                    leftPadding = dip(17.5f)
                    compoundDrawablePadding = dip(10)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.daily_card_detail_icon, 0, 0, 0)
                    backgroundDrawable = GradientDrawable().apply {
                        setColor(Color.parseColor("#21818c91"))
                        setCornerRadius(dip(18).toFloat())
                        setStroke(1, Color.parseColor("#d2d2d2"))
                    }
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    onClick {
                        val dailyBean = bean
                        dailyBean ?: return@onClick
                        val movieBean = MovieDbManager.getInstance().get(dailyBean.db_num)
                        if (movieBean == null) {
                            startActivity<MovieDetailActivity>("type" to 1, BEAN to MovieBean(name = dailyBean.name, db_num = dailyBean.db_num))
                        } else {
                            startActivity<MovieDetailActivity>(BEAN to movieBean)
                        }
                    }
                }.lparams(dip(113.5f), dip(36f)) {
                    leftMargin = dip(22f)
                }
                likes = pressableTextView {
                    textSize = 13f
                    textColor = Color.parseColor("#818c91")
                    gravity = Gravity.CENTER_HORIZONTAL
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_unchecked, 0, 0)
                    onClick {
                        val bean = bean
                        bean ?: return@onClick
                        if (bean.liked == 1) {
                            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_unchecked, 0, 0)
                            text = "${bean.likes - 1}"
                            bean.liked = 0
                            bean.likes -= 1
                        } else {
                            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_checked, 0, 0)
                            text = "${bean.likes + 1}"
                            bean.liked = 1
                            bean.likes += 1
                        }
                        NetManager.getInstance().likeMovieCard(bean.id, 2 - bean.liked, object : NetRequestCallBack() {
                            override fun onSuccess(result: ArrayMap<*, *>?) {
//                                val intent = Intent(SYNC_LIKED_CARDS_ACTION)
//                                intent.putExtra(BEAN, bean)
//                                sendBroadcast(intent)
                            }

                            override fun onFail(result: ArrayMap<*, *>?, error_code: Int) {
                                ThreadManager.getInstance().post {
                                    if (bean.liked == 1) {
                                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_unchecked, 0, 0)
                                        text = "${bean.likes - 1}"
                                        bean.liked = 0;
                                        bean.likes -= 1
                                    } else {
                                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_checked, 0, 0)
                                        text = "${bean.likes + 1}"
                                        bean.liked = 1;
                                        bean.likes += 1
                                    }
                                }
                            }
                        })
                    }
                }.lparams {
                    alignParentRight()
                    rightMargin = dip(100)
                    topMargin = dip(1)
                }
                shares = pressableTextView {
                    textSize = 13f
                    textColor = Color.parseColor("#818c91")
                    gravity = Gravity.CENTER_HORIZONTAL
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_share_icon, 0, 0)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    onClick {
                        if (UmengTool.checkPermission(this@CardDetailActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                            doDrawCard()
                        }
                    }
                }.lparams {
                    alignParentRight()
                    rightMargin = dip(28.5f)
                }
            }.lparams(matchParent, wrapContent) {
                alignParentBottom()
                bottomMargin = dip(15)
            }
            relativeLayout {
                lparams(matchParent, matchParent) {
                    below(R.id.toolbar)
                    above(1)
                    horizontalMargin = dip(6)
                    topMargin = dip(6)
                    bottomMargin = dip(30)
                }
                backgroundResource = R.drawable.share_img_bg
                val imgView = MyImgView(context).lparams(matchParent, ((WWindowManager.getInstance().width - dip(12)) * 0.7).toInt()) {
                    topMargin = dip(6)
                    horizontalMargin = dip(6)
                }
                imgView.id = 11
                addView(imgView)
                textView { //台词
                    id = 12
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    setLineSpacing(0f, 1.2f)
                }.lparams(matchParent, wrapContent) {
                    below(11)
                    topMargin = dip(17)
                    horizontalMargin = dip(22)
                }
                textView { //电影名
                    id = 13
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    gravity = Gravity.RIGHT
                }.lparams(matchParent, wrapContent) {
                    below(12)
                    topMargin = dip(26)
                    horizontalMargin = dip(22)
                }
            }
        }
    }

    private fun applyData() {
        val callback = object : NetRequestCallBack() {
            override fun onDefault() {
                DialogTool.dismissWaitDialog()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                bean = result["bean"] as? MovieCardBean
                if(bean == null){
                    ToastTool.show("加载失败")
                    finish()
                    return
                }
                ThreadManager.getInstance().post {
                    setCard(bean!!)
                }
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getMovieCardDetail(bean!!.id, callback)
    }

    private fun setCard(bean: MovieCardBean) {
        val card = root.getChildAt(root.childCount - 1) as ViewGroup
        val imageView = card.getChildAt(0) as ImageView
        val content = card.getChildAt(1) as TextView
        val name = card.getChildAt(2) as TextView
        Glide.with(this)
                .load(bean.imgUrl)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(imageView)
        content.maxLines = Int.MAX_VALUE
        content.textSize = 14f
        name.textSize = 14f
        content.text = bean.content
        name.text = "━━《 ${bean.name} 》"
        (content.layoutParams as ViewGroup.MarginLayoutParams).topMargin = dip(17)
        (name.layoutParams as ViewGroup.MarginLayoutParams).topMargin = dip(26)
        resizeText(card, content, name)
        updateLikesAndShares(bean)
    }

    private fun resizeText(card: ViewGroup, content: TextView, name: TextView) {
        var isCut = false
        name.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (name.bottom > card.measuredHeight - dip(10)) {
                    if (content.textSize < sp(9)) {
                        if (isCut) {
                            name.viewTreeObserver.removeOnPreDrawListener(this)
                            return true
                        } else {
                            isCut = true
                            content.maxLines = 7
                            content.textSize = 13f
                            name.textSize = 13f
                            (content.layoutParams as ViewGroup.MarginLayoutParams).topMargin = dip(16)
                            (name.layoutParams as ViewGroup.MarginLayoutParams).topMargin = dip(24.5f)
                            return false
                        }
                    }
                    content.setTextSize(TypedValue.COMPLEX_UNIT_PX, content.textSize - sp(1))
                    name.setTextSize(TypedValue.COMPLEX_UNIT_PX, content.textSize - sp(1))
                    (content.layoutParams as ViewGroup.MarginLayoutParams).topMargin -= dip(1)
                    (name.layoutParams as ViewGroup.MarginLayoutParams).topMargin -= dip(1.5f)
                } else {
                    name.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
                return false
            }
        })
    }

    private fun updateLikesAndShares(bean: MovieCardBean) {
        shares.text = bean.shares.toString()
        likes.text = bean.likes.toString()
        if (bean.liked == 1) {
            likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_checked, 0, 0)
        } else {
            likes.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.daily_card_like_unchecked, 0, 0)
        }
    }

    private fun doDrawCard() {
        ThreadManager.getInstance().submit {
            DialogTool.showWaitDialog("图片生成中")
            drawCard(bean!!)
            DialogTool.dismissWaitDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDrawCard()
            } else {
                ToastTool.show("获取权限失败")
            }
        }
    }

    private fun drawCard(bean: MovieCardBean) {
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
        val contentLayout = StaticLayout(bean.content,
                textPaint, (width - 43.524).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, true)
        val titleLayout = StaticLayout("━━《 ${bean.name} 》",
                textPaint, 695, Layout.Alignment.ALIGN_OPPOSITE, 1.2f, 0f, true)
        val contentHeight = if (bean.content.isNullOrEmpty()) -50 else contentLayout.height

        val imgView = ((root.getChildAt(root.childCount - 1) as ViewGroup).getChildAt(0)) as ImageView
        val bitmap = Glide.with(this)
                .load(bean.imgUrl)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(width.toInt(), (width / imgView.width * imgView.height).toInt())
                .get()

        val imgHeight = bitmap.height
        height = imgHeight + 54f + contentHeight + 83.018f + titleLayout.height + 131.781f

        val result = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

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
        try {
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
        runOnUiThread {
            openShareDialog()
            //                        DialogTool.showImgWithDrawable(BitmapDrawable(result))
        }
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

    override fun onClick(v: View?) {
        try {
            UmengTool.getInstance()
            val umImage = UMImage(this, tempFile)
            when (v?.id) {
                R.id.weixin -> {
                    if (!WWindowManager.getInstance().isPkgInstalled("com.tencent.mm")) {
                        ToastTool.show("还没有安装微信")
                        return
                    }
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
                    if (!WWindowManager.getInstance().isPkgInstalled("com.tencent.mm")) {
                        ToastTool.show("还没有安装微信")
                        return
                    }
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

    override fun onStop() {
        super.onStop()
        if (tempFile != null) DialogTool.dismissWaitDialog()
    }

    override fun onStart() {
        super.onStart()
        if (tempFile != null) DialogTool.dismissWaitDialog()
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
            ImageTool.getRoundedRectPath(0f, 0f,
                    measuredWidth.toFloat(), measuredHeight.toFloat(), dip(6).toFloat(), dip(6).toFloat(), true)
        }
        private val paint = Paint()

        init {
            paint.isAntiAlias = true
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        override fun onDraw(canvas: Canvas?) {
            if (canvas != null && drawable is BitmapDrawable) {
                paint.xfermode = null
                canvas.drawPath(path, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap((drawable as BitmapDrawable).bitmap, 0f, 0f, paint)
            }
        }
    }
}