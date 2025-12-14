package com.example.pam_1.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    @SerialName("id")
    val id: Int? = null,

    @SerialName("user_id")
    val userId: String,

    @SerialName("title")
    val title: String,

    @SerialName("category")
    val category: String,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("amount")
    val amount: Long,

    @SerialName("expense_date")
    val expenseDate: String,

    @SerialName("created_at")
    val createdAt: String? = null
)
