package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageAttachment(
        @SerialName("id") val id: Long? = null,
        @SerialName("message_id") val messageId: Long = 0,
        @SerialName("file_path") val filePath: String = "",
        @SerialName("file_name") val fileName: String? = null,
        @SerialName("file_type") val fileType: String? = null, // MIME type
        @SerialName("file_size") val fileSize: Long? = null, // Size in bytes
        @SerialName("thumbnail_path") val thumbnailPath: String? = null,
        @SerialName("created_at") val createdAt: String? = null
)
