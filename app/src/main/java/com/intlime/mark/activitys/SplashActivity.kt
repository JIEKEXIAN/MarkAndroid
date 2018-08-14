package com.intlime.mark.activitys

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.intlime.mark.R
import com.intlime.mark.adapter.ListPagerAdapter
import com.intlime.mark.application.Session
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.tools.ImageTool
import com.intlime.mark.tools.ZhugeTool
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.onPageChangeListener
import org.jetbrains.anko.support.v4.viewPager
import java.util.*

/**
 * Created by root on 16-1-14.
 */
class SplashActivity : BaseActivity() {
    private lateinit var viewPager: ViewPager
    private lateinit var indicator: View
    private lateinit var begin: View

    override fun onCreate(savedInstanceState: Bundle?) {
        customAnimation = false
        val windows = WWindowManager.getInstance().windows
        if ((intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0 && !windows.isEmpty()) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        while (!windows.isEmpty()) {
            WWindowManager.getInstance().windows.pop().finish()
        }
        super.onCreate(savedInstanceState)
        if (Session.uid > 0) {
            startActivity<MainActivity>()
            finish()
            SettingManager.getInstance().setHasToShowGuide(false)
        } else if (SettingManager.getInstance().hasToShowGuide()) {
            initGuide()
        } else {
            initGuide()
            startActivity<LoginActivity>()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Session.uid <= 0) ZhugeTool.onDestroy()
    }

    private fun initGuide() {
        frameLayout {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            backgroundDrawable = BitmapDrawable(ImageTool.getBitmap(R.drawable.guide_bg, options))
            lparams(matchParent, matchParent)
            viewPager = viewPager {
                adapter = ListPagerAdapter(getViewList())
                onPageChangeListener {
                    onPageSelected { p ->
                        indicator.invalidate()
                        if (p == 4) {
                            begin.visibility = View.VISIBLE
                        } else {
                            begin.visibility = View.GONE
                        }
                    }
                }
            }.lparams(matchParent, matchParent)
            indicator = object : View(this@SplashActivity) {
                val dColor = Color.parseColor("#d2d2d2")
                val fColor = Color.parseColor("#304851")
                val paint = Paint()

                init {
                    paint.isDither = true
                    paint.isAntiAlias = true
                }

                override fun onDraw(canvas: Canvas) {
                    for (i in 0..4) {
                        if (i == viewPager.currentItem) {
                            paint.color = fColor
                        } else {
                            paint.color = dColor
                        }
                        canvas.drawCircle(dip(3.5f).toFloat() + i * dip(16), measuredHeight / 2f, measuredHeight / 2f, paint)
                    }
                }
            }.lparams(dip(72), dip(7)) {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dip(52.5f)
            }
            addView(indicator)
            begin = pressableTextView {
                visibility = View.GONE
                text = "开始体验"
                textSize = 13f
                textColor = resources.getColor(R.color.a_main_text_color)
                gravity = Gravity.CENTER
                backgroundResource = R.drawable.bg_button_stroke_white
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                onClick {
                    startActivity<LoginActivity>(ANIMATION to true)
                    SettingManager.getInstance().setHasToShowGuide(false)
                    //                    ZhugeTool.track("开始体验")
                }
            }.lparams(dip(156.5f), dip(39.5f)) {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dip(40)
            }
        }
    }

    private fun getViewList(): ArrayList<View> {
        val item1 = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            imageBitmap = ImageTool.getBitmap(R.drawable.guide_img1, options)
        }
        val item2 = getItem("观影指南", "数百个精选影单，帮你筛选好电影", R.drawable.guide_img2, dip(28))
        val item3 = getItem("电影清单", "标记想看、已看、收藏的电影", R.drawable.guide_img3, dip(25))
        val item4 = getItem("播放源", "聚合数十家播放源，告诉你哪可以播放", R.drawable.guide_img4, dip(35))
        val item5 = getItem("电影卡片", "为你选取剧照和台词自动生成美图", R.drawable.guide_img5, dip(1))
        return arrayListOf(item1, item2, item3, item4, item5)
    }

    private fun getItem(title: String, title2: String, imgResId: Int, imgMargin: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            textView {
                text = title
                textSize = 32f
                textColor = Color.parseColor("#496069")
                topPadding = dip(45)
                layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
            }
            textView {
                text = title2
                textSize = 14f
                textColor = Color.parseColor("#818c91")
                topPadding = dip(10)
                bottomPadding = imgMargin
                layoutParams = ViewGroup.LayoutParams(wrapContent, wrapContent)
            }
            imageView {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.RGB_565
                imageBitmap = ImageTool.getBitmap(imgResId, options)
                val lp = LinearLayout.LayoutParams(wrapContent, matchParent)
                lp.bottomMargin = dip(90)
                layoutParams = lp
            }
        }
    }
}
