package com.gk.news_pro.data.repository

import FirebaseUserService
import android.util.Log
import com.gk.news_pro.data.model.News
import com.gk.news_pro.data.model.User
import com.gk.news_pro.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firebaseService: FirebaseUserService = FirebaseUserService(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "UserRepository"

    init {
        auth.setLanguageCode("vi") // Avoid X-Firebase-Locale warning
    }

    suspend fun createUser(username: String, email: String, password: String, admin: Boolean = false): User? {
        try {
            Log.d(TAG, "createUser: Attempting to create user with email $email, admin: $admin")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Log.d(TAG, "createUser: Authentication successful, UID: ${firebaseUser.uid}")
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    username = username,
                    password = password, // Storing password in DB (consider hashing or not storing if possible)
                    admin = admin
                )
                firebaseService.addUser(firebaseUser.uid, user)
                Log.d(TAG, "createUser: User data saved for ${user.email}")
                return user
            } else {
                Log.e(TAG, "createUser: Firebase user is null")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "createUser: Failed to create user: ${e.message}", e)
            throw Exception("Tạo tài khoản thất bại: ${e.message}")
        }
    }

    suspend fun loginUser(email: String, password: String): User? {
        try {
            Log.d(TAG, "loginUser: Attempting to login with email $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Log.d(TAG, "loginUser: Login successful, UID: ${firebaseUser.uid}")
                return firebaseService.getUser(firebaseUser.uid)
            } else {
                Log.e(TAG, "loginUser: Firebase user is null")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginUser: Failed to login: ${e.message}", e)
            throw Exception("Đăng nhập thất bại: ${e.message}")
        }
    }

    // HOÀN NGUYÊN TÊN HÀM VỀ LẠI updateUser
    // This method updates the CURRENTLY authenticated user's details
    suspend fun updateUser(newUsername: String?, newPassword1: String?, newAvatar: String? = null) {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "updateUser: No user logged in")
            throw Exception("Chưa đăng nhập")
        }
        try {
            Log.d(TAG, "updateUser: Updating profile for current user ${firebaseUser.uid}")

            if (!newPassword1.isNullOrBlank()) {
                firebaseUser.updatePassword(newPassword1).await()
                Log.d(TAG, "updateUser: Current user's Auth password updated")
            }
            // Update DB record.
            firebaseService.updateUser( // Gọi đến firebaseService.updateUser để cập nhật DB
                uid = firebaseUser.uid,
                username = newUsername,
                email = null, // Không thay đổi email Auth từ màn hình profile này
                avatar = newAvatar,
                password = newPassword1, // Cập nhật password trong DB nếu được cung cấp
                admin = null // Không thay đổi quyền admin từ hàm này
            )
            Log.d(TAG, "updateUser: Current user's DB data updated")
        } catch (e: Exception) {
            Log.e(TAG, "updateUser: Failed to update profile: ${e.message}", e)
            throw Exception("Cập nhật hồ sơ thất bại: ${e.message}")
        }
    }


    // Admin function to update ANY user's DB record (not Auth)
    // Tên hàm này là mới, giữ nguyên
    suspend fun adminUpdateUserRecord(
        userIdToUpdate: String,
        username: String?,
        email: String?,
        password: String?,
        admin: Boolean?
    ) {
        try {
            Log.d(TAG, "adminUpdateUserRecord: Admin updating DB record for user $userIdToUpdate")
            firebaseService.updateUser(
                uid = userIdToUpdate,
                username = username,
                email = email,
                avatar = null,
                password = password,
                admin = admin
            )
            Log.d(TAG, "adminUpdateUserRecord: DB record updated for user $userIdToUpdate")
        } catch (e: Exception) {
            Log.e(TAG, "adminUpdateUserRecord: Failed to update DB record for user $userIdToUpdate: ${e.message}", e)
            throw Exception("Admin: Cập nhật dữ liệu người dùng thất bại: ${e.message}")
        }
    }


    suspend fun updateUserLanguage(languageCode: String) {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "updateUserLanguage: No user logged in")
            throw Exception("Chưa đăng nhập để cập nhật ngôn ngữ")
        }
        try {
            firebaseService.updateUserLanguage(firebaseUser.uid, languageCode)
            Log.d(TAG, "updateUserLanguage: Language preference updated for user ${firebaseUser.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "updateUserLanguage: Failed to update language preference: ${e.message}")
            throw e
        }
    }

    // HOÀN NGUYÊN TÊN HÀM VỀ LẠI deleteUser
    // Deletes the CURRENTLY authenticated user (Auth and DB)
    suspend fun deleteUser() {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "deleteUser: No user logged in") // Log message updated
            throw Exception("Chưa đăng nhập")
        }
        try {
            Log.d(TAG, "deleteUser: Deleting current user ${firebaseUser.uid}") // Log message updated
            firebaseService.deleteUser(firebaseUser.uid) // Delete from DB
            firebaseUser.delete().await() // Delete from Auth
            Log.d(TAG, "deleteUser: Current user account deleted") // Log message updated
        } catch (e: Exception) {
            Log.e(TAG, "deleteUser: Failed to delete current user account: ${e.message}", e) // Log message updated
            throw Exception("Xóa tài khoản thất bại: ${e.message}")
        }
    }

    // Admin function to delete ANY user's DB record (not Auth account)
    // Tên hàm này là mới, giữ nguyên
    suspend fun adminDeleteUserRecord(userIdToDelete: String) {
        try {
            Log.d(TAG, "adminDeleteUserRecord: Admin deleting DB record for user $userIdToDelete")
            firebaseService.deleteUser(userIdToDelete)
            Log.d(TAG, "adminDeleteUserRecord: DB record deleted for user $userIdToDelete")
        } catch (e: Exception) {
            Log.e(TAG, "adminDeleteUserRecord: Failed to delete DB record for user $userIdToDelete: ${e.message}", e)
            throw Exception("Admin: Xóa dữ liệu người dùng thất bại: ${e.message}")
        }
    }


    suspend fun getUser(): User? {
        val firebaseUser = auth.currentUser ?: run {
            Log.d(TAG, "getUser: No user logged in, cannot fetch profile.")
            return null
        }
        return firebaseService.getUser(firebaseUser.uid)
    }

    suspend fun getAllUsers(): List<User> {
        return firebaseService.getAllUsers()
    }

    suspend fun saveFavoriteNews(news: News) {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "saveFavoriteNews: No user logged in")
            throw Exception("Chưa đăng nhập")
        }
        try {
            firebaseService.addFavoriteNews(firebaseUser.uid, news)
            Log.d(TAG, "saveFavoriteNews: Successfully saved news ${news.article_id}")
        } catch (e: Exception) {
            Log.e(TAG, "saveFavoriteNews: Failed to save news: ${e.message}")
            throw e
        }
    }

    suspend fun getFavoriteNewsList(): List<News> {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "getFavoriteNewsList: No user logged in")
            return emptyList()
        }
        try {
            val newsList = firebaseService.getFavoriteNews(firebaseUser.uid)
            Log.d(TAG, "getFavoriteNewsList: Retrieved ${newsList.size} news")
            return newsList
        } catch (e: Exception) {
            Log.e(TAG, "getFavoriteNewsList: Failed to retrieve news: ${e.message}")
            throw e
        }
    }

    suspend fun removeFavoriteNews(newsId: String) {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "removeFavoriteNews: No user logged in")
            throw Exception("Chưa đăng nhập")
        }
        try {
            Log.d(TAG, "removeFavoriteNews: Removing news $newsId for user ${firebaseUser.uid}")
            firebaseService.removeFavoriteNews(firebaseUser.uid, newsId)
            Log.d(TAG, "removeFavoriteNews: News removed")
        } catch (e: Exception) {
            Log.e(TAG, "removeFavoriteNews: Failed to remove news: ${e.message}", e)
            throw Exception("Xóa tin tức yêu thích thất bại: ${e.message}")
        }
    }

    suspend fun updateFavoriteTopics(topics: Map<String, Int>) {
        val firebaseUser = auth.currentUser ?: run {
            Log.e(TAG, "updateFavoriteTopics: No user logged in")
            throw Exception("Chưa đăng nhập")
        }
        try {
            firebaseService.updateFavoriteTopics(firebaseUser.uid, topics)
            Log.d(TAG, "updateFavoriteTopics: Successfully updated topics")
        } catch (e: Exception) {
            Log.e(TAG, "updateFavoriteTopics: Failed to update topics: ${e.message}")
            throw e
        }
    }
    suspend fun addComment(articleId: String, text: String, currentUser: User?): String {
        val user = currentUser ?: throw IllegalStateException("Người dùng chưa đăng nhập để bình luận")
        if (articleId.isBlank()) throw IllegalArgumentException("Article ID không được rỗng")

        val comment = Comment(
            articleId = articleId,
            userId = user.id, // Lấy từ User object
            username = user.username.ifEmpty { user.email.substringBefore('@') }, // Tên hoặc phần đầu email
            avatarUrl = user.avatar.takeIf { it.isNotEmpty() }, // Avatar nếu có
            text = text,
            timestamp = System.currentTimeMillis()
        )
        return firebaseService.addCommentToNews(articleId, comment)
    }

    fun getComments(articleId: String): Flow<List<Comment>> {
        if (articleId.isBlank()) throw IllegalArgumentException("Article ID không được rỗng để lấy bình luận")
        return firebaseService.getCommentsForNews(articleId)
    }

    suspend fun deleteCommentAsAdmin(articleId: String, commentId: String, currentUser: User?) {
        if (currentUser?.admin != true) {
            throw SecurityException("Chỉ admin mới có quyền xóa bình luận.")
        }
        if (articleId.isBlank() || commentId.isBlank()) {
            throw IllegalArgumentException("Article ID và Comment ID không được rỗng.")
        }
        firebaseService.deleteComment(articleId, commentId)
    }


    fun signOut() {
        Log.d(TAG, "signOut: Signing out user")
        auth.signOut()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}