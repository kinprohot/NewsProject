package com.gk.news_pro.page.screen.home_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.news_pro.data.model.News
import com.gk.news_pro.data.model.User
import com.gk.news_pro.data.repository.NewsRepository
import com.gk.news_pro.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.gk.news_pro.page.navigation.AppNavigation

class HomeViewModel(
    private val repository: NewsRepository
) : ViewModel() {
    private val _trendingNews = MutableStateFlow<List<News>>(emptyList())
    val trendingNews: StateFlow<List<News>> = _trendingNews
    private val _newsState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val newsState: StateFlow<NewsUiState> = _newsState
    private val TAG = "HomeViewModel"

    // Lưu trữ danh sách News
    private val newsList = mutableListOf<News>()

    init {
        fetchGeneralNews()
        fetchTrendingNews()
    }

    fun testGetUser() {
        val userRepository = UserRepository()
        viewModelScope.launch {
            try {
                val user = userRepository.createUser(
                    username = "test",
                    email = "test@example.com",
                    password = "test1234"
                )
                Log.d(TAG, "testGetUser: Create user successful: ${user?.email}, Password = ${user?.password}")
            } catch (e: Exception) {
                Log.e(TAG, "testGetUser: Error creating user: ${e.message}", e)
            }
        }
    }
    fun fetchTrendingNews() {
        viewModelScope.launch {
            try {
                val localUserRepository = UserRepository() // Tạo instance UserRepository cục bộ
                val currentUser = localUserRepository.getUser() // Lấy user hiện tại
                val languageToUse = currentUser?.language?.takeIf { it.isNotBlank() }
                val response = repository.getNews(
                    language = languageToUse,
                    apiKey = "pub_7827211e80c068cf7ded249ee01e644d60afc"
                )
                _trendingNews.value = response.results?.take(5) ?: emptyList()
                Log.d("ExploreViewModel", "Fetched ${response.results?.size ?: 0} trending news, limited to 5")
            } catch (e: Exception) {
                _trendingNews.value = emptyList()
                Log.e("ExploreViewModel", "Error fetching trending news: ${e.message}", e)
            }
        }
    }
    fun fetchGeneralNews() {
        _newsState.value = NewsUiState.Loading
        viewModelScope.launch {
            try {
                testGetUser()
                val localUserRepository = UserRepository() // Tạo instance UserRepository cục bộ
                val currentUser = localUserRepository.getUser() // Lấy user hiện tại
                val languageToUse = currentUser?.language?.takeIf { it.isNotBlank() }
                val response = repository.getNews(
                    apiKey = "pub_7827211e80c068cf7ded249ee01e644d60afc",//"pub_7827211e80c068cf7ded249ee01e644d60afc" //"pub_7827211e80c068cf7ded249ee01e644d60afc"
                    language = languageToUse
                )
                if (response.results.isNullOrEmpty()) {
                    Log.e(TAG, "fetchGeneralNews: No news available from API")
                    _newsState.value = NewsUiState.Error("No news available")
                } else {
                    newsList.clear()
                    newsList.addAll(response.results)
                    Log.d(TAG, "fetchGeneralNews: Loaded ${newsList.size} news items")
                    _newsState.value = NewsUiState.Success(response.results)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchGeneralNews: Error loading news: ${e.message}", e)
                _newsState.value = NewsUiState.Error(e.message ?: "Failed to load news")
            }
        }
    }
    fun resetAndRefetchNews() {
        Log.d(TAG, "resetAndRefetchNews: Clearing news and refetching...")
        newsList.clear() // Xóa cache tin tức hiện tại trong ViewModel
        _newsState.value = NewsUiState.Loading // Hiển thị trạng thái loading
        fetchGeneralNews() // Gọi lại hàm fetch để tải tin mới
    }
    fun getNewsById(articleId: String): News? {
        val news = newsList.find { it.article_id == articleId }
        if (news == null) {
            Log.e(TAG, "getNewsById: No news found for articleId: $articleId")
        } else {
            Log.d(TAG, "getNewsById: Found news for articleId: $articleId, title: ${news.title}")
        }
        return news
    }

    fun retry() {
        resetAndRefetchNews()
    }

}

sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(val news: List<News>) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}