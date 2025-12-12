package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudyGroup(
        @SerialName("id") val id: String? = null,
        @SerialName("owner") val owner: String = "",
        @SerialName("name") val name: String = "",
        @SerialName("description") val description: String? = null,
        @SerialName("course") val course: String? = null,
        @SerialName("is_public") val isPublic: Boolean = false,
        @SerialName("image_url") val imageUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
)
