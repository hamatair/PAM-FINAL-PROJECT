package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupInvite(
        @SerialName("id") val id: String? = null,
        @SerialName("group_id") val groupId: String = "",
        @SerialName("code") val code: String = "",
        @SerialName("code_hash") val codeHash: String? = null,
        @SerialName("created_by") val createdBy: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("max_uses") val maxUses: Int = 1,
        @SerialName("used_count") val usedCount: Int = 0,
        @SerialName("is_active") val isActive: Boolean = true,
        @SerialName("note") val note: String? = null
)
