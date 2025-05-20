// Trong package page (hoặc một package dùng chung cho ViewModel cấp app)
package com.gk.news_pro.page // Ví dụ

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.news_pro.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppViewModel(userPreferencesRepository: UserPreferencesRepository) : ViewModel() {
    val isDarkThemeEnabled = userPreferencesRepository.isDarkThemeFlow
        .stateIn(
            scope = viewModelScope,
            // Eagerly để nó bắt đầu thu thập ngay lập tức và giữ giá trị
            started = SharingStarted.Eagerly,
            initialValue = UserPreferencesRepository.DEFAULT_THEME_IS_DARK
        )
}