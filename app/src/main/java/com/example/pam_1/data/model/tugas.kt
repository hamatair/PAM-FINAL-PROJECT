package com.example.pam_1.data.model

data class Tugas(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val deadline: String,
    val priority: String = "Medium", // Baru: High, Medium, Low
    val isCompleted: Boolean = false,
    val time: String = "09:00",
    val imageUri: String? = null
)