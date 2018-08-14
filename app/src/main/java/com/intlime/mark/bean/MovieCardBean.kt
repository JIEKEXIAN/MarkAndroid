package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by wtuadn on 16/05/03.
 */
data class MovieCardBean(
        var id: Int = 0,
        var imgUrl: String = "",
        var content: String = "",
        var name: String = "",
        var db_num: String = "",
        var likes: Int = 0,
        var liked: Int = 0,
        var shares: Int = 0
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        id = `in`.readInt()
        imgUrl = `in`.readString()
        content = `in`.readString()
        name = `in`.readString()
        db_num = `in`.readString()
        likes = `in`.readInt()
        liked = `in`.readInt()
        shares = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeString(imgUrl)
        dest?.writeString(content)
        dest?.writeString(name)
        dest?.writeString(db_num)
        dest?.writeInt(likes)
        dest?.writeInt(liked)
        dest?.writeInt(shares)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { MovieCardBean(it) }
    }
}