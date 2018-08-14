package com.intlime.mark.activitys

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import com.intlime.mark.R
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.ToastTool
import com.intlime.mark.view.widget.ClearEditText
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick

/**
 * Created by wtuadn on 16/04/23.
 */
class ChangeNicknameActivity : BaseActivity() {
    private lateinit var edit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUI()
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            backgroundColor = resources.getColor(R.color.bg)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = "修改昵称"
                menu.add("完成").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    if (edit.text.isNullOrEmpty()) return@onMenuItemClick true
                    val callback = object : NetRequestCallBack() {
                        override fun onDefault() {
                            DialogTool.dismissWaitDialog()
                        }

                        override fun onSuccess(result: ArrayMap<*, *>?) {
                            ToastTool.show("修改成功")
                            SettingManager.getInstance().nickname = edit.text.toString().trim()
                            sendBroadcast(Intent(RELOAD_USER_CENTER_ACTION))
                            finish()
                        }
                    }
                    DialogTool.showWaitDialog("修改中", DialogTool.CANCEL_ON_BACK, callback)
                    NetManager.getInstance().changeNickname(edit.text.toString().trim(), callback)
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            edit = ClearEditText(context).lparams(matchParent, dip(50)) {
                topMargin = dip(20)
                horizontalMargin = dip(20)
            }
            edit.setBackgroundResource(R.drawable.bg_edit_stroke_white)
            edit.compoundDrawablePadding = dip(7)
            edit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.nickname_edit_icon, 0, 0, 0)
            edit.horizontalPadding = dip(12)
            edit.setText(SettingManager.getInstance().nickname)
            edit.textSize = 14f
            edit.textColor = resources.getColor(R.color.a_main_text_color)
            edit.hint = "输入昵称"
            edit.hintTextColor = Color.parseColor("#818c91")
            edit.singleLine = true
            try {
                val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                f.isAccessible = true
                f.set(edit, R.drawable.edittext_cursor)
            } catch (ignored: Exception) {
            }
            addView(edit)
            WWindowManager.getInstance().showSoftInput(edit)
            edit.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    edit.setSelection(edit.length())
                    edit.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }
    }
}