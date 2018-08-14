package com.intlime.mark.bean

/**
 * Created by wtuadn on 16/05/03.
 */
data class CommentBean(
        var id: Int = 0,
        var uid: Int = 0,
        var singleId: Int = 0,
        var imgUrl: String = "",
        var content: String = "",
        var name: String = "",
        var likes: Int = 0,
        var liked: Int = 0,
        var preName: String = "",
        var preContent: String = "",
        var timestamp: Int = 0,
        var type: Int = 0,
        var localType: Int = 0 // 0为普通评论，1为特殊评论类型，此时name是类型名称，2为已经删除
) {
}