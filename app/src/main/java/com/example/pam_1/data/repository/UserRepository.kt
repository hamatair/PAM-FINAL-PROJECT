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
    private val profile_bucket = "profile"

    /**
     * Fetch user profile dari database public.users
     */
    suspend fun getCurrentUserProfile(): User {
        return withContext(Dispatchers.IO) {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("User tidak terautentikasi.")

            try {
                println("🔍 Fetching user profile for ID: $currentUserId")

                val response = supabase.postgrest[userTable]
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }

                val users = response.decodeList<User>()

                println("📊 Found ${users.size} user(s)")

                if (users.isEmpty()) {
                    throw Exception("Data user tidak ditemukan di database. Pastikan user dengan ID $currentUserId ada di tabel users.")
                }

                val user = users.first()
                println("✅ User profile loaded: ${user.username}")

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
     * Update profile user ke database public.users
     * ✅ FIX: Update + Fetch terpisah untuk menghindari RLS issue
     */
    suspend fun updateUserProfile(user: User): User {
        return withContext(Dispatchers.IO) {
            try {
                val userId = user.user_id ?: throw Exception("User ID tidak valid")

                println("🔄 Updating user profile for ID: $userId")
                println("📝 Photo URL to update: ${user.photo_profile}")

                // ✅ STEP 1: Buat map data untuk update (lebih eksplisit)
                val updateData = mapOf(
                    "username" to user.username,
                    "full_name" to user.full_name,
                    "phone_number" to user.phone_number,
                    "bio" to user.bio,
                    "photo_profile" to user.photo_profile // Pastikan ini ter-include
                )

                // Update tanpa expect return data
                supabase.postgrest[userTable]
                    .update(updateData) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                println("✅ Update executed successfully")
                println("✅ Photo profile updated to: ${user.photo_profile}")

                // ✅ STEP 2: Fetch data terbaru setelah update
                val response = supabase.postgrest[userTable]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                val updatedUsers = response.decodeList<User>()

                if (updatedUsers.isEmpty()) {
                    throw Exception("User tidak ditemukan setelah update")
                }

                val updatedUser = updatedUsers.first()
                println("✅ User profile fetched after update: ${updatedUser.username}")

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

                val storageClient = supabase.storage[profile_bucket]

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

    /**
     * Hapus foto profile dari Supabase Storage
     * ✅ NEW: Fungsi untuk menghapus foto dari bucket
     */
    suspend fun deleteProfilePhoto(photoUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Jika URL adalah default avatar, skip penghapusan
                if (photoUrl == DEFAULT_AVATAR || photoUrl.contains("default.png")) {
                    println("⏭️ Skipping deletion of default avatar")
                    return@withContext true
                }

                // Extract filename dari URL
                // URL format: https://.../storage/v1/object/public/profile/profile_userid.jpg
                val fileName = photoUrl.substringAfterLast("/").substringBefore("?")

                if (fileName.isBlank()) {
                    println("⚠️ Invalid filename from URL: $photoUrl")
                    return@withContext false
                }

                println("🗑️ Deleting profile photo: $fileName")

                val storageClient = supabase.storage[profile_bucket]

                storageClient.delete(fileName)

                println("✅ Photo deleted successfully: $fileName")
                true

            } catch (e: Exception) {
                println("❌ Error deleting profile photo: ${e.message}")
                e.printStackTrace()
                // Tidak throw exception karena ini bukan critical error
                // Return false untuk indicate kegagalan
                false
            }
        }
    }
}