package com.intlime.mark.bean

import android.os.Parcel
import android.os.Parcelable
import android.support.v4.util.ArrayMap

/**
 * Created by root on 15-11-3.
 */
data class MovieBean(
        var id: Int = 0,
        var name: String? = null, //名称
        var done: Int = 0,
        var update_time: Long = 0,
        var cursorPosition: Int = -1, //cursor中的位置
        var db_num: String? = null,
        var english_name: String? = null, //名称
        var image: String? = null, //海报
        var directors: String? = null, //导演
        var casts: String? = null, //主演
        var pubdate: String? = null, //上映日期
        var pubdateTimestamp: Long = 0, //上映日期
        var countries: String? = null, //制片国家/地区
        var summary: String? = null, //简介
        var db_rating: Float = 0.0f, //豆瓣评分
        var movieType: String? = null, //电影类型
        var urls: ArrayMap<String, String>? = null, //豆瓣链接
        var year: String? = null, //年份
        var duration: String? = null,
        var stagePhoto: List<String>? = null,
        var mark_rating: Float = 0.0f, //用户打的分
        var scriptWriter: String? = null,
        var watchTime: Long = 0,
        var note: String? = null,
        var trailer: String? = null,
        var groupUpdateTime: String? = null, //分组显示用的更新时间
        var groupWatchTime: String? = null, //分组显示用的观影时间
        var groupPubdate: String? = null, //分组显示用的上映时间
        var pinyin: String? = null //全拼
) : Parcelable {

    protected constructor(`in`: Parcel) : this() {
        id = `in`.readInt()
        name = `in`.readString()
        done = `in`.readInt()
        update_time = `in`.readLong()
        cursorPosition = `in`.readInt()
        db_num = `in`.readString()
        english_name = `in`.readString()
        image = `in`.readString()
        directors = `in`.readString()
        casts = `in`.readString()
        pubdate = `in`.readString()
        pubdateTimestamp = `in`.readLong()
        countries = `in`.readString()
        summary = `in`.readString()
        db_rating = `in`.readFloat()
        movieType = `in`.readString()
        year = `in`.readString()
        duration = `in`.readString()
        stagePhoto = `in`.createStringArrayList()
        mark_rating = `in`.readFloat()
        scriptWriter = `in`.readString()
        watchTime = `in`.readLong()
        note = `in`.readString()
        trailer = `in`.readString()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeString(name)
        dest?.writeInt(done)
        dest?.writeLong(update_time)
        dest?.writeInt(cursorPosition)
        dest?.writeString(db_num)
        dest?.writeString(english_name)
        dest?.writeString(image)
        dest?.writeString(directors)
        dest?.writeString(casts)
        dest?.writeString(pubdate)
        dest?.writeLong(pubdateTimestamp)
        dest?.writeString(countries)
        dest?.writeString(summary)
        dest?.writeFloat(db_rating)
        dest?.writeString(movieType)
        dest?.writeString(year)
        dest?.writeString(duration)
        dest?.writeStringList(stagePhoto)
        dest?.writeFloat(mark_rating)
        dest?.writeString(scriptWriter)
        dest?.writeLong(watchTime)
        dest?.writeString(note)
        dest?.writeString(trailer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { MovieBean(it) }
    }
}
