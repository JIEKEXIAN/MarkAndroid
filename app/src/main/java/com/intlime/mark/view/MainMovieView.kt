package com.intlime.mark.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.intlime.mark.R
import com.intlime.mark.activitys.BaseActivity
import com.intlime.mark.activitys.SingleEditActivity
import com.intlime.mark.application.SettingManager
import com.intlime.mark.tools.ZhugeTool
import com.tendcloud.tenddata.TCAgent
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-7-21.
 */
class MainMovieView(val context: BaseActivity, val checkBox: CheckBox, val rightView: TextView) : RelativeLayout(context), View.OnClickListener {
    companion object {
        val EXIT_MULTI_CHOICE_ACTION = "EXIT_MULTI_CHOICE_ACTION"
    }

    lateinit var radioGroup: RadioGroup
    private lateinit var todoTab: RadioButton
    private lateinit var doneTab: RadioButton
    private lateinit var singleTab: RadioButton
    private val sortDialog by lazy {
        SortDialog(context)
    }

    private var isDoneInited = false
    private var isSingleInited = false
    private val movieTodoView by lazy {
        val v = MovieTodoView(context, checkBox)
        addView(v, LayoutParams(matchParent, matchParent).apply {
            below(radioGroup)
        })
        return@lazy v
    }
    private val movieDoneView by lazy {
        val v = MovieDoneView(context, checkBox)
        addView(v, LayoutParams(matchParent, matchParent).apply {
            below(radioGroup)
        })
        isDoneInited = true
        return@lazy v
    }
    private val movieSingleView by lazy {
        val v = MovieSingleView(context)
        addView(v, LayoutParams(matchParent, matchParent).apply {
            below(radioGroup)
        })
        isSingleInited = true
        return@lazy v
    }

    init {
        initRadio()
        rightView.setOnClickListener(this)
    }

