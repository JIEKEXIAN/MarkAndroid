package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by wtuadn on 16/04/26.
 */
data class SingleAccessBean(
        var single_id: Int = 0,
        var movie_id: Int = 0,
        var update_time:Long = 0
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        single_id = `in`.readInt()
        movie_id = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(single_id)
        dest?.writeInt(movie_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { SingleAccessBean(it) }
    }
}