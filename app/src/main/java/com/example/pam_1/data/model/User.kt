package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_AVATAR =
    "https://jhrbjirccxuhtzygwzgx.supabase.co/storage/v1/object/public/profile/avatar/default.png"

@Serializable
data class User(
    // ✅ CRITICAL: Database column is "user_id", NOT "id"
    // Property name: user_id
    // Database column name: user_id
    @SerialName("user_id")
    val user_id: String? = null,

    @SerialName("email")
    val email: String = "",

    @SerialName("username")
    val username: String = "",

    @SerialName("full_name")
    val full_name: String = "",

    @SerialName("phone_number")
    val phone_number: String = "",

    @SerialName("photo_profile")
    val photo_profile: String? = DEFAULT_AVATAR,

    @SerialName("bio")
    val bio: String? = null,

    @SerialName("created_at")
    val created_at: String? = null,

    @SerialName("updated_at")
    val updated_at: String? = null
)