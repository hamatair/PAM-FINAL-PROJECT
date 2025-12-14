package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMessage(
        @SerialName("id") val id: Long? = null,
        @SerialName("group_id") val groupId: Long = 0,
        @SerialName("sender_id") val senderId: String = "", // UUID
        @SerialName("content") val content: String? = null, // Nullable for image-only messages
        @SerialName("message_type") val messageType: String = "text", // text, image, file
        @SerialName("reply_to") val replyTo: Long? = null,
        @SerialName("is_edited") val isEdited: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,

        // Sender profile info (from joined query)
        @SerialName("sender_username") val senderUsername: String? = null
)

// Helper enum for message types
enum class MessageType(val value: String) {
    TEXT("text"),
    IMAGE("image"),
    FILE("file");

    companion object {
        fun fromString(value: String): MessageType {
            return values().find { it.value == value } ?: TEXT
        }
    }
}
