package com.intlime.mark.tools.glide

import com.bumptech.glide.load.Key
import java.io.UnsupportedEncodingException
import java.security.MessageDigest

/**
 * Created by wtuadn on 16-7-27.
 */
class PinnedKey(var imgUrl: String?) : Key, PrefixKey {
    init {
        if (imgUrl == null) {
            imgUrl = ""
        }
    }

    @Throws(UnsupportedEncodingException::class)
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(imgUrl!!.toByteArray())
    }

    override fun equals(o: Any?): Boolean {
        if (o is PinnedKey) {
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
        return ""
    }

    override fun getMaxSize(): Int {
        return 20 * 1024 * 1024
    }
}