package com.example.pam_1.data.repository

import android.util.Log
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.Tugas
import io.github.jan.supabase.auth.auth // <--- WAJIB IMPORT INI
import io.github.jan.supabase.postgrest.from

class TugasRepository {

    private val client = SupabaseClient.client

    suspend fun createTugas(tugas: Tugas) {
        val currentUser = client.auth.currentUserOrNull()
        val currentUserId = currentUser?.id

        if (currentUserId == null) {
            throw Exception("Anda harus login untuk membuat tugas.")
        }

        val tugasBaru = tugas.copy(
            userId = currentUserId
        )

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

    suspend fun getTugas(): List<Tugas> {
        return client.from("tugas").select().decodeList<Tugas>()
    }

    suspend fun updateTugas(tugas: Tugas) {
        tugas.id?.let { id ->
            client.from("tugas").update(tugas) {
                filter { eq("tugas_id", id) }
                select()
            }
        }
    }

    suspend fun updateStatus(id: String, isCompleted: Boolean) {
        client.from("tugas").update(mapOf("is_completed" to isCompleted)) {
            filter { eq("tugas_id", id) }
            select()
        }
    }

    suspend fun deleteTugas(id: String) {
        client.from("tugas").delete {
            filter { eq("tugas_id", id) }
            select()
        }
    }


}
