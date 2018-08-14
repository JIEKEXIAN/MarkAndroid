package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.activitys.CinemaActivity
import com.intlime.mark.activitys.DailyCardActivity
import com.intlime.mark.activitys.GroupClassifyActivity
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.tools.ZhugeTool
import com.intlime.mark.view.drawable.ResizeDrawable
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableRelativeLayout
import org.jetbrains.anko.*
import java.util.*

/**
 * Created by root on 16-2-29.
 */
class DiscoverClassify(context: Context) : LinearLayout(context) {
    private lateinit var dailyText: TextView

    fun updateDay() {
        dailyText.text = Calendar.getInstance(Locale.CHINA).get(Calendar.DAY_OF_MONTH).toString();
    }

    init {
        orientation = HORIZONTAL
        val line = ColorDrawable(Color.parseColor("#e1e1e1"))
        val bg = ColorDrawable(Color.parseColor("#f4f4f4"))
        val layers = arrayOf<Drawable>(line, bg)
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, 0, 0, 0, 1)
        backgroundDrawable = layerDrawable
        layoutParams = ViewGroup.LayoutParams(matchParent, dip(105f))
        val circleBg = GradientDrawable()
        circleBg.setShape(GradientDrawable.OVAL)
        circleBg.setColor(Color.TRANSPARENT)
        circleBg.setStroke(dip(1), resources.getColor(R.color.dark_blue))
        pressableRelativeLayout {
            layoutParams = LayoutParams(matchParent, matchParent).apply {
                weight = 1f
            }
            imageView {
                id = 31
                backgroundDrawable = circleBg
                scaleType = ImageView.ScaleType.CENTER
                val d = ResizeDrawable(null, ImageTool.getBitmap(R.drawable.discover_classify_group_icon, null), 18f, 18f)
                setImageDrawable(d)
                layoutParams = RelativeLayout.LayoutParams(dip(50), dip(50)).apply {
                    centerHorizontally()
                    topMargin = dip(17f)
                    bottomMargin = dip(5)
                }
            }
            textView("分类查找") {
                textSize = 13f
                textColor = resources.getColor(R.color.a_main_text_color)
                layoutParams = RelativeLayout.LayoutParams(wrapContent, wrapContent).apply {
                    centerHorizontally()
                    below(31)
                }
            }
            PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
            onClick {
                WWindowManager.getInstance().currentActivity.startActivity<GroupClassifyActivity>()
                ZhugeTool.track("全部分类")
            }
        }
        pressableRelativeLayout {
            layoutParams = LayoutParams(matchParent, matchParent).apply {
                weight = 1f
            }
            dailyText = textView {
                id = 32
                backgroundDrawable = circleBg
                textSize = 23f
                textColor = resources.getColor(R.color.dark_blue)
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                layoutParams = RelativeLayout.LayoutParams(dip(50), dip(50)).apply {
                    centerHorizontally()
                    topMargin = dip(17f)
                    bottomMargin = dip(5)
                }
            }
            textView("每日电影卡片") {
                textSize = 13f
                textColor = resources.getColor(R.color.a_main_text_color)
                layoutParams = RelativeLayout.LayoutParams(wrapContent, wrapContent).apply {
                    centerHorizontally()
                    below(32)
                }
            }
            PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
            onClick {
                WWindowManager.getInstance().currentActivity.startActivity<DailyCardActivity>()
            }
        }
        pressableRelativeLayout {
            layoutParams = LayoutParams(matchParent, matchParent).apply {
                weight = 1f
            }
            imageView {
                id = 33
                backgroundDrawable = circleBg
                scaleType = ImageView.ScaleType.CENTER
                val d = ResizeDrawable(null, ImageTool.getBitmap(R.drawable.discover_classify_cinema_icon, null), 27f, 18.5f)
                setImageDrawable(d)
                layoutParams = RelativeLayout.LayoutParams(dip(50), dip(50)).apply {
                    centerHorizontally()
                    topMargin = dip(17f)
                    bottomMargin = dip(5)
                }
            }
            textView("影院热映") {
                textSize = 13f
                textColor = resources.getColor(R.color.a_main_text_color)
                layoutParams = RelativeLayout.LayoutParams(wrapContent, wrapContent).apply {
                    centerHorizontally()
                    below(33)
                }
            }
            PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
            onClick {
                WWindowManager.getInstance().currentActivity.startActivity<CinemaActivity>()
                ZhugeTool.track("院线电影")
            }
        }
        updateDay()
    }
}
