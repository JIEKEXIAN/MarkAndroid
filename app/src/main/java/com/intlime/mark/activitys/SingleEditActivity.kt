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
import com.intlime.mark.application.WWindowManager
import com.intlime.mark.bean.SingleBean
import com.intlime.mark.network.NetManager
import com.intlime.mark.network.NetRequestCallBack
import com.intlime.mark.tools.DialogTool
import com.intlime.mark.tools.db.MovieSingleDbManager
import com.intlime.mark.view.widget.ClearEditText
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.appcompat.v7.onMenuItemClick

/**
 * Created by wtuadn on 16/04/26.
 */
class SingleEditActivity : BaseActivity() {
    companion object {
        val EDIT_NAME_ACTION = "EDIT_NAME_ACTION"
    }

    private lateinit var edit: EditText
    private var bean: SingleBean? = null
    private var type = 0 //1为新建，2为修改

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bean = intent.getParcelableExtra(BEAN)
        type = intent.getIntExtra("type", 0)
        if (type <= 0) {
            finish()
            return
        } else if (type == 2 && bean == null) {
            finish()
            return
        }
        applyUI()
    }

    private fun applyUI() {
        rootView = verticalLayout {
            lparams(matchParent, matchParent)
            backgroundColor = resources.getColor(R.color.bg)
            toolbar = include<Toolbar>(R.layout.toolbar_layout) {
                navigationIconResource = R.drawable.back_icon
                setNavigationOnClickListener { finish() }
                title = if (type == 1) "新建影单" else "编辑影单名称"
                menu.add("完成").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                onMenuItemClick {
                    if (type == 1) {
                        val callback = object : NetRequestCallBack() {
                            override fun onDefault() {
                                DialogTool.dismissWaitDialog()
                            }

                            override fun onSuccess(result: ArrayMap<*, *>) {
                                MovieSingleDbManager.insertSingles(SingleBean(result["id"] as Int, edit.text.toString()))
                                sendBroadcast(Intent(RELOAD_SINGLE_ACTION))
                                finish()
                            }
                        }
                        DialogTool.showWaitDialog("新建中", DialogTool.CANCEL_ON_BACK, callback)
                        NetManager.getInstance().newSingle(edit.text.toString(), callback)
                    } else {
                        val callback = object : NetRequestCallBack() {
                            override fun onDefault() {
                                DialogTool.dismissWaitDialog()
                            }

                            override fun onSuccess(result: ArrayMap<*, *>?) {
                                bean!!.name = edit.text.toString()
                                MovieSingleDbManager.insertSingles(bean!!)
                                sendBroadcast(Intent(RELOAD_SINGLE_ACTION))
                                val i = Intent(EDIT_NAME_ACTION)
                                i.putExtra(BEAN, bean!!)
                                sendBroadcast(i)
                                finish()
                            }
                        }
                        DialogTool.showWaitDialog("修改中", DialogTool.CANCEL_ON_BACK, callback)
                        NetManager.getInstance().changeSingleName(bean!!.id, edit.text.toString(), callback)
                    }
                    return@onMenuItemClick true
                }
            }.lparams(matchParent, dip(49))
            edit = ClearEditText(context).lparams(matchParent, dip(50))
            edit.setBackgroundResource(R.drawable.movie_search_edit_bg)
            edit.horizontalPadding = dip(16)
            edit.setText(bean?.name)
            edit.textSize = 15f
            edit.textColor = resources.getColor(R.color.a_main_text_color)
            edit.hint = "影单标题"
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