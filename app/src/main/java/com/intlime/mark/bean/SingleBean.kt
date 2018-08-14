package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by wtuadn on 16/04/26.
 */
data class SingleBean(
        var id: Int = 0,
        var name: String? = null
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        id = `in`.readInt()
        name = `in`.readString()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { SingleBean(it) }
    }
}