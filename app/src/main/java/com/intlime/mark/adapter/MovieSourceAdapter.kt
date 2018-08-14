package com.intlime.mark.adapter

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.intlime.mark.R
import com.intlime.mark.application.AppEngine
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.tools.JsonTool
import com.intlime.mark.tools.glide.PinnedKey
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import org.jetbrains.anko.*
import org.json.JSONObject

/**
 * Created by root on 16-3-9.
 */
class MovieSourceAdapter(lists: MutableList<JSONObject>?) : RecyclerListAdapter<JSONObject>(lists) {
    override fun getItemViewType(position: Int): Int {
        var type = super.getItemViewType(position)
        if (type == 0) {
            if (getItem(position).optBoolean("is_other_head")) {
                type = 2
            } else {
                type = 3
            }
        }
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var holder = super.onCreateViewHolder(parent, viewType)
        if (holder == null) {
            if (viewType == 2) {
                val view = LinearLayout(AppEngine.getContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    view {
                        layoutParams = LinearLayout.LayoutParams(matchParent, dip(1))
                        backgroundColor = Color.parseColor("#e1e1e1")
                    }
                    val textView = TextView(AppEngine.getContext())
                    textView.text = "这些平台可能会有免费的播放源"
                    textView.textColor = resources.getColor(R.color.dark_blue)
                    textView.textSize = 15f
                    textView.backgroundColor = resources.getColor(R.color.bg)
                    textView.setPadding(dip(15.6f), dip(19f), 0, dip(6))
                    addView(textView, ViewGroup.LayoutParams(matchParent, wrapContent))
                    val item = View.inflate(AppEngine.getContext(), R.layout.movie_source_item_layout, null)
                    item.layoutParams = RecyclerView.LayoutParams(matchParent, AppEngine.getContext().dip(58))
                    addView(item)
                }
                view.layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent)
                holder = Holder(view)
            } else {
                val view = View.inflate(AppEngine.getContext(), R.layout.movie_source_item_layout, null)
                view.layoutParams = RecyclerView.LayoutParams(matchParent, AppEngine.getContext().dip(58))
                holder = Holder(view)
            }
        }
        return holder
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        if (viewHolder is Holder) {
            if (getItemViewType(position) == 2) {
                viewHolder.itemView.setOnClickListener(null)
                (viewHolder.itemView as ViewGroup).getChildAt(2).onClick {
                    recyclerItemListener.onClick(viewHolder.itemView)
                }
            }
            if (getItemViewType(position + 1) == 2) {
                viewHolder.line.visibility = View.INVISIBLE
            } else {
                viewHolder.line.visibility = View.VISIBLE
            }
            val jsonObject = getItem(position)
            val imgUrl = JsonTool.optString(jsonObject, "icon_url")
            Glide.with(viewHolder.imageView.context)
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .signature(PinnedKey(imgUrl))
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: Exception?, model: String, target: Target<GlideDrawable>,
                                                 isFirstResource: Boolean): Boolean {
                            ThreadManager.getInstance().post {
                                viewHolder.imageView.setImageResource(R.drawable.movie_source_default_icon)
                            }
                            return false
                        }

                        override fun onResourceReady(resource: GlideDrawable, model: String,
                                                     target: Target<GlideDrawable>, isFromMemoryCache: Boolean,
                                                     isFirstResource: Boolean): Boolean {
                            return false
                        }
                    })
                    .into(viewHolder.imageView)
            viewHolder.name.text = JsonTool.optString(jsonObject, "name")
            val isFree = jsonObject.optInt("is_free", -1)
            if (isFree == -1) {
                viewHolder.charge.text = ""
            } else {
                viewHolder.charge.text = if (jsonObject.optInt("is_free") == 1) "免费" else "收费"
            }
        }
    }

    private inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView by lazy {
            itemView.findViewById(R.id.imageView) as ImageView
        }
        val name: TextView by lazy {
            itemView.findViewById(R.id.name) as TextView
        }
        val charge: TextView by lazy {
            itemView.findViewById(R.id.charge) as TextView
        }
        val line by lazy {
            itemView.findViewById(R.id.line)
        }
    }
}