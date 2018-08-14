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
class MovieListKey(val bean: MovieListBean, var imgUrl: String?) : Key, PrefixKey {
    companion object {
        fun getCachedFiles(): Array<File>? {
            var cacheDir: File? = AppEngine.getContext().cacheDir
            if (cacheDir != null) {
                cacheDir = File(cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    val subDir = File(cacheDir, MovieListKey::class.java.simpleName)
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
        if (imgUrl == null) {
            imgUrl = ""
        }
    }

    @Throws(UnsupportedEncodingException::class)
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(bean.img_url!!.toByteArray())
    }

    override fun equals(o: Any?): Boolean {
        if (o is MovieListKey) {
            return o.imgUrl == imgUrl
        }
        return false
    }

    override fun hashCode(): Int {
        return imgUrl!!.hashCode()
    }

    override fun getSubDir(): String {
        return javaClass.simpleName
    }

    override fun getPrefix(): String {
        return "${bean.publish_time}_${bean.id}_"
    }

    override fun getMaxSize(): Int {
        return 15 * 1024 * 1024
    }
}