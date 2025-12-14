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
         Log.d("DEBUG_REPO", "Current User: $currentUser") // Boleh dihapus nanti

        val userId = currentUser?.id
            ?: throw Exception("User belum login")

        // SOLUSI: Jangan pakai Map manual. Pakai object Note langsung.
        // Kita copy object note yang dikirim, lalu isi userId-nya.
        // Biarkan id, createdAt, updatedAt tetap null agar Database yang mengisinya (Auto Increment).
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

        // Kita buat object baru untuk dikirim.
        // PENTING: Jangan kirim id di body update jika tidak perlu,
        // tapi supabase-kt butuh object Note yang valid.
        // Kita set updatedAt ke null agar trigger DB (jika ada) atau ignore berfungsi,
        // TAPI lebih aman kita kirim data yang ingin diubah saja jika pakai object.

        val noteToUpdate = note.copy(
            userId = userId,
            // Pastikan ID diset agar serialisasi lancar (meski filter pakai eq id)
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
            // Log error agar ketahuan jika gagal
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

        // Format URL Supabase: .../storage/v1/object/notes/nama_file.jpg
        // Kita hanya butuh 'nama_file.jpg' untuk path deletion

        // Asumsi bucket Anda bernama "notes" (sesuai log error Anda)
        val path = imageUrl.substringAfter("/notes/")

        try {
            // Perintah delete di bucket "notes"
            supabase.storage["notes"].delete(listOf(path))
            Log.d("NoteRepository", "Gambar lama berhasil dihapus: $path")
        } catch (e: Exception) {
            // Penting: Jika gagal hapus (misal file sudah tidak ada),
            // kita log error tapi TIDAK perlu throw exception agar proses update data tetap berjalan.
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
            // Gunakan mapOf untuk update parsial (hanya 1 kolom)
            // Ini biasanya LEBIH AMAN untuk switch toggle
            // Tapi pastikan value-nya tidak Any? yang membingungkan serializer.
            // Kita bungkus jadi JsonElement atau biarkan library menghandle primitive types.

            // CARA PALING AMAN: Update pakai Map string-to-primitive
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
