package com.intlime.mark.adapter

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.MovieListBean
import com.intlime.mark.tools.StringTool
import com.intlime.mark.tools.glide.RoundTransform
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.wtuadn.pressable.PressableLinearLayout
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-21.
 */
class RelateMovieListAdapter(lists: List<MovieListBean>) : RecyclerListAdapter<MovieListBean>(lists) {
    private val ct = RoundTransform(AppEngine.getContext(), 2f)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var holder = super.onCreateViewHolder(parent, viewType)
        if (holder == null) {
            holder = Holder(PressableLinearLayout(recyclerView.context))
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is Holder) {
            val bean = getItem(position) ?: return
            val imgUrl = StringTool.getQiniuScaledImgUrl(bean.img_url, (holder.imgWidth * 0.95).toInt(), (holder.imgHeight * 0.95).toInt())
            Glide.with(holder.imgView.context)
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .transform(ct)
                    .into(holder.imgView)
            holder.nameView.text = bean.name
        }
    }

    private inner class Holder(itemView: PressableLinearLayout) : RecyclerView.ViewHolder(itemView) {
        lateinit var imgView: ImageView
        lateinit var nameView: TextView
        var imgWidth = 0
        var imgHeight = 0

        init {
            itemView.apply {
                orientation = LinearLayout.VERTICAL
                imgWidth = (WWindowManager.getInstance().width - dip(43.5f)) / 3
                imgHeight = (imgWidth * 0.534).toInt()
                lparams(imgWidth, wrapContent)
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                imgView = imageView().lparams(matchParent, imgHeight)
                nameView = textView {
                    textSize = 13f
                    textColor = Color.parseColor("#10181e")
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    setLineSpacing(0f, 1.4f)
                }.lparams {
                    topMargin = dip(9.1f)
                }
            }
        }
    }
}