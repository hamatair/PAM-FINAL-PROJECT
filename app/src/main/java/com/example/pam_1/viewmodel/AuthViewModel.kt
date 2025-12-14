package com.example.pam_1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.User
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.navigations.NavigationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State Auth (Login/Register/Reset)
sealed class AuthUIState {
    object Idle : AuthUIState()
    object Loading : AuthUIState()
    object Success : AuthUIState()
    object AwaitingOTP : AuthUIState()
    object AwaitingNewPassword : AuthUIState()
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

    var pendingEmail by mutableStateOf("")
        private set

    var isResetPasswordFlow by mutableStateOf(false)
        private set

    private val _lastActiveTab = MutableStateFlow(NavigationItem.Tugas.route)
    val lastActiveTab: StateFlow<String> = _lastActiveTab

    fun setLastActiveTab(route: String) {
        _lastActiveTab.value = route
    }

    // --- AUTH FUNCTIONS ---

    fun login(email: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.login(email, pass, rememberMe)
                authState = AuthUIState.Success
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Login Gagal")
            }
        }
    }

    fun register(email: String, pass: String, username: String, full_name: String, phone_number: Nothing?) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                val user = User(
                    email = email,
                    username = username,
                    full_name = full_name,
                    phone_number = phone_number
                )
                authRepository.registerWithOTP(user, pass)

                pendingEmail = email
                isResetPasswordFlow = false

                authState = AuthUIState.AwaitingOTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Register Gagal")
            }
        }
    }

    fun sendResetPasswordOTP(email: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.sendPasswordReset(email)

                pendingEmail = email
                isResetPasswordFlow = true

                authState = AuthUIState.AwaitingOTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengirim kode reset password")
            }
        }
    }

    fun verifyOTP(token: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                if (isResetPasswordFlow) {
                    authRepository.verifyResetPasswordOTP(pendingEmail, token)
                    authState = AuthUIState.AwaitingNewPassword
                } else {
                    authRepository.verifyOTP(pendingEmail, token)
                    pendingEmail = ""
                    authState = AuthUIState.Success
                }
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Kode OTP Salah atau Kadaluarsa")
            }
        }
    }

    fun resendOTP() {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                if (isResetPasswordFlow) {
                    authRepository.sendPasswordReset(pendingEmail)
                } else {
                    authRepository.resendOTP(pendingEmail)
                }
                authState = AuthUIState.AwaitingOTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengirim ulang kode")
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.updatePassword(newPassword)

                isResetPasswordFlow = false
                pendingEmail = ""

                authState = AuthUIState.Success
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengubah password")
            }
        }
    }

    fun logout(onResult: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _profileState.value = ProfileUIState.Loading
            onResult()
        }
    }

    fun resetState() {
        authState = AuthUIState.Idle
    }

    // --- PROFILE FUNCTIONS ---

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

    fun saveProfileChanges(
        username: String,
        fullName: String,
        phone: String,
        bio: String,
        photoBytes: ByteArray?,
        isPhotoDeleted: Boolean = false,
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
                val DEFAULT_AVATAR = "https://jhrbjirccxuhtzygwzgx.supabase.co/storage/v1/object/public/profile/avatar/default.png"
                var finalPhotoUrl = currentUser.photo_profile
                val finalPhone = if (phone.isBlank()) null else phone

                // 1. Handle foto
                if (isPhotoDeleted) {
                    // ✅ HAPUS foto lama dari storage (jika bukan default)
                    val oldPhotoUrl = currentUser.photo_profile
                    println("🗑️ Deleting photo - Old URL: $oldPhotoUrl")

                    if (oldPhotoUrl != null && oldPhotoUrl != DEFAULT_AVATAR && !oldPhotoUrl.contains("default.png")) {
                        val deleted = userRepository.deleteProfilePhoto(oldPhotoUrl)
                        if (deleted) {
                            println("✅ Old photo deleted from storage")
                        } else {
                            println("⚠️ Failed to delete old photo, but continuing...")
                        }
                    }

                    // Set ke default avatar
                    finalPhotoUrl = DEFAULT_AVATAR
                    println("📝 Setting photo URL to DEFAULT_AVATAR: $finalPhotoUrl")

                } else if (photoBytes != null) {
                    // ✅ HAPUS foto lama sebelum upload foto baru (jika bukan default)
                    val oldPhotoUrl = currentUser.photo_profile
                    println("📤 Uploading new photo - Old URL: $oldPhotoUrl")

                    if (oldPhotoUrl != null && oldPhotoUrl != DEFAULT_AVATAR && !oldPhotoUrl.contains("default.png")) {
                        userRepository.deleteProfilePhoto(oldPhotoUrl)
                    }

                    // Upload foto baru
                    val fileName = "profile_${currentUser.user_id}.jpg"
                    val url = userRepository.uploadProfilePhoto(photoBytes, fileName)
                    finalPhotoUrl = url
                    println("✅ New photo uploaded: $finalPhotoUrl")
                }

                // 2. Siapkan data user yang akan diupdate
                val updatedUser = currentUser.copy(
                    username = username.trim(),
                    full_name = fullName.trim(),
                    phone_number = finalPhone,
                    bio = bio.trim().takeIf { it.isNotEmpty() },
                    photo_profile = finalPhotoUrl
                )

                println("📊 Data to update:")
                println("   - Username: ${updatedUser.username}")
                println("   - Full Name: ${updatedUser.full_name}")
                println("   - Phone: ${updatedUser.phone_number}")
                println("   - Bio: ${updatedUser.bio}")
                println("   - Photo URL: ${updatedUser.photo_profile}")

                // 3. Update ke database
                val savedUser = userRepository.updateUserProfile(updatedUser)

                println("✅ Saved user from DB:")
                println("   - Photo URL: ${savedUser.photo_profile}")

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
}