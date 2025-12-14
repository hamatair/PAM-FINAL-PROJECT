package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("user_id")  val userId: String? = null,
    @SerialName("event_name") val eventName: String,
    @SerialName("event_description") val eventDescription: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("event_start_time") val startTime: String,
    @SerialName("event_end_time") val endTime: String,
    @SerialName("event_location") val eventLocation: String,
    @SerialName("event_image_url") val eventImageUrl: String? = null,
    @SerialName("event_status") val eventStatus: String? = null,
    @SerialName("created_at") val createdAt: String? = null,

    @SerialName("users") val creator: User? = null,
    // Field Relasi (Read Only)
    @SerialName("event_category_pivot")
    val categoryPivots: List<EventCategoryPivot>? = null
) {
    // Helper: Ambil List ID Kategori (untuk UI Edit)
    val categoryIds: List<String>
        get() = categoryPivots?.map { it.categoryId } ?: emptyList()

    // Helper: Ambil List Nama Kategori (untuk UI List)
}

@Serializable
data class EventCategory(
    @SerialName("event_category_id") val categoryId: String,
    @SerialName("event_category") val categoryName: String
)

@Serializable
data class EventCategoryPivot(
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("category_id") val categoryId: String,
    @SerialName("event_category") val categoryDetail: EventCategory? = null,
    @SerialName("user_id") val userId: String? = null,
)