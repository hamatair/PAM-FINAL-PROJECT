package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
        @SerialName("id") val id: String? = null,
        @SerialName("group_id") val groupId: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("role") val role: String = "member", // owner | moderator | member
        @SerialName("joined_at") val joinedAt: String? = null
)

// Helper enum for roles
enum class GroupRole(val value: String) {
    OWNER("owner"),
    MODERATOR("moderator"),
    MEMBER("member");

    companion object {
        fun fromString(value: String): GroupRole {
            return values().find { it.value == value } ?: MEMBER
        }
    }
}
