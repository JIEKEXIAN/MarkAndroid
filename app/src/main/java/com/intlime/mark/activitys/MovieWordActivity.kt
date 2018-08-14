package com.intlime.mark.activitys

import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.application.Session
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick
import java.util.*

/**
 * Created by wtuadn on 16/04/15.
 */
class MovieWordActivity : BaseActivity() {
    private lateinit var editText: EditText
    private var wordList: ArrayList<String>? = null
    private lateinit var wordsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordList = intent.getStringArrayListExtra("list")
        if (wordList == null) {
            finish()
            return
        }
        if (savedInstanceState != null) {
            intent.putExtra("word", savedInstanceState.get("word") as String)
        }
        applyUI()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString("word", editText.text.toString())
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener {
                    finish()
                }
                title = "台词"
                menu.add("完成").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    Session.map.put(MovieCardShareActivity.WORD, editText.text.toString())
                    finish()
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            scrollView {
                lparams(matchParent, matchParent)
                backgroundColor = resources.getColor(R.color.bg)
                isFillViewport = true
                wordsLayout = verticalLayout {
                    editText = editText {
                        setText(intent.getStringExtra("word"))
                        setSelection(length())
                        backgroundResource = R.drawable.bg_edit_stroke_white
                        padding = dip(15)
                        gravity = Gravity.TOP or Gravity.LEFT
                        setOnTouchListener { view, event ->
                            view.parent.requestDisallowInterceptTouchEvent(true)
                            if(event.action == MotionEvent.ACTION_UP){
                                view.parent.requestDisallowInterceptTouchEvent(false)
                            }
                            return@setOnTouchListener  false
                        }
                        try {
                            val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                            f.isAccessible = true
                            f.set(this, R.drawable.edittext_cursor)
                        } catch (ignored: Exception) {
                        }
                    }.lparams(matchParent, dip(135)) {
                        topMargin = dip(12)
                        bottomMargin = dip(30)
                        horizontalMargin = dip(12)
                    }
                    for (i in 0..wordList!!.size - 1) {
                        val w = wordList!![i]
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            imageView {
                                var resId = R.drawable.movie_word_uncheck
                                if (w.equals(editText.text.toString())) resId = R.drawable.movie_word_checked
                                imageResource = resId
                            }.lparams {
                                margin = dip(12)
                            }
                            textView {
                                text = w
                                textSize = 14f
                                textColor = resources.getColor(R.color.a_main_text_color)
                                topPadding = dip(10)
                                bottomPadding = dip(10)
                                rightPadding = dip(12)
                                onClick {
                                    editText.setText(w)
                                    editText.setSelection(editText.length())
                                    for (j in 0..wordsLayout.childCount - 1) {
                                        val c = wordsLayout.getChildAt(j)
                                        if (c is LinearLayout) {
                                            (c.getChildAt(0) as ImageView).imageResource = R.drawable.movie_word_uncheck
                                        }
                                    }
                                    ((parent as LinearLayout).getChildAt(0) as ImageView).imageResource = R.drawable.movie_word_checked
                                }
                            }.lparams(matchParent, wrapContent)
                        }.lparams(matchParent, wrapContent)
                        if (i != wordList!!.size - 1) {
                            view {
                                backgroundColor = Color.parseColor("#ededed")
                            }.lparams(matchParent, dip(1)) {
                                leftMargin = dip(12)
                            }
                        }
                    }
                }
            }
        }
    }
}