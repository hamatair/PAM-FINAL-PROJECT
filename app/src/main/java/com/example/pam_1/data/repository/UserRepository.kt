package com.example.pam_1.data.repository

import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.DEFAULT_AVATAR
import com.example.pam_1.data.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    private val supabase = SupabaseClient.client
    private val userTable = "users"
    private val bucket = "profile"

    /**
     * Fetch user profile dari database
     */
    suspend fun getCurrentUserProfile(): User {
        return withContext(Dispatchers.IO) {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("User tidak terautentikasi.")

            try {
                println("🔍 Fetching user profile for ID: $currentUserId")

                // ✅ CRITICAL FIX: Pakai "user_id", BUKAN "id"
                // Karena di database kolom primary key bernama "user_id"
                val response = supabase.postgrest[userTable]
                    .select {
                        filter {
                            eq("user_id", currentUserId)  // ← UBAH DARI "id" KE "user_id"
                        }
                    }

                val users = response.decodeList<User>()

                println("📊 Found ${users.size} user(s)")

                if (users.isEmpty()) {
                    throw Exception("Data user tidak ditemukan di database. Pastikan user dengan ID $currentUserId ada di tabel users.")
                }

                val user = users.first()
                println("✅ User profile loaded: ${user.username}")

                // Pastikan photo_profile tidak null
                user.copy(
                    photo_profile = user.photo_profile.takeIf { !it.isNullOrBlank() } ?: DEFAULT_AVATAR
                )

            } catch (e: Exception) {
                println("❌ Error fetching user profile: ${e.message}")
                e.printStackTrace()
                throw Exception("Gagal memuat profil: ${e.message}")
            }
        }
    }

    /**
     * Update profile user ke database
     */
    suspend fun updateUserProfile(user: User): User {
        return withContext(Dispatchers.IO) {
            try {
                val userId = user.user_id ?: throw Exception("User ID tidak valid")

                println("🔄 Updating user profile for ID: $userId")

                val dataToUpdate = mapOf<String, Any?>(
                    "username" to user.username,
                    "full_name" to user.full_name,
                    "phone_number" to user.phone_number,
                    "bio" to user.bio,
                    "photo_profile" to user.photo_profile
                )

                // ✅ CRITICAL FIX: Pakai "user_id", BUKAN "id"
                val response = supabase.postgrest[userTable]
                    .update(dataToUpdate) {
                        filter {
                            eq("user_id", userId)  // ← UBAH DARI "id" KE "user_id"
                        }
                    }

                val updatedUsers = response.decodeList<User>()

                if (updatedUsers.isEmpty()) {
                    throw Exception("User tidak ditemukan saat update")
                }

                val updatedUser = updatedUsers.first()
                println("✅ User profile updated successfully")

                updatedUser.copy(
                    photo_profile = updatedUser.photo_profile.takeIf { !it.isNullOrBlank() } ?: DEFAULT_AVATAR
                )

            } catch (e: Exception) {
                println("❌ Error updating user profile: ${e.message}")
                e.printStackTrace()
                throw Exception("Gagal update profil: ${e.message}")
            }
        }
    }

    /**
     * Upload foto profile ke Supabase Storage
     */
    suspend fun uploadProfilePhoto(bytes: ByteArray, fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                println("📤 Uploading profile photo: $fileName")

                val storageClient = supabase.storage[bucket]

                storageClient.upload(
                    path = fileName,
                    data = bytes
                ) {
                    upsert = true
                }

                val url = storageClient.publicUrl(fileName)
                println("✅ Photo uploaded: $url")

                url

            } catch (e: Exception) {
                println("❌ Error uploading profile photo: ${e.message}")
                e.printStackTrace()
                throw Exception("Gagal upload foto: ${e.message}")
            }
        }
    }
}