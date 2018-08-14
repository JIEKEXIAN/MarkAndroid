package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by root on 16-2-22.
 */
data class MovieListBean(
        var id: Int = 0,
        var name: String? = null, //名称
        var img_url: String? = null,
        var liked: Int = 0,
        var likes: Int = 0,
        var publish_time: Int = 0,
        var timeToShow: String? = null,
        var type: Int = 1, //1影单  2影单专题分类
        var isNew: Int = 0,
        var shares: Int = 0,
        var htmlCode: String? = null,
        var comments: Int = 0,
        var cat_name: String? = null
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        id = `in`.readInt()
        name = `in`.readString()
        img_url = `in`.readString()
        liked = `in`.readInt()
        likes = `in`.readInt()
        publish_time = `in`.readInt()
        timeToShow = `in`.readString()
        type = `in`.readInt()
        isNew = `in`.readInt()
        shares = `in`.readInt()
        htmlCode = `in`.readString()
        comments = `in`.readInt()
        cat_name = `in`.readString()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeString(name)
        dest?.writeString(img_url)
        dest?.writeInt(liked)
        dest?.writeInt(likes)
        dest?.writeInt(publish_time)
        dest?.writeString(timeToShow)
        dest?.writeInt(type)
        dest?.writeInt(isNew)
        dest?.writeInt(shares)
        dest?.writeString(htmlCode)
        dest?.writeInt(comments)
        dest?.writeString(cat_name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { MovieListBean(it) }
    }
}
