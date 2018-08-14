package com.intlime.mark.tools.glide

import android.util.Log
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.EngineKey
import com.bumptech.glide.load.engine.OriginalKey
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.DiskCacheWriteLocker
import com.bumptech.glide.util.LruCache
import com.bumptech.glide.util.Util
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Created by wtuadn on 16-7-27.
 */
class YDiskCacheWrapper(val directory: File, val maxSize: Int) : DiskCache {
    private val TAG = "YDiskCacheWrapper"

    private val APP_VERSION = 1
    private val VALUE_COUNT = 1

    private val writeLocker = DiskCacheWriteLocker()
    private val safeKeyGenerator = SafeKeyGenerator()
    private var diskLruCache: DiskLruCache? = null
    private var prefixCaches: HashMap<String, DiskLruCache>? = null

    @Synchronized @Throws(IOException::class)
    private fun getDiskCache(key: Key? = null): DiskLruCache {
        var signature: Key? = null
        if (key is EngineKey) {
            signature = key.signature
        } else if (key is OriginalKey) {
            signature = key.signature
        }
        if (signature is PrefixKey) {
            if (prefixCaches == null) prefixCaches = HashMap()
            val keyName = signature.javaClass.simpleName
            var prefixCache = prefixCaches!![keyName]
            if (prefixCache == null) {
                val subDir = File(directory, signature.subDir)

                if (!subDir.exists()) {
                    subDir.mkdirs()
                }
                if (subDir.exists() && subDir.isDirectory) {
                    prefixCache = DiskLruCache.open(subDir, APP_VERSION, VALUE_COUNT, signature.maxSize.toLong())
                    prefixCaches!![keyName] = prefixCache
                }
            }
            if (prefixCache != null) {
                return prefixCache
            }
        }
        if (diskLruCache == null) {
            diskLruCache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize.toLong())
        }
        return diskLruCache!!
    }

    @Synchronized private fun resetDiskCache() {
        diskLruCache = null
    }

    override fun get(key: Key): File? {
        val safeKey = safeKeyGenerator.getSafeKey(key)
        var result: File? = null
        try {
            //It is possible that the there will be a put in between these two gets. If so that shouldn't be a problem
            //because we will always put the same value at the same key so our input streams will still represent
            //the same data
            val value = getDiskCache(key).get(safeKey)
            if (value != null) {
                result = value.getFile(0)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Unable to get from disk cache", e)
        }

        return result
    }

    override fun put(key: Key, writer: DiskCache.Writer) {
        val safeKey = safeKeyGenerator.getSafeKey(key)
        writeLocker.acquire(key)
        try {
            val editor = getDiskCache(key).edit(safeKey)
            // Editor will be null if there are two concurrent puts. In the worst case we will just silently fail.
            if (editor != null) {
                try {
                    val file = editor.getFile(0)
                    if (writer.write(file)) {
                        editor.commit()
                    }
                } finally {
                    editor.abortUnlessCommitted()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Unable to put to disk cache", e)
        } finally {
            writeLocker.release(key)
        }
    }

    override fun delete(key: Key) {
        val safeKey = safeKeyGenerator.getSafeKey(key)
        try {
            getDiskCache(key).remove(safeKey)
        } catch (e: IOException) {
            Log.w(TAG, "Unable to delete from disk cache", e)
        }

    }

    @Synchronized override fun clear() {
        try {
            getDiskCache().delete()
            resetDiskCache()
        } catch (e: IOException) {
            Log.w(TAG, "Unable to clear disk cache", e)
        }

    }

    private inner class SafeKeyGenerator {
        private val loadIdToSafeHash = LruCache<Key, String>(1000)

        fun getSafeKey(key: Key): String {
            var safeKey: String? = null
            synchronized(loadIdToSafeHash) {
                safeKey = loadIdToSafeHash.get(key)
            }
            if (safeKey == null) {
                try {
                    val messageDigest = MessageDigest.getInstance("SHA-256")
                    key.updateDiskCacheKey(messageDigest)
                    safeKey = Util.sha256BytesToHex(messageDigest.digest())
                    var signature: Key? = null
                    if (key is EngineKey) {
                        signature = key.signature
                    } else if (key is OriginalKey) {
                        signature = key.signature
                    }
                    if (signature is PrefixKey) {
                        safeKey = signature.prefix + safeKey
                    }
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                }

                synchronized(loadIdToSafeHash) {
                    loadIdToSafeHash.put(key, safeKey)
                }
            }
            return safeKey ?: ""
        }
    }
}