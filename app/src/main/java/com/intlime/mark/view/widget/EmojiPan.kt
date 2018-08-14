package com.intlime.mark.view.widget

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewPager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.intlime.mark.R
import com.intlime.mark.adapter.ListPagerAdapter
import com.intlime.mark.tools.DensityUtils
import com.intlime.mark.tools.MResource
import com.rockerhieu.emojicon.EmojiconTextView
import com.wtuadn.pressable.IPressable
import com.wtuadn.pressable.PressableImageView
import com.wtuadn.pressable.PressableUtils
import org.jetbrains.anko.dip
import org.jetbrains.anko.topPadding
import java.util.*

/**
 * 表情选择控件
 * Created by wtuadn on 15/10/27.
 */
class EmojiPan(context: Context, var mEditText: EditText? = null) : FrameLayout(context) {
    private lateinit var viewPager: ViewPager
    private val unicodes = arrayOf(
            arrayOf("😄", "😃", "😀", "😊", "☺️", "😉", "😍", "😘", "😚", "😗", "😙", "😜", "😝", "😛", "😳", "😁", "😔", "😌", "😒", "😞", ""),
            arrayOf("😣", "😢", "😂", "😭", "😪", "😥", "😰", "😅", "😓", "😩", "😫", "😨", "😱", "😠", "😡", "😤", "😖", "😆", "😋", "😷", ""),
            arrayOf("😎", "😴", "😵", "😲", "😟", "😦", "😧", "😈", "👿", "😮", "😬", "😐", "😕", "😯", "😶", "😇", "😏", "😑", "💩", "🔥", ""),
            arrayOf("👍", "👎", "👌", "👊", "✊", "✌️", "✋", "👆", "👇", "👉", "👈", "🙏", "👏", "💪", "❤️", "💔", "💋", "🍗", "🍺", "💰", ""),
            arrayOf("👑", "🎉", "🐶", "🐱", "🐭", "🐹", "🐰", "🐸", "🐯", "🐻", "🐷", "🐮", "🐴", "🐼", "⭐️", "☀️", "🌙", "⚡️", "☔️", "👻", ""),
            arrayOf("🎅", "🎁", "💊", "🏀", "⚽️", "⚾️", "🎱", "🈲", "🈶", "🈚️", "㊙️", "㊗️", "🚫", ""))

