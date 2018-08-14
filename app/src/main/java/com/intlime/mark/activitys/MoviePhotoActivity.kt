package com.intlime.mark.activitys

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.Session
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.tools.UmengTool
import com.intlime.mark.view.drawable.EmptyDrawable
import com.intlime.mark.view.recyclerview.LoadRecyclerView
import com.intlime.mark.view.recyclerview.RecyclerItemListener
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.wtuadn.pressable.PressableImageView
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import java.util.*

/**
 * Created by wtuadn on 16/04/18.
 */
class MoviePhotoActivity : BaseActivity(), LoadRecyclerView.OnLoadListener {
    private val limit = 10;
    private val resultCode = 77
    private var photoList: ArrayList<Array<String>>? = null
    private lateinit var loadRecyclerView: LoadRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoList = intent.getSerializableExtra("list") as? ArrayList<Array<String>>
        if (photoList == null) {
            finish()
            return
        }
        applyUI()
        applyData()
        val y = Session.map[MovieCardShareActivity.PHOTO_POSITION] as? Int
        if (y != null) {
            loadRecyclerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    loadRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                    loadRecyclerView.scrollBy(0, y)
                    return true
                }
            })
        }
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener {
                    finish()
                }
                title = "剧照"
            }.lparams(matchParent, dip(49))
            loadRecyclerView = LoadRecyclerView(context).apply {
                backgroundColor = Color.parseColor("#10181e")
                setLoadListener(this@MoviePhotoActivity)
                setHasFixedSize(true)
                val glm = GridLayoutManager(context, 2)
                glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        if (adapter.getItemViewType(position) != 0)
                            return glm.spanCount
                        else
                            return 1
                    }
                }
                layoutManager = glm
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                        view ?: return
                        outRect ?: return
                        val params = view.layoutParams as GridLayoutManager.LayoutParams
                        val position = params.viewLayoutPosition
                        outRect.set(0, 0, 0, 0)
                        if (position != adapter.itemCount - 1) {
                            if (position >= 2) {
                                outRect.top = dip(2)
                            }
                            if (position % 2 == 0) {
                                outRect.right = dip(1)
                            }
                        }
                    }
                })
                adapter = MyAdapter(photoList!!)
                setRecyclerItemListener(object : RecyclerItemListener() {
                    init {
                        clickable = true
                    }

                    override fun onItemClick(v: View?, position: Int) {
                        if (position == 0) {
                            if (UmengTool.checkPermission(this@MoviePhotoActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE, UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE)) {
                                val intent = Intent()
                                intent.type = "image/*"
                                intent.action = Intent.ACTION_GET_CONTENT
                                startActivityForResult(intent, resultCode)
                            }
                        } else {
                            Session.map.put(MovieCardShareActivity.PHOTO, photoList!![position - 1][1])
                            finish()
                        }
                    }
                })
            }
            addView(loadRecyclerView, ViewGroup.LayoutParams(matchParent, matchParent))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == UmengTool.REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(intent, resultCode)
            } else {
                ToastTool.show("获取权限失败")
            }
        }
    }

    private fun applyData() {
        if (photoList!!.isEmpty()) {
            val callback = object : NetRequestCallBack() {
                override fun onDefault() {
                    DialogTool.dismissWaitDialog()
                }

                override fun onSuccess(result: ArrayMap<*, *>) {
                    runOnUiThread {
                        photoList!!.addAll(result["list"] as ArrayList<Array<String>>)
                        if (photoList!!.size < limit) {
                            loadRecyclerView.isCanLoad = false
                        } else {
                            loadRecyclerView.isCanLoad = true
                        }
                        Session.map.put(MovieCardShareActivity.PHOTO_LIST, photoList)
                        loadRecyclerView.adapter.notifyDataSetChanged()
                    }
                }
            }
            DialogTool.showWaitDialog("加载图片中", DialogTool.FINISH_ON_BACK, callback)
            NetManager.getInstance().getMoviePhotos(intent.getIntExtra("id", 0), 0, callback)
        }
    }

    override fun onLoad() {
        NetManager.getInstance().getMoviePhotos(intent.getIntExtra("id", 0), photoList!!.size, object : NetRequestCallBack() {
            override fun onDefault() {
                loadRecyclerView.loadFinish()
            }

            override fun onSuccess(result: ArrayMap<*, *>) {
                runOnUiThread {
                    val list = result["list"] as ArrayList<Array<String>>
                    if (list.size < limit) {
                        loadRecyclerView.isCanLoad = false
                    }
                    if (list.isNotEmpty()) {
                        photoList!!.addAll(list)
                        loadRecyclerView.adapter.notifyItemRangeInserted(photoList!!.size + 1, list.size)
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == this.resultCode && data != null) {
            Session.map.put(MovieCardShareActivity.PHOTO, "file://" + ImageTool.getPath(AppEngine.getContext(), data.data))
            finish()
        }
    }

    override fun finish() {
        if (photoList != null) {
            Session.map.put(MovieCardShareActivity.PHOTO_POSITION, loadRecyclerView.computeVerticalScrollOffset())
        }
        super.finish()
    }

    private inner class MyAdapter(lists: MutableList<Array<String>>) : RecyclerListAdapter<Array<String>>(lists) {
        private val placeholder = ColorDrawable(Color.parseColor("#2c2c2c"))
        private val error = EmptyDrawable(60f, 60f)
        private val mWidth = (WWindowManager.getInstance().width - dip(2)) / 2
        private val mHeight = dip(122)

        override fun getNormalItemCount(): Int {
            return super.getNormalItemCount() + 1
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
            super.onBindViewHolder(viewHolder, position)

            if (viewHolder is Holder) {
                if (position == 0) {
                    viewHolder.imgView.scaleType = ImageView.ScaleType.CENTER
                    viewHolder.imgView.imageResource = R.drawable.movie_photo_upload_icon
                } else {
                    viewHolder.imgView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(viewHolder.imgView.context)
                            .load(photoList!![position - 1][0])
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .placeholder(placeholder)
                            .error(error)
                            .into(viewHolder.imgView)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            var viewHolder: RecyclerView.ViewHolder? = super.onCreateViewHolder(parent, viewType)
            if (viewHolder == null) {
                val imgView = PressableImageView(this@MoviePhotoActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(mWidth, mHeight)
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                }
                viewHolder = Holder(imgView)
            }
            return viewHolder
        }

        private inner class Holder(val imgView: ImageView) : RecyclerView.ViewHolder(imgView) {
        }
    }
}