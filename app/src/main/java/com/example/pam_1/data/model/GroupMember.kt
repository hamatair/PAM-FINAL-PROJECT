package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
        @SerialName("id") val id: Long? = null, // Changed to Long (BIGINT)
        @SerialName("group_id") val groupId: Long = 0, // Changed to Long (BIGINT)
        @SerialName("user_id") val userId: String = "", // UUID stays as String
        @SerialName("role") val role: String = "member", // owner | moderator | member
        @SerialName("joined_at") val joinedAt: String? = null,

        // User profile info (from joined query with profiles table)
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("username") val username: String? = null,
        @SerialName("email") val email: String? = null
) {
    // Helper to get display name (fallback chain: full_name -> username -> email -> user_id)
    fun getDisplayName(): String {
        return fullName?.takeIf { it.isNotBlank() }
                ?: username?.takeIf { it.isNotBlank() }
                        ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                        ?: userId.take(8) // Last resort: show first 8 chars of UUID
    }
}

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
