package com.intlime.mark.view.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.intlime.mark.R
import com.intlime.mark.activitys.ChangeNicknameActivity
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ToastTool
import com.rockerhieu.emojicon.EmojiconEditText
import com.wtuadn.pressable.PressableUtils
import com.wtuadn.pressable.pressableImageView
import com.wtuadn.pressable.pressableTextView
import org.jetbrains.anko.*

/**
 * Created by wtuadn on 16-6-20.
 */
class CommentInputView(context: Context, var rootViewGroup: ViewGroup, mHint: String = "发表评论") : _RelativeLayout(context) {
    private val textLimit = 200
    var onCommentListener: OnCommentListener? = null
    private lateinit var emojiIcon: ImageView
    private lateinit var editText: EditText
    private var emojiPan: EmojiPan? = null

    fun setHint(hint: String) {
        editText.hint = hint
    }

    fun clear() {
        editText.setText("")
        editText.hint = "发表评论"
        hideEmoji()
    }

    fun showKeyboard() {
        WWindowManager.getInstance().showSoftInput(editText)
        hideEmoji()
    }

    init {
        apply {
            lparams(matchParent, wrapContent) { alignParentBottom() }
            backgroundColor = Color.parseColor("#f6f8f9")
            emojiIcon = pressableImageView {
                id = 1733
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                imageResource = R.drawable.open_emoji_icon
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.black_pressed_color))
                onClick {
                    handleEmojiPan(context)
                }
            }.lparams(dip(46), dip(46)) {
                centerVertically()
            }

            editText = EmojiconEditText(context).apply {
                textSize = 14f
                textColor = Color.parseColor("#0e0e0e")
                hint = mHint
                hintTextColor = Color.parseColor("#a3b0b5")
                gravity = Gravity.CENTER_VERTICAL
                backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    setCornerRadius(dip(2).toFloat())
                    setStroke(1, Color.parseColor("#e1e1e1"))
                }
                horizontalPadding = dip(11)
                verticalPadding = dip(7)
                minHeight = dip(34.1f)
                maxHeight = dip(65f)
            }.lparams(matchParent, wrapContent) {
                topMargin = dip(8.5f)
                bottomMargin = dip(7.4f)
                centerVertically()
                rightOf(1733)
                leftOf(1734)
            }
            addView(editText)

            pressableTextView {
                id = 1734
                text = "评论"
                textColor = Color.WHITE
                textSize = 14f
                gravity = Gravity.CENTER
                backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.parseColor("#10181e"))
                    setCornerRadius(dip(2).toFloat())
                }
                PressableUtils.setPressableDrawable(this, resources.getColor(R.color.white_pressed_color))
                onClick {
                    if (editText.length() == 0) {
                        ToastTool.show("评论不能为空", Toast.LENGTH_SHORT, 1)
                        return@onClick
                    }
                    if (TextUtils.isEmpty(SettingManager.getInstance().nickname)) {
                        val dialog = DialogTool.getConfirmDialog("Mark暂时不接受无名之辈，写上你的名号才能互动哦！", null, null)
                        dialog.confirm_agree.onClick {
                            dialog.dismiss()
                            WWindowManager.getInstance().currentActivity?.startActivity<ChangeNicknameActivity>()
                        }
                        dialog.show()
                        return@onClick
                    }
                    onCommentListener?.onComment(editText.text.toString())
                }
            }.lparams(dip(67), dip(34.1f)) {
                alignParentRight()
                centerVertically()
                leftMargin = dip(10)
                rightMargin = dip(12)
            }
            view {
                backgroundColor = Color.parseColor("#ebebeb")
            }.lparams(matchParent, dip(1))
        }
    }

    private fun handleEmojiPan(context: Context) {
        if (emojiPan == null) {
            emojiPan = EmojiPan(context, editText)
            emojiPan!!.id = 1730
        }
        if (emojiPan!!.parent == null) {
            WWindowManager.getInstance().hideInput()
            ThreadManager.getInstance().postDelayed({
                emojiIcon.imageResource = R.drawable.open_keyboard_icon
                rootViewGroup.addView(emojiPan)
                if (rootViewGroup is RelativeLayout) {
                    emojiPan!!.layoutParams.width = matchParent
                    emojiPan!!.layoutParams.height = dip(230)
                    (emojiPan!!.layoutParams as LayoutParams).alignParentBottom()
                    lparams(matchParent, dip(50)) { above(emojiPan!!) }
                    (layoutParams as LayoutParams).above(emojiPan!!)
                    editText.onClick {
                        hideEmoji()
                    }
                    editText.setOnFocusChangeListener { view, b ->
                        if (b) {
                            hideEmoji()
                        }
                    }
                }
            }, 200)
        } else {
            hideEmoji()
            WWindowManager.getInstance().showSoftInput(editText)
        }
    }

    private fun hideEmoji() {
        emojiIcon.imageResource = R.drawable.open_emoji_icon
        rootViewGroup.removeView(emojiPan)
        lparams(matchParent, dip(50)) { alignParentBottom() }
        editText.setOnClickListener(null)
        editText.setOnFocusChangeListener(null)
    }

    interface OnCommentListener {
        fun onComment(comment: String)
    }
}