package com.gk.news_pro.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties // Quan trọng để Firebase bỏ qua các trường không khớp
data class Comment(
    @PropertyName("commentId") // Tên trường trong Firebase
    var commentId: String = "", // ID duy nhất của bình luận, sẽ được tạo bởi Firebase

    @PropertyName("articleId")
    var articleId: String = "", // ID của bài báo mà bình luận này thuộc về

    @PropertyName("userId")
    var userId: String = "",    // UID của người dùng đăng bình luận

    @PropertyName("username")
    var username: String = "",  // Tên hiển thị của người dùng

    @PropertyName("avatarUrl")
    var avatarUrl: String? = null, // URL avatar của người dùng (tùy chọn)

    @PropertyName("text")
    var text: String = "",      // Nội dung bình luận

    @PropertyName("timestamp")
    var timestamp: Long = 0L    // Thời gian đăng bình luận (Unix timestamp)
) {
    // Constructor không tham số cần thiết cho Firebase deserialization
    constructor() : this("", "", "", "", null, "", 0L)
}
