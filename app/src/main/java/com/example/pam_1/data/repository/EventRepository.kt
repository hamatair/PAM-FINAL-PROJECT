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

    // --- UPLOAD GAMBAR (DIPERBAIKI: Terima ByteArray) ---
    suspend fun uploadEventImage(imageBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "event_${System.currentTimeMillis()}.jpg"
                println("📤 Uploading event photo: $fileName (${imageBytes.size} bytes)")

                val storageClient = supabase.storage["events"]
                storageClient.upload(path = fileName, data = imageBytes) {
                    upsert = true
                }

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

        val queryColumns = Columns.raw("*, users(*), event_category_pivot(category_id, event_category(*))")

        return supabase.postgrest["event"]
            .select(columns = queryColumns) {
                order("created_at", order = Order.DESCENDING)
                range(start.toLong(), end.toLong())
            }
            .decodeList()
    }

    suspend fun getEventById(eventId: String): Event? {
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
        val createdEvent = supabase.postgrest["event"]
            .insert(event) { select() }
            .decodeSingle<Event>()

        val newEventId = createdEvent.eventId ?: throw Exception("Gagal membuat ID Event")
        val currentUserId = event.userId ?: supabase.auth.currentUserOrNull()?.id

        if (categoryIds.isNotEmpty()) {
            val pivots = categoryIds.map { catId ->
                EventCategoryPivot(
                    eventId = newEventId,
                    categoryId = catId,
                    userId = currentUserId
                )
            }
            supabase.postgrest["event_category_pivot"].insert(pivots)
        }
    }

    // --- UPDATE ---
    suspend fun updateEvent(eventId: String, event: Event, newCategoryIds: List<String>) {
        supabase.postgrest["event"].update(event) {
            filter { eq("event_id", eventId) }
        }

        supabase.postgrest["event_category_pivot"].delete {
            filter { eq("event_id", eventId) }
        }

        if (newCategoryIds.isNotEmpty()) {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: event.userId
                ?: throw Exception("User ID tidak ditemukan")

            val newPivots = newCategoryIds.map { catId ->
                EventCategoryPivot(
                    eventId = eventId,
                    categoryId = catId,
                    userId = currentUserId
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