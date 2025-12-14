package com.example.pam_1.data.repository

import android.content.Context
import com.example.pam_1.data.DataStoreManager
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import io.github.jan.supabase.auth.OtpType // Tetap perlukan untuk verifikasi

class AuthRepository(context: Context) {

    private val supabase = SupabaseClient.client
    private val dataStoreManager = DataStoreManager(context)

    // ... (Fungsi-fungsi lama: isUserLoggedIn, login, registerWithOTP) ...

    suspend fun isUserLoggedIn(): Boolean {
        val hasSession = supabase.auth.currentSessionOrNull() != null
        val rememberMe = dataStoreManager.rememberMeFlow.first()

        if (!rememberMe && hasSession) {
            logout()
            return false
        }

        return hasSession && rememberMe
    }

    suspend fun login(email: String, pass: String, rememberMe: Boolean) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = pass
        }

        dataStoreManager.saveRememberMe(rememberMe)
    }

    suspend fun registerWithOTP(user: User, pass: String) {
        val metadata = buildJsonObject {
            put("username", JsonPrimitive(user.username))
            put("full_name", JsonPrimitive(user.full_name))
            put("phone_number", JsonPrimitive(user.phone_number))
        }

        supabase.auth.signUpWith(Email) {
            email = user.email
            password = pass
            data = metadata
        }
    }

    // Verifikasi OTP (untuk SIGNUP)
    suspend fun verifyOTP(email: String, token: String) {
        supabase.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
    }

    // Kirim ulang OTP (untuk SIGNUP)
    suspend fun resendOTP(email: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
        dataStoreManager.saveRememberMe(false)
    }


    // ========================================
    // FUNGSI RESET PASSWORD (Menggunakan resetPasswordForEmail)
    // ========================================

    // 1. Mengirim Kode Reset Password (Asumsi template Supabase sudah diubah menjadi OTP)
    suspend fun sendPasswordReset(email: String) {
        // Ini akan mengirim email dengan token/kode ke user.
        supabase.auth.resetPasswordForEmail(email)
    }

    // 2. Verifikasi Kode Reset Password
    // Kita tetap harus menggunakan verifyEmailOtp dengan tipe RECOVERY
    // karena ini adalah fungsi yang memvalidasi kode dan mengaktifkan sesi sementara
    // untuk update password.
    suspend fun verifyResetPasswordOTP(email: String, token: String) {
        supabase.auth.verifyEmailOtp(
            type = OtpType.Email.RECOVERY,
            email = email,
            token = token
        )
    }

    // 3. Update Password Baru
    suspend fun updatePassword(newPassword: String) {
        // Hanya bisa dipanggil setelah verifyResetPasswordOTP berhasil
        supabase.auth.updateUser {
            password = newPassword
        }
    }
}