package com.example.pam_1.data.repository

import android.util.Log
import com.example.pam_1.data.model.Note
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepository(private val supabase: SupabaseClient) {

    // =========================
    // UPLOAD IMAGE
    // =========================
    suspend fun uploadNoteImage(bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val fileName = "note_${System.currentTimeMillis()}.jpg"
            val storageClient = supabase.storage["notes"]

            storageClient.upload(fileName, bytes) { upsert = true }
            storageClient.publicUrl(fileName)
        }

    // =========================
    // READ - LIST
    // =========================
    suspend fun getNotes(): List<Note> {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        return supabase.postgrest["notes"]
            .select {
                filter { eq("user_id", userId) }
                order("is_pinned", Order.DESCENDING)
                order("updated_at", Order.DESCENDING)
            }
            .decodeList()
    }

    // =========================
    // READ - DETAIL
    // =========================
    suspend fun getNoteById(noteId: Long): Note? {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        return supabase.postgrest["notes"]
            .select {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Note>()
            .firstOrNull()
    }

    // =========================
    // CREATE
    // =========================
    suspend fun createNote(note: Note) {
        val currentUser = supabase.auth.currentUserOrNull()
         Log.d("DEBUG_REPO", "Current User: $currentUser")

        val userId = currentUser?.id
            ?: throw Exception("User belum login")

        val noteToUpload = note.copy(
            userId = userId,
            id = null,
            createdAt = null,
            updatedAt = null
        )

        try {
            // Supabase pintar, dia akan mengabaikan field yang null (seperti id)
            // dan menggunakan default value (auto increment) dari database.
            supabase.postgrest["notes"].insert(noteToUpload)

             Log.d("DEBUG_REPO", "Insert Berhasil dengan Object Note!")
        } catch (e: Exception) {
             Log.e("DEBUG_REPO", "Insert GAGAL: ${e.message}", e)
            throw e
        }
    }


    // =========================
    // UPDATE
    // =========================
    suspend fun updateNote(noteId: Long, note: Note) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")
        val noteToUpdate = note.copy(
            userId = userId,
            id = noteId
        )

        try {
            supabase.postgrest["notes"].update(noteToUpdate) {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteRepository", "Gagal Update Note: ${e.message}")
            throw e
        }
    }

    // =========================
    // DELETE IMAGE (NEW)
    // =========================
    // Fungsi untuk menghapus file dari Supabase Storage berdasarkan URL-nya
    suspend fun deleteNoteImage(imageUrl: String) = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext
        val path = imageUrl.substringAfter("/notes/")
        try {
            supabase.storage["notes"].delete(listOf(path))
            Log.d("NoteRepository", "Gambar lama berhasil dihapus: $path")
        } catch (e: Exception) {
            Log.e("NoteRepository", "Gagal menghapus gambar lama: ${e.message}")
        }
    }

    // =========================
    // UPDATE PIN
    // =========================
    suspend fun updatePinned(noteId: Long, pinned: Boolean) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        try {
            supabase.postgrest["notes"].update(
                mapOf("is_pinned" to pinned)
            ) {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteRepository", "Gagal Update Pin: ${e.message}")
            throw e
        }
    }

    // =========================
    // DELETE
    // =========================
    suspend fun deleteNote(noteId: Long) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        supabase.postgrest["notes"]
            .delete {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
    }
}
