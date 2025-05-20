// Trong package page/screen/profile_screen (tạo package và file mới)
package com.gk.news_pro.page.screen.profile_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gk.news_pro.data.model.User
import com.gk.news_pro.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileScreenUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccessMessage: String? = null
)

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenUiState(isLoading = true))
    val uiState: StateFlow<ProfileScreenUiState> = _uiState.asStateFlow()

    private val TAG = "ProfileViewModel"

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, updateSuccessMessage = null)
            try {
                val currentUser = userRepository.getUser()
                _uiState.value = _uiState.value.copy(user = currentUser, isLoading = false)
                Log.d(TAG, "User profile loaded: ${currentUser?.email}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to load profile", isLoading = false)
            }
        }
    }

    fun updateProfile(newUsername: String, newPassword1: String, newPassword2: String, newAvatar: String? = null) {
        if (newPassword1.isNotEmpty() && newPassword1 != newPassword2) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match.", updateSuccessMessage = null)
            return
        }

        val passwordToUpdate = if (newPassword1.isNotBlank()) newPassword1 else null
        val usernameToUpdate = newUsername.takeIf { it.isNotBlank() }


        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, updateSuccessMessage = null)
            try {
                userRepository.updateUser( // Gọi hàm updateUser với tên gốc
                    newUsername = usernameToUpdate,
                    newPassword1 = passwordToUpdate,
                    newAvatar = newAvatar
                )
                val updatedUser = userRepository.getUser()
                _uiState.value = _uiState.value.copy(
                    user = updatedUser,
                    isLoading = false,
                    updateSuccessMessage = "Profile updated successfully!",
                    error = null
                )
                Log.d(TAG, "Profile updated successfully for ${updatedUser?.email}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update profile",
                    isLoading = false,
                    updateSuccessMessage = null
                )
            }
        }
    }


    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, updateSuccessMessage = null)
    }
}