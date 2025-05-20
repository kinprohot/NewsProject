

import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.gk.news_pro.data.model.News
import com.gk.news_pro.data.model.Comment
import com.gk.news_pro.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await


class FirebaseUserService {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val usersRef: DatabaseReference = db.child("users")
    private val newsRef: DatabaseReference = db.child("news")
    private val TAG = "FirebaseUserService"

    suspend fun addUser(uid: String, user: User) {
        try {
            // The 'user' object now includes 'admin', it will be saved.
            usersRef.child(uid).setValue(user).await()
            Log.d(TAG, "addUser: Successfully saved user $uid to Realtime Database. admin: ${user.admin}")
        } catch (e: Exception) {
            Log.e(TAG, "addUser: Failed to save user $uid: ${e.message}", e)
            throw e
        }
    }
    suspend fun updateUserLanguage(uid: String, languageCode: String) { // Đổi tên hàm cho nhất quán
        try {
            // Sử dụng tên trường mới là "language" khi ghi vào Firebase
            usersRef.child(uid).child("language").setValue(languageCode).await()
            Log.d(TAG, "updateUserLanguage: Successfully updated language for user $uid to $languageCode")
        } catch (e: Exception) {
            Log.e(TAG, "updateUserLanguage: Failed to update language for user $uid: ${e.message}", e)
            throw e
        }
    }
    suspend fun getUser(uid: String): User? {
        try {
            val snapshot = usersRef.child(uid).get().await()
            val user = snapshot.getValue(User::class.java)?.copy(id = snapshot.key ?: uid)
            Log.d(TAG, "getUser: Retrieved user $uid: ${user?.email}, admin: ${user?.admin}")
            return user
        } catch (e: Exception) {
            Log.e(TAG, "getUser: Failed to retrieve user $uid: ${e.message}", e)
            return null
        }
    }

