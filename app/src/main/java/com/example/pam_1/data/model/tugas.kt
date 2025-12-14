package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tugas(
    @SerialName("tugas_id")
    val id: String? = null, // Ini akan null saat Insert, tapi Repository menanganinya

    @SerialName("user_id")
    val userId: String? = null,

    val title: String,
    val description: String,
    val priority: String = "Medium",

    @SerialName("date")
    val deadline: String, // Format yyyy-MM-dd

    val time: String = "09:00",

    @SerialName("is_completed")
    val isCompleted: Boolean = false,

    @SerialName("image_uri")
    val imageUri: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)