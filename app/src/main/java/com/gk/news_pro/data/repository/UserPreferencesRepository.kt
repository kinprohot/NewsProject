package com.gk.news_pro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Khởi tạo DataStore instance ở top-level của file
// Tên "user_settings" là tên của file preferences sẽ được tạo.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    // Khai báo các khóa (keys) cho từng cài đặt bạn muốn lưu
     companion object PreferenceKeys {
        // Bạn có thể giữ lại các key này nếu có lúc nào đó muốn dùng DataStore cho ngôn ngữ/quốc gia
        // Hoặc xóa chúng nếu chỉ dùng Firebase cho ngôn ngữ.
        // val SELECTED_COUNTRY = stringPreferencesKey("selected_country")
        // val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")

        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")

        // Giá trị mặc định
        // const val DEFAULT_COUNTRY = "us"
        // const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_THEME_IS_DARK = false // Mặc định là giao diện sáng
    }

    // Flow để đọc cài đặt theme sáng/tối
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            // IOException thường xảy ra khi đọc file
            if (exception is IOException) {
                // Ghi log lỗi và trả về preferences rỗng
                emit(emptyPreferences())
            } else {
                // Ném lại các lỗi khác
                throw exception
            }
        }
        .map { preferences ->
            // Đọc giá trị boolean từ key IS_DARK_THEME.
            // Nếu key không tồn tại, sử dụng giá trị mặc định DEFAULT_THEME_IS_DARK.
            preferences[IS_DARK_THEME] ?: DEFAULT_THEME_IS_DARK
        }

    // Hàm để lưu cài đặt theme sáng/tối
    suspend fun saveThemePreference(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDark
        }
    }

    // --- Các hàm cho COUNTRY và LANGUAGE (Bạn có thể XÓA nếu không dùng DataStore cho chúng) ---
    /*
    val selectedCountryFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_COUNTRY] ?: DEFAULT_COUNTRY
        }

    suspend fun saveSelectedCountry(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_COUNTRY] = countryCode
        }
    }

    val selectedLanguageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_LANGUAGE] ?: DEFAULT_LANGUAGE
        }

    suspend fun saveSelectedLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = languageCode
        }
    }
    */
    // --- Kết thúc phần có thể xóa ---
}