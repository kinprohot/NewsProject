package com.gk.news_pro.page.screen.settings_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.news_pro.data.model.User
import com.gk.news_pro.data.repository.UserPreferencesRepository
import com.gk.news_pro.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AdminActionStatus {
    object Idle : AdminActionStatus()
    object Loading : AdminActionStatus()
    data class Success(val message: String) : AdminActionStatus()
    data class Error(val message: String) : AdminActionStatus()
}

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val selectedLanguage: StateFlow<String> = _currentUser.map { user ->
        user?.language?.takeIf { it.isNotBlank() } ?: "vi" // Mặc định 'vi'
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = "vi"
    )

    val isDarkTheme: StateFlow<Boolean> = userPreferencesRepository.isDarkThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesRepository.DEFAULT_THEME_IS_DARK // Giả sử có hằng số này
        )

    // Trạng thái dành riêng cho admin
    val admin: StateFlow<Boolean> = _currentUser.map { it?.admin ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _adminActionStatus = MutableStateFlow<AdminActionStatus>(AdminActionStatus.Idle)
    val adminActionStatus: StateFlow<AdminActionStatus> = _adminActionStatus.asStateFlow()

    init {
        fetchCurrentUser() // Gọi trước
        viewModelScope.launch {
            admin.collect { adminUserValue -> // Lắng nghe giá trị từ admin StateFlow
                Log.d("SettingsViewModel", "admin StateFlow collected value: $adminUserValue") // THÊM LOG
                if (adminUserValue) {
                    loadAllUsers()
                } else {
                    _allUsers.value = emptyList()
                }
            }
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                _currentUser.value = userRepository.getUser()
                Log.d("SettingsViewModel", "Current user fetched: ${_currentUser.value?.email}, lang: ${_currentUser.value?.language}, admin: ${_currentUser.value?.admin}")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error fetching current user: ${e.message}")
                _currentUser.value = null
            }
        }
    }

    fun updateSelectedLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUserLanguage(languageCode)
                fetchCurrentUser() // Tải lại để cập nhật selectedLanguage flow
                Log.d("SettingsViewModel", "Language updated to $languageCode and user re-fetched.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to update language: ${e.message}", e)
            }
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemePreference(isDark)
        }
    }

    // --- Các hàm của Admin ---
    fun loadAllUsers() {
        if (_currentUser.value?.admin == true) {
            viewModelScope.launch {
                _adminActionStatus.value = AdminActionStatus.Loading
                try {
                    _allUsers.value = userRepository.getAllUsers()
                    _adminActionStatus.value = AdminActionStatus.Idle // Hoặc Success("Users loaded")
                    Log.d("SettingsViewModel", "Loaded ${_allUsers.value.size} users for admin.")
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Failed to load all users: ${e.message}", e)
                    _adminActionStatus.value = AdminActionStatus.Error("Không thể tải danh sách người dùng: ${e.message}")
                }
            }
        } else {
            Log.w("SettingsViewModel", "Attempt to load all users by non-admin.")
            _allUsers.value = emptyList()
        }
    }

    fun adminCreateNewUser(username: String, email: String, password: String, makeAdmin: Boolean) {
        if (_currentUser.value?.admin != true) return
        viewModelScope.launch {
            _adminActionStatus.value = AdminActionStatus.Loading
            try {
                userRepository.createUser(username, email, password, makeAdmin)
                _adminActionStatus.value = AdminActionStatus.Success("Người dùng $username đã được tạo thành công.")
                loadAllUsers() // Làm mới danh sách người dùng
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Admin failed to create user: ${e.message}", e)
                _adminActionStatus.value = AdminActionStatus.Error("Không thể tạo người dùng: ${e.message}")
            }
        }
    }

    fun adminUpdateUser(
        userId: String,
        newUsername: String?,
        newEmail: String?, // Chỉ cập nhật email trong DB
        newPassword: String?, // Chỉ cập nhật password trong DB
        newadmin: Boolean?
    ) {
        if (_currentUser.value?.admin != true) return
        viewModelScope.launch {
            _adminActionStatus.value = AdminActionStatus.Loading
            try {
                userRepository.adminUpdateUserRecord(userId, newUsername, newEmail, newPassword, newadmin)
                _adminActionStatus.value = AdminActionStatus.Success("Người dùng $userId đã được cập nhật.")
                loadAllUsers() // Làm mới danh sách người dùng
                if (userId == _currentUser.value?.id && newadmin != null && newadmin != _currentUser.value?.admin) {
                    fetchCurrentUser() // Nếu admin thay đổi quyền của chính mình, tải lại người dùng hiện tại
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Admin failed to update user $userId: ${e.message}", e)
                _adminActionStatus.value = AdminActionStatus.Error("Không thể cập nhật người dùng: ${e.message}")
            }
        }
    }

    fun adminDeleteUser(userId: String) {
        if (_currentUser.value?.admin != true) return
        if (userId == _currentUser.value?.id) {
            _adminActionStatus.value = AdminActionStatus.Error("Admin không thể tự xóa tài khoản của mình từ bảng này.")
            return
        }
        viewModelScope.launch {
            _adminActionStatus.value = AdminActionStatus.Loading
            try {
                userRepository.adminDeleteUserRecord(userId) // Chỉ xóa bản ghi DB
                _adminActionStatus.value = AdminActionStatus.Success("Dữ liệu người dùng $userId đã được xóa.")
                loadAllUsers() // Làm mới danh sách người dùng
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Admin failed to delete user $userId: ${e.message}", e)
                _adminActionStatus.value = AdminActionStatus.Error("Không thể xóa dữ liệu người dùng: ${e.message}")
            }
        }
    }

    fun clearAdminActionStatus() {
        _adminActionStatus.value = AdminActionStatus.Idle
    }
}