    private fun initRadio() {
        radioGroup = radioGroup {
            id = 253
            layoutParams = ViewGroup.LayoutParams(matchParent, dip(44))
            orientation = LinearLayout.HORIZONTAL
            backgroundResource = R.drawable.toolbar_bg
            var bd: Drawable? = null
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                bd = ColorDrawable()
            }
            todoTab = radioButton {
                id = 1
                textSize = 13f
                setLineSpacing(0f, 1.1f)
                gravity = Gravity.CENTER_HORIZONTAL
                buttonDrawable = bd
                padding = 0
            }.lparams(0, matchParent) {
                weight = 1f
            }
            doneTab = radioButton {
                id = 2
                textSize = 13f
                setLineSpacing(0f, 1.1f)
                gravity = Gravity.CENTER_HORIZONTAL
                buttonDrawable = bd
                padding = 0
            }.lparams(0, matchParent) {
                weight = 1f
            }
            singleTab = radioButton {
                id = 3
                textSize = 13f
                setLineSpacing(0f, 1.1f)
                gravity = Gravity.CENTER_HORIZONTAL
                buttonDrawable = bd
                padding = 0
            }.lparams(0, matchParent) {
                weight = 1f
            }

        }
        val indicatorLine = ColorDrawable(Color.BLACK)
        indicatorLine.setBounds(0, 0, dip(26), dip(2.5f))
        var isFirstCheck = true
        var lastCheckedId = 1
        radioGroup.setOnCheckedChangeListener { radioGroup, id ->
            checkBox.setOnCheckedChangeListener(null)
            todoTab.setCompoundDrawables(null, null, null, null)
            doneTab.setCompoundDrawables(null, null, null, null)
            singleTab.setCompoundDrawables(null, null, null, null)
            if (id == 1) {
                if (isFirstCheck) {
                    isFirstCheck = false
                } else {
                    ZhugeTool.track("进入想看", null)
                    TCAgent.onPageStart(context, "想看")
                }
                checkBox.setOnCheckedChangeListener(movieTodoView)
                todoTab.setCompoundDrawables(null, null, null, indicatorLine)
                movieTodoView.visibility = View.VISIBLE
                if (isDoneInited) movieDoneView.visibility = View.GONE
                if (isSingleInited) movieSingleView.visibility = View.GONE
                checkBox.isChecked = if (SettingManager.getInstance().todoViewMode == 1) true else false
                checkBox.visibility = View.VISIBLE
                rightView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.movie_sort_icon, 0)
                rightView.text = "排序"
            } else if (id == 2) {
                ZhugeTool.track("进入已看", null)
                TCAgent.onPageStart(context, "已看")
                checkBox.setOnCheckedChangeListener(movieDoneView)
                doneTab.setCompoundDrawables(null, null, null, indicatorLine)
                movieTodoView.visibility = View.GONE
                movieDoneView.visibility = View.VISIBLE
                if (isSingleInited) movieSingleView.visibility = View.GONE
                checkBox.isChecked = if (SettingManager.getInstance().doneViewMode == 1) true else false
                checkBox.visibility = View.VISIBLE
                rightView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.movie_sort_icon, 0)
                rightView.text = "排序"
            } else {
                ZhugeTool.track("进入自建影单", null)
                TCAgent.onPageStart(context, "自建影单")
                singleTab.setCompoundDrawables(null, null, null, indicatorLine)
                movieTodoView.visibility = View.GONE
                if (isDoneInited) movieDoneView.visibility = View.GONE
                movieSingleView.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                rightView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                rightView.text = "自建影单"
            }
            if (lastCheckedId == 1) {
                TCAgent.onPageEnd(context, "想看")
            } else if (lastCheckedId == 2) {
                TCAgent.onPageEnd(context, "已看")
            } else {
                TCAgent.onPageEnd(context, "自建影单")
            }
            updateTabs()
            lastCheckedId = id
        }
        radioGroup.check(1)
    }

    private fun updateTabs() {
        val normalColor = Color.parseColor("#496069")
        val highlightColor = Color.parseColor("#10181e")
        todoTab.textColor = normalColor
        doneTab.textColor = normalColor
        singleTab.textColor = normalColor
        if (radioGroup.checkedRadioButtonId == 1) {
            todoTab.textColor = highlightColor
        } else if (radioGroup.checkedRadioButtonId == 2) {
            doneTab.textColor = highlightColor
        } else {
            singleTab.textColor = highlightColor
        }
        val todoCount = MovieTodoView.getCount()
        todoTab.text = "$todoCount\n想看"
        val doneCount = MovieDoneView.getCount()
        doneTab.text = "$doneCount\n已看"
        val singleCount = MovieSingleView.myAdapter.normalItemCount
        singleTab.text = "$singleCount\n影单"
        setCountSize(todoTab)
        setCountSize(doneTab)
        setCountSize(singleTab)
    }

    private fun setCountSize(textView: TextView) {
        val ss = SpannableString(textView.text)
        ss.setSpan(AbsoluteSizeSpan(sp(17)), 0, ss.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
    }

    fun reloadAll() {
        movieTodoView.onSortChanged(movieTodoView.sortType, false)
        if (isDoneInited) {
            movieDoneView.onSortChanged(movieDoneView.sortType, false)
        } else {
            MovieDoneView.newCursor()
        }
        updateTabs()
    }

    override fun onClick(v: View?) {
        v ?: return
        if (radioGroup.checkedRadioButtonId == 3) {
            context.startActivity<SingleEditActivity>("type" to 1)
            ZhugeTool.track("新建影单", null)
        } else {
            sortDialog.setMode(radioGroup.checkedRadioButtonId)
            if (sortDialog.parent == null) {
                (context.rootView as ViewGroup).addView(sortDialog)
            } else {
                (sortDialog.parent as ViewGroup).removeView(sortDialog)
            }
        }
    }

    override fun onVisibilityChanged(changedView: View?, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            val id = radioGroup.checkedRadioButtonId
            if (id == 1) {
                checkBox.setOnCheckedChangeListener(movieTodoView)
            } else if (id == 2) {
                checkBox.setOnCheckedChangeListener(movieDoneView)
            }
        }
    }

    inner class SortDialog(ctx: Context) : _RadioGroup(ctx), OnClickListener {
        private var mode = -1
        private var mode1CheckedId = 895
        private var mode2CheckedId = 895

        init {
            val chosedIcon = resources.getDrawable(R.drawable.movie_sort_check_icon)
            val unChosedIcon = ColorDrawable()
            unChosedIcon.setBounds(0, 0, dip(15), dip(15))
            val colorList = ColorStateList(arrayOf(
                    intArrayOf(android.R.attr.state_checked), intArrayOf()
            ), intArrayOf(Color.parseColor("#10181e"), Color.parseColor("#496069")))

            orientation = LinearLayout.VERTICAL
            backgroundColor = Color.parseColor("#b0000000")

            val line = ColorDrawable(Color.parseColor("#d2d2d2"))
            val bg = ColorDrawable(Color.parseColor("#f4f4f4"))
            val layers = arrayOf<Drawable>(line, bg)
            val itemBg = LayerDrawable(layers)
            itemBg.setLayerInset(0, 0, 0, 0, 0)
            itemBg.setLayerInset(1, 0, 1, 0, 0)

            radioButton {
                id = 895
                backgroundDrawable = itemBg
                buttonDrawable = unChosedIcon
                textSize = 14f
                setTextColor(colorList)
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dip(8.7f)
                leftPadding = dip(15)
                val stateListDrawable = StateListDrawable()
                stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), chosedIcon)
                stateListDrawable.addState(intArrayOf(), unChosedIcon)
                stateListDrawable.setBounds(0, 0, dip(15), dip(15))
                setCompoundDrawables(stateListDrawable, null, null, null)
                setOnClickListener(this@SortDialog)
            }.lparams(matchParent, dip(44.5f))
            radioButton {
                id = 896
                backgroundDrawable = itemBg
                buttonDrawable = unChosedIcon
                textSize = 14f
                setTextColor(colorList)
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dip(8.7f)
                leftPadding = dip(15)
                val stateListDrawable = StateListDrawable()
                stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), chosedIcon)
                stateListDrawable.addState(intArrayOf(), unChosedIcon)
                stateListDrawable.setBounds(0, 0, dip(15), dip(15))
                setCompoundDrawables(stateListDrawable, null, null, null)
                setOnClickListener(this@SortDialog)
            }.lparams(matchParent, dip(44.5f))
            radioButton {
                id = 897
                backgroundDrawable = itemBg
                buttonDrawable = unChosedIcon
                textSize = 14f
                setTextColor(colorList)
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dip(8.7f)
                leftPadding = dip(15)
                val stateListDrawable = StateListDrawable()
                stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), chosedIcon)
                stateListDrawable.addState(intArrayOf(), unChosedIcon)
                stateListDrawable.setBounds(0, 0, dip(15), dip(15))
                setCompoundDrawables(stateListDrawable, null, null, null)
                setOnClickListener(this@SortDialog)
            }.lparams(matchParent, dip(44.5f))
            radioButton {
                id = 898
                backgroundDrawable = itemBg
                buttonDrawable = unChosedIcon
                textSize = 14f
                setTextColor(colorList)
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dip(8.7f)
                leftPadding = dip(15)
                val stateListDrawable = StateListDrawable()
                stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), chosedIcon)
                stateListDrawable.addState(intArrayOf(), unChosedIcon)
                stateListDrawable.setBounds(0, 0, dip(15), dip(15))
                setCompoundDrawables(stateListDrawable, null, null, null)
                setOnClickListener(this@SortDialog)
            }.lparams(matchParent, dip(44.5f))

            check(895)
            setOnClickListener {
                if (parent != null) {
                    (parent as ViewGroup).removeView(this)
                }
            }

            layoutParams = RelativeLayout.LayoutParams(matchParent, matchParent).apply {
                below(R.id.titleBar)
            }
        }

        override fun onClick(v: View?) {
            v ?: return
            if (parent != null) {
                (parent as ViewGroup).removeView(this)
            }
            if (mode == 1) {
                mode1CheckedId = v.id
                movieTodoView.onSortChanged(mode1CheckedId - 894)
            } else {
                mode2CheckedId = v.id
                movieDoneView.onSortChanged(mode2CheckedId - 894)
            }
        }

        fun setMode(mode2: Int) {
            if (mode2 != mode) {
                mode = mode2
                (getChildAt(0) as RadioButton).text = if (mode == 1) "最近添加" else "观影时间"
                (getChildAt(1) as RadioButton).text = "上映日期"
                (getChildAt(2) as RadioButton).text = if (mode == 1) "豆瓣评分" else "我的评分"
                (getChildAt(3) as RadioButton).text = "电影名称"
                if (mode == 1) {
                    check(mode1CheckedId)
                } else {
                    check(mode2CheckedId)
                }
            }
        }
    }
}