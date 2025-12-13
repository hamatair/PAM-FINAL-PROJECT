package com.example.pam_1.data.repository

import com.example.pam_1.data.model.Event
import com.example.pam_1.data.model.EventCategory
import com.example.pam_1.data.model.EventCategoryPivot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventRepository(private val supabase: SupabaseClient) {

    // --- UPLOAD GAMBAR ---
    suspend fun uploadEventImage(bytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "event_${System.currentTimeMillis()}.jpg"
                println("📤 Uploading event photo: $fileName")

                val storageClient = supabase.storage["events"]
                storageClient.upload(path = fileName, data = bytes) { upsert = true }

                storageClient.publicUrl(fileName)
            } catch (e: Exception) {
                throw Exception("Gagal upload foto: ${e.message}")
            }
        }
    }

    // --- READ ---
    suspend fun getCategories(): List<EventCategory> {
        return supabase.postgrest["event_category"].select().decodeList()
    }

    suspend fun getEvents(page: Int, pageSize: Int): List<Event> {
        val start = (page - 1) * pageSize
        val end = start + pageSize - 1

        // PERBAIKAN 1: Tambahkan "users(*)" agar data pembuat event ikut terambil
        val queryColumns = Columns.raw("*, users(*), event_category_pivot(category_id, event_category(*))")

        return supabase.postgrest["event"]
            .select(columns = queryColumns) {
                order("created_at", order = Order.DESCENDING)
                range(start.toLong(), end.toLong())
            }
            .decodeList()
    }

    suspend fun getEventById(eventId: String): Event? {
        // PERBAIKAN 1: Tambahkan "users(*)" di sini juga
        val queryColumns = Columns.raw("*, users(*), event_category_pivot(category_id, event_category(*))")

        return supabase.postgrest["event"]
            .select(columns = queryColumns) {
                filter { eq("event_id", eventId) }
            }
            .decodeList<Event>()
            .firstOrNull()
    }

    // --- CREATE ---
    suspend fun createEvent(event: Event, categoryIds: List<String>) {
        // 1. Insert ke tabel Event
        val createdEvent = supabase.postgrest["event"]
            .insert(event) { select() }
            .decodeSingle<Event>()

        val newEventId = createdEvent.eventId ?: throw Exception("Gagal membuat ID Event")
        // Ambil User ID dari event yang dikirim (pastikan AddEventScreen mengirim userId)
        val currentUserId = event.userId ?: supabase.auth.currentUserOrNull()?.id

        // 2. Insert ke tabel Pivot (Many-to-Many)
        if (categoryIds.isNotEmpty()) {
            val pivots = categoryIds.map { catId ->
                EventCategoryPivot(
                    eventId = newEventId,
                    categoryId = catId,
                    userId = currentUserId // <--- PERBAIKAN 2: Wajib isi user_id agar lolos RLS
                )
            }
            supabase.postgrest["event_category_pivot"].insert(pivots)
        }
    }

    // --- UPDATE ---
    suspend fun updateEvent(eventId: String, event: Event, newCategoryIds: List<String>) {
        // 1. Update data tabel Event
        supabase.postgrest["event"].update(event) {
            filter { eq("event_id", eventId) }
        }

        // 2. Hapus Pivot Lama
        supabase.postgrest["event_category_pivot"].delete {
            filter { eq("event_id", eventId) }
        }

        // 3. Masukkan Pivot Baru
        if (newCategoryIds.isNotEmpty()) {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: event.userId
                ?: throw Exception("User ID tidak ditemukan")

            val newPivots = newCategoryIds.map { catId ->
                EventCategoryPivot(
                    eventId = eventId,
                    categoryId = catId,
                    userId = currentUserId // Sudah benar (ada userId)
                )
            }
            supabase.postgrest["event_category_pivot"].insert(newPivots)
        }
    }

    // --- DELETE ---
    suspend fun deleteEvent(eventId: String) {
        supabase.postgrest["event"].delete {
            filter { eq("event_id", eventId) }
        }
    }
}