package com.example.pam_1.data.repository

import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepository {

    private val supabase = SupabaseClient.client

    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun login(email: String, pass: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            password = pass
        }
    }

    suspend fun register(user: User, pass: String) {
        // Kirim SEMUA data user lewat raw_user_meta_data
        val metadata = buildJsonObject {
            put("username", JsonPrimitive(user.username))
            put("full_name", JsonPrimitive(user.full_name))
            put("phone_number", JsonPrimitive(user.phone_number))
        }

        supabase.auth.signUpWith(Email) {
            email = user.email
            password = pass
            // Metadata ini akan ditangkap oleh trigger SQL
            this.data = metadata
        }

        // Trigger SQL otomatis insert ke tabel public.users
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }

    suspend fun testConnection(): Boolean {
        return try {
            val res = supabase.from("users")
                .select()
            println("Supabase Connected: ${res.data}")
            true
        } catch (e: Exception) {
            println("Supabase Error: ${e.message}")
            false
        }
    }
}