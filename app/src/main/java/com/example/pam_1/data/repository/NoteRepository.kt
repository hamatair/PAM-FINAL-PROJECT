package com.example.pam_1.data.repository

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
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        supabase.postgrest["notes"].insert(
            mapOf(
                "title" to note.title,
                "description" to note.description,
                "image_url" to note.imageUrl,
                "is_pinned" to note.isPinned,
                "user_id" to userId
            )
        )
    }


    // =========================
    // UPDATE
    // =========================
    suspend fun updateNote(noteId: Long, note: Note) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        supabase.postgrest["notes"]
            .update(note.copy(updatedAt = null)) {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
    }

    // =========================
    // UPDATE PIN
    // =========================
    suspend fun updatePinned(noteId: Long, pinned: Boolean) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("User belum login")

        supabase.postgrest["notes"]
            .update(mapOf("is_pinned" to pinned)) {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
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
