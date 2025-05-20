package com.gk.news_pro.page.main_viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gk.news_pro.data.repository.NewsRepository
import com.gk.news_pro.data.repository.GeminiRepository
import com.gk.news_pro.data.repository.UserRepository
import com.gk.news_pro.page.screen.auth.LoginViewModel
import com.gk.news_pro.page.screen.auth.RegisterViewModel
import com.gk.news_pro.page.screen.explore_screen.ExploreViewModel
import com.gk.news_pro.page.screen.favorite_screen.FavoriteViewModel
import com.gk.news_pro.page.screen.home_screen.HomeViewModel
import com.gk.news_pro.page.screen.settings_screen.SettingsViewModel
import com.gk.news_pro.data.repository.UserPreferencesRepository
import com.gk.news_pro.page.navigation.Screen
import com.gk.news_pro.page.screen.detail_screen.NewsDetailViewModel
import com.gk.news_pro.page.screen.profile_screen.ProfileViewModel

class ViewModelFactory(
    private val repositories: Any
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repositories as NewsRepository) as T
            }
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> {
                val (newsRepo, userRepo) = repositories as List<Any>
                ExploreViewModel(newsRepo as NewsRepository, userRepo as UserRepository) as T
            }
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repositories as UserRepository) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(repositories as UserRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                val (userRepo, userPre) = repositories as List<Any>
                SettingsViewModel(userRepo as UserRepository,userPre as UserPreferencesRepository) as T
            }
            modelClass.isAssignableFrom(NewsDetailViewModel::class.java) -> {
                val (geiminiRepo, userRepo) = repositories as List<Any>
                NewsDetailViewModel(geiminiRepo as GeminiRepository, userRepo as UserRepository) as T
            }
            modelClass.isAssignableFrom(FavoriteViewModel::class.java) -> {
                FavoriteViewModel(repositories as UserRepository) as T
            }modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repositories as UserRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}