package com.intlime.mark.tools.glide

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.cache.DiskCache
import com.intlime.mark.application.AppEngine
import com.intlime.mark.bean.MovieListBean
import java.io.File
import java.io.UnsupportedEncodingException
import java.security.MessageDigest

/**
 * Created by wtuadn on 16-7-27.
 */
class BannerKey(val bean:MovieListBean) : Key, PrefixKey {
    companion object{
        fun getCachedFiles():Array<File>?{
            var cacheDir: File? = AppEngine.getContext().cacheDir
            if (cacheDir != null) {
                cacheDir = File(cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    val subDir = File(cacheDir, BannerKey::class.java.simpleName)
                    if (subDir.exists() && subDir.isDirectory) {
                        val files = subDir.listFiles()
                        if (files != null) {
                            return files
                        }
                    }
                }
            }
            return null
        }
    }

    init {
        if (bean.img_url == null) {
            bean.img_url = ""
        }
    }

    @Throws(UnsupportedEncodingException::class)
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(bean.img_url!!.toByteArray())
    }

    override fun equals(o: Any?): Boolean {
        if (o is BannerKey) {
            return o.bean.img_url == bean.img_url
        }
        return false
    }

    override fun hashCode(): Int {
        return bean.img_url!!.hashCode()
    }

    override fun getSubDir(): String {
        return javaClass.simpleName
    }

    override fun getPrefix(): String {
        return "${bean.id}_"
    }

    override fun getMaxSize(): Int {
        return 5 * 1024 * 1024
    }
}