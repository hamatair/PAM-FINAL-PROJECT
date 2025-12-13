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
    object AwaitingNewPassword : AuthUIState() // [FIX] State ini wajib ada untuk alur reset password
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

    // Simpan email untuk verifikasi OTP
    var pendingEmail by mutableStateOf("")
        private set

    // Penanda apakah sedang dalam proses Reset Password atau Register biasa
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

    // Register dengan OTP
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
                authRepository.registerWithOTP(user, pass)

                // Simpan email untuk verifikasi
                pendingEmail = email
                isResetPasswordFlow = false // Pastikan ini flow register

                // Set state ke AwaitingOTP
                authState = AuthUIState.AwaitingOTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Register Gagal")
            }
        }
    }

    // [FIX] Mengirim OTP Reset Password (Recovery)
    // Nama fungsi disesuaikan agar cocok dengan panggilan di ForgotPasswordScreen
    fun sendResetPasswordOTP(email: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.sendPasswordReset(email)

                pendingEmail = email
                isResetPasswordFlow = true // TANDAI INI ADALAH ALUR RESET PASSWORD

                authState = AuthUIState.AwaitingOTP // Langsung navigasi ke layar OTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengirim kode reset password")
            }
        }
    }

    // [FIX] Verifikasi OTP (Menangani baik Register maupun Reset Password)
    fun verifyOTP(token: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                if (isResetPasswordFlow) {
                    // ALUR RESET PASSWORD:
                    // Memanggil fungsi repo yang menggunakan type = OtpType.Email.RECOVERY
                    authRepository.verifyResetPasswordOTP(pendingEmail, token)

                    // Jika sukses, jangan clear pendingEmail dulu (masih butuh buat update password)
                    authState = AuthUIState.AwaitingNewPassword
                } else {
                    // ALUR SIGNUP:
                    // Memanggil fungsi repo yang menggunakan type = OtpType.Email.SIGNUP
                    authRepository.verifyOTP(pendingEmail, token)

                    pendingEmail = "" // Clear email karena sudah selesai
                    authState = AuthUIState.Success
                }
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Kode OTP Salah atau Kadaluarsa")
            }
        }
    }

    // Kirim ulang OTP
    fun resendOTP() {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                if (isResetPasswordFlow) {
                    // Kirim ulang untuk alur Reset Password
                    authRepository.sendPasswordReset(pendingEmail)
                } else {
                    // Kirim ulang untuk alur Sign Up
                    authRepository.resendOTP(pendingEmail)
                }
                authState = AuthUIState.AwaitingOTP
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengirim ulang kode")
            }
        }
    }

    // Update Password Baru (Langkah terakhir Reset Password)
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            authState = AuthUIState.Loading
            try {
                authRepository.updatePassword(newPassword)

                isResetPasswordFlow = false // Reset penanda alur
                pendingEmail = ""

                authState = AuthUIState.Success // Sukses total, kembali ke Login
            } catch (e: Exception) {
                authState = AuthUIState.Error(e.message ?: "Gagal mengubah password")
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
}