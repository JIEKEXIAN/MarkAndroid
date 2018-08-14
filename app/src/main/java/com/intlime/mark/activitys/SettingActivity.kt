package com.intlime.mark.activitys

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.intlime.mark.R
import com.intlime.mark.application.SettingManager
import com.intlime.mark.application.ThreadManager
import com.intlime.mark.tools.FileTool
import com.intlime.mark.tools.ToastTool
import kotlinx.android.synthetic.main.activity_setting_layout.*

/**
 * Created by root on 16-1-22.
 */
class SettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_layout)
    }

    override fun initToolbar() {
        super.initToolbar()
        toolbar.title = "设置"
        toolbar.setNavigationIcon(R.drawable.back_icon)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initOther() {
        notify_switch.isChecked = SettingManager.getInstance().notifySwitch
        notify_switch.setOnCheckedChangeListener { compoundButton, bool ->
            SettingManager.getInstance().notifySwitch = bool
        }
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.clear_cache -> {
                try {
                    Glide.get(this).clearMemory()
                    cleanDiskCache()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun cleanDiskCache() {
        ThreadManager.getInstance().submit {
            try {
                val cacheDir = cacheDir
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    val files = cacheDir.listFiles()
                    if (files != null) {
                        for (file in files){
                            if(file.isDirectory){
                                if(file.name.equals(DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)){
                                    val glideCaches = file.listFiles()
                                    if(glideCaches!=null){
                                        for (cache in glideCaches){
                                            if(!cache.isDirectory){
                                                cache.delete()
                                            }
                                        }
                                    }
                                }else{
                                    FileTool.cleanDirectory(file)
                                    file.delete()
                                }
                            }else{
                                file.delete()
                            }
                        }
                    }
                }
                ToastTool.show("清除成功")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