    suspend fun getAllUsers(): List<User> {
        try {
            val snapshot = usersRef.get().await()
            val userList = mutableListOf<User>()
            snapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(User::class.java)?.let { user ->
                    // Add the UID to the user object if 'id' field is meant for it
                    userList.add(user.copy(id = dataSnapshot.key ?: ""))
                }
            }
            Log.d(TAG, "getAllUsers: Retrieved ${userList.size} users")
            return userList
        } catch (e: Exception) {
            Log.e(TAG, "getAllUsers: Failed to retrieve users: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun updateUser(uid: String, username: String?, email: String?, avatar: String?, password: String?, language: String? = null,admin: Boolean? ) {
        try {
            val updates = mutableMapOf<String, Any>()
            username?.let { updates["username"] = it }
            email?.let { updates["email"] = it }
            avatar?.let { updates["avatar"] = it }
            password?.let { updates["password"] = it }
            language?.let { updates["language"] = it }
            admin?.let { updates["admin"] = it }
            if (updates.isNotEmpty()) {
                usersRef.child(uid).updateChildren(updates).await()
                Log.d(TAG, "updateUser: Successfully updated user $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateUser: Failed to update user $uid: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteUser(uid: String) {
        try {
            usersRef.child(uid).removeValue().await()
            Log.d(TAG, "deleteUser: Successfully deleted user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "deleteUser: Failed to delete user $uid: ${e.message}", e)
            throw e
        }
    }

    suspend fun addFavoriteNews(uid: String, news: News) {
        try {
            usersRef.child(uid)
                .child("favoriteNews")
                .child(news.article_id)
                .setValue(news)
                .await()
            Log.d(TAG, "addFavoriteNews: Successfully added news ${news.article_id} for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "addFavoriteNews: Failed to add news for user $uid: ${e.message}", e)
            throw e
        }
    }

    suspend fun getFavoriteNews(uid: String): List<News> {
        try {
            val snapshot = usersRef.child(uid).child("favoriteNews").get().await()
            val newsList = mutableListOf<News>()
            snapshot.children.forEach { child ->
                try {
                    val news = child.getValue(News::class.java)
                    if (news != null) {
                        newsList.add(news)
                    } else {
                        Log.w(TAG, "getFavoriteNews: Failed to parse news for key ${child.key}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getFavoriteNews: Error parsing news for key ${child.key}: ${e.message}", e)
                }
            }
            Log.d(TAG, "getFavoriteNews: Retrieved ${newsList.size} favorite news for user $uid")
            return newsList
        } catch (e: Exception) {
            Log.e(TAG, "getFavoriteNews: Failed to retrieve favorite news for user $uid: ${e.message}", e)
            return emptyList()
        }
    }
    suspend fun removeFavoriteNews(uid: String, newsId: String) {
        try {
            usersRef.child(uid)
                .child("favoriteNews")
                .child(newsId)
                .removeValue()
                .await()
            Log.d(TAG, "removeFavoriteNews: Successfully removed news $newsId for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "removeFavoriteNews: Failed to remove news $newsId for user $uid: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateFavoriteTopics(uid: String, topics: Map<String, Int>) {
        try {
            usersRef.child(uid).child("favoriteTopics").setValue(topics).await()
            Log.d(TAG, "updateFavoriteTopics: Successfully updated topics for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "updateFavoriteTopics: Failed to update topics for user $uid: ${e.message}", e)
            throw e
        }
    }
    suspend fun addCommentToNews(articleId: String, comment: Comment): String {
        return try {
            val commentRef = newsRef.child(articleId).child("comments").push() // Tạo ID mới cho comment
            val commentId = commentRef.key ?: throw IllegalStateException("Không thể tạo comment ID")
            val commentWithId = comment.copy(commentId = commentId, articleId = articleId) // Gán ID vào đối tượng comment
            commentRef.setValue(commentWithId).await()
            Log.d(TAG, "addCommentToNews: Successfully added comment $commentId to article $articleId")
            commentId
        } catch (e: Exception) {
            Log.e(TAG, "addCommentToNews: Failed to add comment to article $articleId: ${e.message}", e)
            throw e // Ném lại lỗi để tầng trên xử lý
        }
    }

    fun getCommentsForNews(articleId: String): Flow<List<Comment>> = callbackFlow {
        val commentsPath = newsRef.child(articleId).child("comments")
        Log.d(TAG, "getCommentsForNews: Listening to $commentsPath")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentsList = mutableListOf<Comment>()
                if (snapshot.exists()) {
                    for (commentSnapshot in snapshot.children) {
                        try {
                            val comment = commentSnapshot.getValue(Comment::class.java)
                            comment?.let {
                                commentsList.add(it.copy(commentId = commentSnapshot.key ?: it.commentId)) // Đảm bảo commentId đúng
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "getCommentsForNews: Error parsing comment ${commentSnapshot.key}: ${e.message}", e)
                        }
                    }
                }
                Log.d(TAG, "getCommentsForNews: Retrieved ${commentsList.size} comments for article $articleId")
                trySend(commentsList.sortedByDescending { it.timestamp }) // Sắp xếp theo thời gian mới nhất lên đầu
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getCommentsForNews: Listener cancelled for article $articleId: ${error.message}", error.toException())
                close(error.toException()) // Đóng flow với lỗi
            }
        }
        commentsPath.addValueEventListener(listener)
        // Khi flow bị hủy (ví dụ ViewModel bị destroy), gỡ bỏ listener
        awaitClose {
            Log.d(TAG, "getCommentsForNews: Removing listener from $commentsPath")
            commentsPath.removeEventListener(listener)
        }
    }

    suspend fun deleteComment(articleId: String, commentId: String) {
        try {
            newsRef.child(articleId).child("comments").child(commentId).removeValue().await()
            Log.d(TAG, "deleteComment: Successfully deleted comment $commentId from article $articleId")
        } catch (e: Exception) {
            Log.e(TAG, "deleteComment: Failed to delete comment $commentId: ${e.message}", e)
            throw e
        }
    }
}