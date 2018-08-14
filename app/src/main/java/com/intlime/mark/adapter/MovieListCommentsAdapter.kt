package com.intlime.mark.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.v4.util.ArrayMap
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.intlime.mark.activitys.ChangeNicknameActivity
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.CommentBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.view.recyclerview.RecyclerListAdapter
import com.intlime.mark.view.widget.CommentView
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-20.
 */
class MovieListCommentsAdapter(lists: MutableList<CommentBean>) : RecyclerListAdapter<CommentBean>(lists), CommentView.OnLikeChangedListener {
    var totalCommentCount = 0

    override fun getItemViewType(position: Int): Int {
        var type = super.getItemViewType(position)
        if (type == 0) {
            val bean = getItem(position)
            if (bean.localType == 0) {
                type = 2
            } else if (bean.localType == 1) {
                type = 3//评论类型
            } else {
                type = 4//评论已删除
            }
        }
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        var holder = super.onCreateViewHolder(parent, viewType)
        if (holder == null) {
            if (viewType == 2) {
                val cv = CommentView(recyclerView.context)
                cv.onLikeChangedListener = this
                holder = object : RecyclerView.ViewHolder(cv) {}
            } else if (viewType == 3) {
                val view = TextView(recyclerView.context).apply {
                    textSize = 12f
                    textColor = Color.parseColor("#818c91")
                    val line = ColorDrawable(Color.parseColor("#e1e1e1"))
                    val bg = ColorDrawable(Color.parseColor("#ebebeb"))
                    val layers = arrayOf<Drawable>(line, bg)
                    val layerDrawable = LayerDrawable(layers)
                    layerDrawable.setLayerInset(0, 0, 0, 0, 0)
                    layerDrawable.setLayerInset(1, 0, 1, 0, 1)
                    backgroundDrawable = layerDrawable
                    layoutParams = ViewGroup.LayoutParams(matchParent, dip(21))
                    gravity = Gravity.CENTER_VERTICAL
                    leftPadding = dip(12.3f)
                }
                view.isEnabled = false
                holder = object : RecyclerView.ViewHolder(view) {}
            } else {
                val view = TextView(recyclerView.context).apply {
                    text = "该评论已删除"
                    textSize = 15f
                    textColor = Color.parseColor("#818c91")
                    layoutParams = ViewGroup.LayoutParams(matchParent, dip(100))
                    gravity = Gravity.CENTER
                }
                view.isEnabled = false
                holder = object : RecyclerView.ViewHolder(view) {}
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder != null) {
            val bean = getItem(position)
            bean ?: return
            val viewType = getItemViewType(position)
            if (viewType == 2) {
                val cv = holder.itemView as CommentView
                cv.commentBean = bean
                cv.update()
                cv.bottomLine.visibility = View.VISIBLE
                cv.bottomPadding = cv.context.dip(0)
                val nextBean = getItem(position + 1)
                if (nextBean != null && nextBean.localType == 1) {
                    cv.bottomLine.visibility = View.GONE
                    cv.bottomPadding = cv.context.dip(13)
                }
            } else if (viewType == 3) {
                (holder.itemView as TextView).text = "${bean.name}"
                if (getItem(position + 1) != null && bean.name.contains("最新评论")) {
                    (holder.itemView as TextView).append("($totalCommentCount)")
                }
            }
        }
    }

    override fun onLikeChangedListener(commentBean: CommentBean?, cv: CommentView) {
        commentBean ?: return
        if (TextUtils.isEmpty(SettingManager.getInstance().nickname)) {
            val dialog = DialogTool.getConfirmDialog("Mark暂时不接受无名之辈，写上你的名号才能互动哦！", null, null)
            dialog.confirm_agree.onClick {
                dialog.dismiss()
                WWindowManager.getInstance().currentActivity?.startActivity<ChangeNicknameActivity>()
            }
            dialog.show()
            return
        }
        val position = (cv.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
        val bean = getItem(position) ?: return
        if (bean.liked == 1) {
            syncLikes(bean, false)
        } else {
            syncLikes(bean, true)
        }
        notifyDataSetChanged()
        NetManager.getInstance().movieListCommentLike(bean.id, if (bean.liked == 1) true else false,
                object : NetRequestCallBack() {
                    override fun onFail(result: ArrayMap<Any, Any>?, error_code: Int) {
                        ThreadManager.getInstance().post {
                            if (bean.liked == 1) {
                                syncLikes(bean, false)
                            } else {
                                syncLikes(bean, true)
                            }
                            notifyDataSetChanged()
                        }
                    }
                })
    }

    private fun syncLikes(bean: CommentBean, isLiked: Boolean) {
        if (isLiked) {
            bean.liked = 1
            bean.likes++
            for (item in lists) {
                if (item.id == bean.id) {
                    item.liked = bean.liked
                    item.likes = bean.likes
                }
            }
        } else {
            bean.liked = 0
            bean.likes--
            for (item in lists) {
                if (item.id == bean.id) {
                    item.liked = bean.liked
                    item.likes = bean.likes
                }
            }
        }
    }
}