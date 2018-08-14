package com.intlime.mark.bean

/**
 * Created by wtuadn on 16/05/03.
 */
data class WriterBean(
        var imgUrl: String = "",
        var nickname: String = "",
        var saying: String = "",
        var singleLikes: Int = 0,
        var singleCount: Int = 0
) {
}