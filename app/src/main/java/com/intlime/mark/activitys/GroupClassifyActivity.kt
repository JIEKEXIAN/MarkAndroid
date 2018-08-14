package com.intlime.mark.activitys

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.intlime.mark.R
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.ClassifyBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.glide.PinnedKey
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource

/**
 * Created by root on 16-3-1.
 */
class GroupClassifyActivity : BaseActivity(), View.OnClickListener {
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUI()
        applyData()
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "全部影单"
            }
            scrollView {
                backgroundColor = Color.parseColor("#ebebeb")
                lparams(matchParent, matchParent)
                isFillViewport = true
                container = verticalLayout {
                    lparams(matchParent, wrapContent)
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
                applyClassify(result["list"] as List<ClassifyBean>)
            }
        }
        DialogTool.showWaitDialog("加载中", DialogTool.FINISH_ON_BACK, callback)
        NetManager.getInstance().getGroupClassify(callback)
    }

    private fun applyClassify(list: List<ClassifyBean>) {
        for (bean in list) {
            container.addView(getGroupItem(bean))
        }
        if (list.isNotEmpty()) {
            (container.getChildAt(container.childCount - 1).layoutParams as LinearLayout.LayoutParams).bottomMargin = dip(12)
        }
    }

    private fun getGroupItem(groupBean: ClassifyBean): ViewGroup {
        return GridLayout(this).apply {
            columnCount = 4
            orientation = GridLayout.HORIZONTAL
            backgroundColor = Color.parseColor("#f4f4f4")
            layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                horizontalMargin = dip(12)
                topMargin = dip(12)
            }
            val itemBg = GradientDrawable()
            itemBg.setColor(Color.TRANSPARENT)
            itemBg.setStroke(1, Color.parseColor("#d2d2d2"))
            val itemWidth = (WWindowManager.getInstance().width - dip(24)) / 4
            relativeLayout {
                backgroundDrawable = itemBg
                layoutParams = GridLayout.LayoutParams().apply {
                    width = itemWidth
                    height = itemWidth
                    rowSpec = GridLayout.spec(0, 2)
                    columnSpec == GridLayout.spec(0, 1)
                }
                textView(groupBean.name) {
                    textSize = 14f
                    textColor = Color.parseColor("#6f93a2")
                    gravity = Gravity.CENTER_HORIZONTAL
                    Glide.with(context)
                            .load(groupBean.imgUrl)
                            .diskCacheStrategy(DiskCacheStrategy.RESULT)
                            .signature(PinnedKey(groupBean.imgUrl))
                            .override(dip(30), dip(30))
                            .into(object : SimpleTarget<GlideDrawable>() {
                                override fun onResourceReady(d: GlideDrawable?, glideAnimation: GlideAnimation<in GlideDrawable>?) {
                                    if (d != null) {
                                        compoundDrawablePadding = dip(7.8f)
                                        setCompoundDrawablesWithIntrinsicBounds(null, d, null, null)
                                    }
                                }

                            })
                }.lparams {
                    centerInParent()
                }
            }
            var maxLengthView: TextView? = null
            for (i in groupBean.list!!.indices) {
                val bean = groupBean.list!![i]
                pressableTextView() {
                    tag = bean
                    backgroundDrawable = itemBg.mutate().constantState.newDrawable()
                    horizontalPadding = dip(5)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = itemWidth
                        height = itemWidth / 2
                    }
                    text = bean.name
                    textSize = 14f
                    textColor = resources.getColor(R.color.a_main_text_color)
                    gravity = Gravity.CENTER
                    PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                    setOnClickListener(this@GroupClassifyActivity)
                    if (maxLengthView == null) {
                        maxLengthView = this
                    } else {
                        if (length() > maxLengthView!!.length()) {
                            maxLengthView = this
                        }
                    }
                }
            }
            if (maxLengthView != null)
                resizeText(maxLengthView!!)
            //填补空缺
            val size = groupBean.list!!.size
            if (size < 6) {
                for (i in  size..5) {
                    view {
                        backgroundDrawable = itemBg.mutate().constantState.newDrawable()
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = itemWidth
                            height = itemWidth / 2
                        }
                    }
                }
            } else {
                val count = 4 - (size - 6) % 4
                if (count == 4) return@apply
                for (i in 1..count) {
                    view {
                        backgroundDrawable = itemBg.mutate().constantState.newDrawable()
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = itemWidth
                            height = itemWidth / 2
                        }
                    }
                }
            }
        }
    }

    private fun resizeText(view: TextView) {
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.layout ?: return false
                if (view.layout.lineCount > 1) {
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.textSize - sp(1))
                    return false
                } else {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    val parent = view.parent as ViewGroup
                    for (i in 1..parent.childCount) {
                        val child = parent.getChildAt(i)
                        if (child is TextView) {
                            child.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.textSize)
                        }
                    }
                    return true
                }
            }
        })
    }

    override fun onClick(v: View?) {
        val bean = v?.tag as? ClassifyBean
        if (bean != null) {
            startActivity<MovieListSetActivity>(BEAN to bean)
        }
    }
}
