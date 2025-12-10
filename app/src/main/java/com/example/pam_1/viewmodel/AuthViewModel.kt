package com.example.pam_1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.User
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State Auth (Login/Register)
sealed class AuthUIState {
    object Idle : AuthUIState()
    object Loading : AuthUIState()
    object Success : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

// State Profile
sealed class ProfileUIState {
    object Loading : ProfileUIState()
    data class Success(val user: User) : ProfileUIState()
    data class Error(val message: String) : ProfileUIState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var authState by mutableStateOf<AuthUIState>(AuthUIState.Idle)
        private set

    private val _profileState = MutableStateFlow<ProfileUIState>(ProfileUIState.Loading)
    val profileState: StateFlow<ProfileUIState> = _profileState.asStateFlow()

    var isUpdatingProfile by mutableStateOf(false)
        private set

    // --- AUTH FUNCTIONS ---

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.login(email, pass)
                authState = AuthUIState.Success
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Login Gagal")
            }
        }
    }

    fun register(email: String, pass: String, username: String, full_name: String, phone_number: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                val user = User(
                    email = email,
                    username = username,
                    full_name = full_name,
                    phone_number = phone_number
                )
                authRepository.register(user, pass)
                authState = AuthUIState.Success
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Register Gagal")
            }
        }
    }

    fun logout(onResult: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            // Reset profile state saat logout
            _profileState.value = ProfileUIState.Loading
            onResult()
        }
    }

    fun resetState() {
        authState = AuthUIState.Idle
    }

    // --- PROFILE FUNCTIONS ---

    /**
     * Fetch user profile dari database
     * Dipanggil saat masuk ke ProfileScreen
     */
    fun fetchUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUIState.Loading
            try {
                val user = userRepository.getCurrentUserProfile()
                _profileState.value = ProfileUIState.Success(user)
            } catch (e: Exception) {
                _profileState.value = ProfileUIState.Error(
                    e.message ?: "Gagal memuat profil"
                )
            }
        }
    }

    /**
     * Simpan perubahan profile user
     * Flow:
     * 1. Upload foto baru (jika ada)
     * 2. Update data user ke database
     * 3. Update state di UI
     */
    fun saveProfileChanges(
        username: String,
        fullName: String,
        phone: String,
        bio: String,
        photoBytes: ByteArray?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _profileState.value
        if (currentState !is ProfileUIState.Success) {
            onError("Profile belum dimuat")
            return
        }

        viewModelScope.launch {
            isUpdatingProfile = true
            try {
                val currentUser = currentState.user
                var finalPhotoUrl = currentUser.photo_profile

                // 1. Upload foto baru jika ada
                if (photoBytes != null) {
                    val fileName = "profile_${currentUser.user_id}.jpg"
                    val url = userRepository.uploadProfilePhoto(photoBytes, fileName)

                    // Cache busting: tambahkan timestamp agar gambar refresh
                    finalPhotoUrl = "$url?t=${System.currentTimeMillis()}"
                }

                // 2. Siapkan data user yang akan diupdate
                val updatedUser = currentUser.copy(
                    username = username.trim(),
                    full_name = fullName.trim(),
                    phone_number = phone.trim(),
                    bio = bio.trim().takeIf { it.isNotEmpty() },
                    photo_profile = finalPhotoUrl
                )

                // 3. Update ke database
                val savedUser = userRepository.updateUserProfile(updatedUser)

                // 4. Update UI state dengan data terbaru
                _profileState.value = ProfileUIState.Success(savedUser)

                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyimpan perubahan")
            } finally {
                isUpdatingProfile = false
            }
        }
    }

    /**
     * Refresh profile data
     * Berguna saat kembali dari EditProfileScreen
     */
    fun refreshProfile() {
        fetchUserProfile()
    }
}