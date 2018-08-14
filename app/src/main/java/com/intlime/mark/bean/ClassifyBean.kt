package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by root on 16-2-29.
 */
data class ClassifyBean(var id: Int = 0,
                        var imgUrl: String? = null,
                        var name: String? = null,
                        var type: Int = 2, //1影单  2影单专题分类
                        var isGroup: Boolean = false,
                        var list: List<ClassifyBean>? = null
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        id = `in`.readInt()
        imgUrl = `in`.readString()
        name = `in`.readString()
        type = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(imgUrl)
        dest.writeString(name)
        dest.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { ClassifyBean(it) }
    }
}
