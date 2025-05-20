package com.gk.news_pro.page.screen.detail_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.news_pro.data.model.Comment
import com.gk.news_pro.data.model.News
import com.gk.news_pro.data.repository.UserRepository
import com.gk.news_pro.data.model.User
import com.gk.news_pro.data.repository.GeminiRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.gk.news_pro.page.screen.detail_screen.NewsDetailScreen

class NewsDetailViewModel(
    private val geminiRepository: GeminiRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val TAG = "NewsDetailViewModel"

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis

    private val _aiContinuation = MutableStateFlow<String?>(null)
    val aiContinuation: StateFlow<String?> = _aiContinuation

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _isSubmittingComment = MutableStateFlow(false)
    val isSubmittingComment: StateFlow<Boolean> = _isSubmittingComment.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                _currentUser.value = userRepository.getUser()
                Log.d(TAG, "Current user fetched: ${_currentUser.value?.email}, Admin: ${_currentUser.value?.admin}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current user: ${e.message}")
            }
        }
    }

    fun analyzeNewsContent(news: News) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val content = buildString {
                    append(news.title)
                    if (!news.description.isNullOrEmpty()) {
                        append("\n\n${news.description}")
                    }
                }
                val analysis = geminiRepository.analyzeNews(content)
                _aiAnalysis.value = analysis
            } catch (e: Exception) {
                _error.value = when (e) {
                    is IllegalArgumentException -> "Nội dung bài báo không hợp lệ"
                    else -> e.localizedMessage ?: "Lỗi không xác định khi phân tích nội dung"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun analyzeNewsFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val analysis = geminiRepository.analyzeArticleFromUrl(url)
                _aiAnalysis.value = analysis
            } catch (e:

                     Exception) {
                _error.value = when (e) {
                    is IllegalArgumentException -> "URL không hợp lệ"
                    is java.net.UnknownHostException -> "Không thể kết nối đến URL"
                    else -> e.localizedMessage ?: "Lỗi không xác định khi phân tích URL"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun continueArticle(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val continuation = geminiRepository.continueArticleFromUrl(url)
                _aiContinuation.value = continuation
            } catch (e: Exception) {
                _error.value = when (e) {
                    is IllegalArgumentException -> "URL không hợp lệ để tiếp tục bài báo"
                    is java.net.UnknownHostException -> "Không thể kết nối đến URL"
                    else -> e.localizedMessage ?: "Lỗi không xác định khi tiếp tục bài báo"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadComments(articleId: String) {
        if (articleId.isBlank()) {
            Log.e(TAG, "loadComments: Article ID is blank, cannot load comments.")
            _comments.value = emptyList() // Hoặc hiển thị lỗi
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "loadComments: Loading comments for article $articleId")
            userRepository.getComments(articleId)
                .catch { e ->
                    Log.e(TAG, "Error loading comments for $articleId: ${e.message}", e)
                    _commentError.value = "Không thể tải bình luận: ${e.message}"
                    _comments.value = emptyList() // Đảm bảo danh sách rỗng nếu lỗi
                }
                .collect { commentsList ->
                    Log.d(TAG, "loadComments: Received ${commentsList.size} comments for $articleId")
                    _comments.value = commentsList
                    _commentError.value = null // Xóa lỗi nếu tải thành công
                }
        }
    }

    fun onCommentTextChanged(newText: String) {
        _commentText.value = newText
    }

    fun submitComment(articleId: String) {
        if (_commentText.value.isBlank()) {
            _commentError.value = "Nội dung bình luận không được để trống."
            return
        }
        if (articleId.isBlank()) {
            _commentError.value = "Không tìm thấy ID bài báo để bình luận."
            return
        }
        if (_currentUser.value == null) {
            _commentError.value = "Bạn cần đăng nhập để bình luận."
            // Có thể điều hướng đến màn hình đăng nhập ở đây
            return
        }

        _isSubmittingComment.value = true
        _commentError.value = null // Xóa lỗi cũ
        viewModelScope.launch {
            try {
                userRepository.addComment(articleId, _commentText.value, _currentUser.value)
                _commentText.value = "" // Xóa nội dung đã nhập
                Log.d(TAG, "Comment submitted successfully for article $articleId")
                // Không cần loadComments lại vì Flow sẽ tự cập nhật
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting comment for $articleId: ${e.message}", e)
                _commentError.value = "Gửi bình luận thất bại: ${e.message}"
            } finally {
                _isSubmittingComment.value = false
            }
        }
    }

    fun deleteCommentByAdmin(articleId: String, commentId: String) {
        if (_currentUser.value?.admin != true) {
            Log.w(TAG, "Attempt to delete comment by non-admin or unauthenticated user.")
            _commentError.value = "Bạn không có quyền xóa bình luận này."
            return
        }
        if (articleId.isBlank() || commentId.isBlank()) {
            _commentError.value = "Thông tin không hợp lệ để xóa bình luận."
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Admin deleting comment $commentId for article $articleId")
                userRepository.deleteCommentAsAdmin(articleId, commentId, _currentUser.value)
                // Flow sẽ tự cập nhật danh sách bình luận
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting comment $commentId by admin: ${e.message}", e)
                _commentError.value = "Xóa bình luận thất bại: ${e.message}"
            }
        }
    }

    fun clearCommentError() {
        _commentError.value = null
    }
    fun clearAnalysis() {
        _aiAnalysis.value = null
        _error.value = null
    }

    fun clearContinuation() {
        _aiContinuation.value = null
        _error.value = null
    }
}