    init{
        viewPager = ViewPager(context)
        viewPager.overScrollMode = View.OVER_SCROLL_NEVER
        val pagerViews = ArrayList<View>()
        initPagerViews(pagerViews)
        viewPager.adapter = ListPagerAdapter(pagerViews)
        val indicator = initIndicator(viewPager)
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                indicator.invalidate()
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        addView(viewPager, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setBackgroundColor(MResource.getColor(R.color.bg))
    }

    private fun initPagerViews(pagerViews: MutableList<View>) {
        val listener = View.OnClickListener { v ->
            val index = mEditText!!.selectionStart//获取光标所在位置
            val edit = mEditText!!.text//获取EditText的文字
            if (v is TextView) {
                val text = v.text
                if (index < 0 || index >= edit.length) {
                    edit.append(text)
                } else {
                    edit.insert(index, text)//光标所在位置插入文字
                }
            } else if (v is ImageView) {
                if (!TextUtils.isEmpty(edit)) {
                    edit.delete(index - Character.charCount(Character.codePointAt(edit, index - if (edit.length > 1) 2 else 1)), index)
                }
            }
        }
        for (i in unicodes.indices) {
            val gridView = GridView(context)
            gridView.overScrollMode = View.OVER_SCROLL_NEVER
            gridView.numColumns = 7
            gridView.verticalSpacing = DensityUtils.dp2px(context, 15f)
            gridView.horizontalSpacing = DensityUtils.dp2px(context, 10f)
            val padding = DensityUtils.dp2px(context, 23f)
            gridView.setPadding((padding * 0.65).toInt(), padding, (padding * 0.65).toInt(), padding)
            val finalI = i
            gridView.adapter = object : BaseAdapter() {
                override fun getCount(): Int {
                    return unicodes[finalI].size
                }

                override fun getItem(position: Int): Any {
                    return position
                }

                override fun getItemId(position: Int): Long {
                    return position.toLong()
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view: View
                    if (position == count - 1) {//最后一个
                        val imageView = PressableImageView(context)
                        imageView.minimumHeight = DensityUtils.dp2px(context, 40f)
                        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        imageView.setImageResource(R.drawable.emoji_pan_delete)
                        PressableUtils.setPressableDrawable(imageView, MResource.getColor(R.color.black_pressed_color))
                        view = imageView
                    } else {
                        val textView = PressableEmojiTextView(context)
                        textView.minHeight = DensityUtils.dp2px(context, 40f)
                        textView.setEmojiconSize(DensityUtils.dp2px(context, 25f))
                        textView.text = unicodes[finalI][position]
                        textView.gravity = Gravity.CENTER
                        textView.topPadding = dip(6)
                        PressableUtils.setPressableDrawable(textView, MResource.getColor(R.color.black_pressed_color));
                        view = textView
                    }
                    view.setOnClickListener(listener)
                    return view
                }
            }
            pagerViews.add(gridView)
        }
    }

    private fun initIndicator(viewPager: ViewPager): View {
        val dColor = Color.parseColor("#bbbbbb")
        val cColor = Color.parseColor("#8b8b8b")

        val paint = Paint()
        paint.isDither = true
        paint.isAntiAlias = true
        val divideWidth = DensityUtils.dp2px(context, 9f)
        val indicatorWidth = DensityUtils.dp2px(context, 7f)
        val totalWidth = divideWidth * (unicodes.size - 1) + indicatorWidth * unicodes.size
        val indicator = object : View(context) {
            override fun onDraw(canvas: Canvas) {
                canvas.save()
                canvas.translate(((width - totalWidth) / 2).toFloat(), 0f)
                for (i in unicodes.indices) {
                    if (i == viewPager.currentItem) {
                        paint.color = cColor
                    } else {
                        paint.color = dColor
                    }
                    val left = i * (indicatorWidth + divideWidth)
                    canvas.drawCircle((left + indicatorWidth / 2).toFloat(), (height / 2).toFloat(), (indicatorWidth / 2).toFloat(), paint)
                }
                canvas.restore()
            }
        }
        val lp = FrameLayout.LayoutParams(
                DensityUtils.dp2px(context, 100f), DensityUtils.dp2px(context, 10f))
        lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        lp.bottomMargin = DensityUtils.dp2px(context, 27f)
        addView(indicator, lp)
        return indicator
    }

    private inner class PressableEmojiTextView(context: Context) : EmojiconTextView(context), IPressable {
        private var pressableDrawable: Drawable? = null

        @TargetApi(21)
        override fun drawableHotspotChanged(x: Float, y: Float) {
            super.drawableHotspotChanged(x, y)
            if (pressableDrawable != null) {
                pressableDrawable!!.setHotspot(x, y)
            }
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()

            if (pressableDrawable != null) {
                pressableDrawable!!.state = drawableState
                invalidate()
            }
        }

        override fun onSizeChanged(width: Int, height: Int, oldwidth: Int, oldheight: Int) {
            super.onSizeChanged(width, height, oldwidth, oldheight)

            if (pressableDrawable != null) {
                pressableDrawable!!.setBounds(0, 0, width, height)
            }
        }

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            if (pressableDrawable != null) {
                pressableDrawable!!.draw(canvas)
            }
        }

        override fun verifyDrawable(who: Drawable): Boolean {
            if (pressableDrawable != null) {
                return super.verifyDrawable(who) || who === pressableDrawable
            }
            return super.verifyDrawable(who)
        }

        override fun jumpDrawablesToCurrentState() {
            super.jumpDrawablesToCurrentState()

            if (pressableDrawable != null) {
                pressableDrawable!!.jumpToCurrentState()
            }
        }

        override fun setPressableDrawable(pressableDrawable: Drawable) {
            this.pressableDrawable = pressableDrawable
        }
    }
}
