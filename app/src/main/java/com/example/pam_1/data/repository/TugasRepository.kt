package com.example.pam_1.data.repository

import android.util.Log
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.Tugas
import io.github.jan.supabase.auth.auth // <--- WAJIB IMPORT INI
import io.github.jan.supabase.postgrest.from

class TugasRepository {

    private val client = SupabaseClient.client

    // CREATE (INI BAGIAN YANG DIPERBAIKI)
    // TugasRepository.kt

    suspend fun createTugas(tugas: Tugas) {
        // 1. Ambil User ID
        val currentUser = client.auth.currentUserOrNull()
        val currentUserId = currentUser?.id

        if (currentUserId == null) {
            throw Exception("Anda harus login untuk membuat tugas.")
        }

        // 2. PERBAIKAN: Jangan pakai Map<String, Any>.
        // Gunakan .copy() untuk menyisipkan user_id ke object Tugas yang sudah ada.
        val tugasBaru = tugas.copy(
            userId = currentUserId // Masukkan ID user ke sini
        )

        // 3. Eksekusi Insert dengan Object Tugas langsung (bukan Map)
        try {
            client.from("tugas").insert(tugasBaru) {
                select()
            }
            Log.d("TugasRepository", "Insert Sukses!")
        } catch (e: Exception) {
            Log.e("TugasRepository", "Error Supabase: ${e.message}")
            throw e
        }
    }

    // READ
    suspend fun getTugas(): List<Tugas> {
        return client.from("tugas").select().decodeList<Tugas>()
    }

    // UPDATE
    suspend fun updateTugas(tugas: Tugas) {
        tugas.id?.let { id ->
            // Saat update, kita kirim objek tugas apa adanya (atau pakai Map jika mau aman)
            client.from("tugas").update(tugas) {
                filter { eq("tugas_id", id) }
                select()
            }
        }
    }

    // UPDATE STATUS
    suspend fun updateStatus(id: String, isCompleted: Boolean) {
        client.from("tugas").update(mapOf("is_completed" to isCompleted)) {
            filter { eq("tugas_id", id) }
            select()
        }
    }

    // DELETE
    suspend fun deleteTugas(id: String) {
        client.from("tugas").delete {
            filter { eq("tugas_id", id) }
            select()
        }
    }
}