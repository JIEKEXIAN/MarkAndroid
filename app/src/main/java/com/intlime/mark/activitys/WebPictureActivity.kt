package com.intlime.mark.activitys

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.tools.UmengTool
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.intlime.mark.view.widget.RecyclePager
import com.intlime.mark.view.widget.recyclePager
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class WebPictureActivity : BaseActivity(), RecyclePager.OnPageChangeListener {
    private var picList: List<String>? = null
    private var position = 0
    private lateinit var recyclePager: RecyclePager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picList = intent.getStringArrayListExtra("list")
        position = intent.getIntExtra("position", -1)
        if (position == -1 || picList == null) {
            finish()
            return
        }
        applyUI()
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener {
                    finish()
                }
                title = "查看图片（${position + 1}/${picList!!.size}）"
                menu.add("下载").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    if (UmengTool.checkPermission(this@WebPictureActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                        savePic()
                    }
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            recyclePager = recyclePager {
                backgroundColor = Color.parseColor("#e9e9e9")
                adapter = MyAdapter(picList!!)
                scrollToPosition(position)
                pageChangeListener = this@WebPictureActivity
            }.lparams(matchParent, matchParent)
        }
    }

    override fun onPageChange(position: Int) {
        toolbar.title = "查看图片（${position + 1}/${picList!!.size}）"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePic()
            } else {
                ToastTool.show("获取权限失败")
            }
        }
    }

    private fun savePic() {
        ThreadManager.getInstance().submit {
            try {
                val bitmap: Bitmap? = Glide.with(AppEngine.getContext())
                        .load(picList!![recyclePager.currentItem])
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get()
                if (bitmap == null) {
                    ToastTool.show("图片保存失败")
                    return@submit
                }
                val path = Environment.getExternalStorageDirectory().absolutePath + "/" + Environment.DIRECTORY_DCIM + "/Mark"
                var dir = File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
                val curDate = Date(System.currentTimeMillis())//获取当前时间
                val fileName = formatter.format(curDate)//用日期作文件名
                val savedFile = File(path, "$fileName.png")
                val bos = BufferedOutputStream(FileOutputStream(savedFile))

                bitmap.compress(Bitmap.CompressFormat.PNG, 80, bos)
                bos.flush()
                bos.close()
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(savedFile)))
                ToastTool.show("已成功保存到" + savedFile.absolutePath, 0, Toast.LENGTH_LONG)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastTool.show("存储空间不足", Toast.LENGTH_LONG, 0)
            }
        }
    }

    private inner class MyAdapter(list: List<String>) : RecyclerListAdapter<String>(list) {
        private val placeholder = ColorDrawable(Color.TRANSPARENT)
        private val error = EmptyDrawable(60f, 60f)

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)
            if (viewHolder is Holder) {
                viewHolder.bar.visibility = View.VISIBLE
                Glide.with(viewHolder.imgView.context)
                        .load(picList!![position])
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .listener(object : RequestListener<String, GlideDrawable> {
                            override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                                viewHolder.bar.visibility = View.INVISIBLE
                                return false
                            }

                            override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                                viewHolder.bar.visibility = View.INVISIBLE
                                return false
                            }
                        })
                        .placeholder(placeholder)
                        .error(error)
                        .into(viewHolder.imgView)
                ThreadManager.getInstance().post {
                    for (i in 0..recyclerView.childCount - 1) {
                        val attacher = recyclerView.getChildAt(i).tag as? PhotoViewAttacher
                        attacher?.update()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            var viewHolder: RecyclerView.ViewHolder? = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                viewHolder = Holder(FrameLayout(this@WebPictureActivity))
            }
            return viewHolder
        }

        private inner class Holder(itemView: FrameLayout) : RecyclerView.ViewHolder(itemView) {
            lateinit var bar: ProgressBar
            lateinit var imgView: ImageView
            private lateinit var attacher: PhotoViewAttacher

            init {
                itemView.apply {
                    layoutParams = RecyclerView.LayoutParams(matchParent, matchParent)
                    bar = progressBar {
                        layoutParams = FrameLayout.LayoutParams(dip(50), dip(50)).apply {
                            gravity = Gravity.CENTER
                        }
                    }
                    imgView = object : ImageView(context) {
                        override fun setImageDrawable(drawable: Drawable?) {
                            super.setImageDrawable(drawable)
                            attacher.update()
                        }
                    }.apply {
                        layoutParams = FrameLayout.LayoutParams(matchParent, matchParent)
                    }
                    addView(imgView)
                    attacher = PhotoViewAttacher(imgView)
                    attacher.scaleType = ImageView.ScaleType.FIT_CENTER
                    tag = attacher
                }
            }
        }
    }
}
