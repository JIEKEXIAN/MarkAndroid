package com.intlime.mark.tools.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.module.GlideModule;

import java.io.File;

/**
 * Created by wtu on 15-8-24.
 */
public class GlideConfiguration implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Apply options to the builder here.
//        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            cacheDir = new File(cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                builder.setDiskCache(new YDiskCacheWrapper(cacheDir, 60 * 1024 * 1024));//40M磁盘缓存
            }
        }
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        // register ModelLoaders here.
    }
}