package com.example.pam_1.data.repository

import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.Expense
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository {

    private val client = SupabaseClient.client

    /* ================= FETCH EXPENSES (READ) ================= */

    suspend fun fetchExpenses(): List<Expense> {
        return try {
            // Get current user ID
            val currentUser = client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id
                ?: throw Exception("You must be logged in to fetch expenses")

            // Fetch expenses for current user with server-side filter
            client.from("expenses")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<Expense>()
        } catch (e: Exception) {
            throw Exception("Failed to fetch expenses: ${e.message}")
        }
    }

    /* ================= GET EXPENSE BY ID ================= */

    suspend fun getExpenseById(expenseId: Int): Expense? {
        return try {
            val currentUser = client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id
                ?: throw Exception("You must be logged in")

            // Fetch specific expense by ID and user
            client.from("expenses")
                .select {
                    filter {
                        eq("id", expenseId)
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<Expense>()
                .firstOrNull()
        } catch (e: Exception) {
            throw Exception("Failed to fetch expense: ${e.message}")
        }
    }

    /* ================= UPLOAD IMAGE ================= */

    suspend fun uploadExpenseImage(file: File): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "expense_${System.currentTimeMillis()}.jpg"
                val bytes = file.readBytes()

                val storageClient = client.storage["expense_images"]
                storageClient.upload(path = fileName, data = bytes) {
                    upsert = true
                }

                // Return public URL
                storageClient.publicUrl(fileName)
            } catch (e: Exception) {
                throw Exception("Failed to upload image: ${e.message}")
            }
        }
    }

    /* ================= ADD EXPENSE ================= */

    suspend fun addExpense(
        title: String,
        amount: Long,
        category: String,
        imageUrl: String? = null
    ) {
        try {
            // Get current user ID
            val currentUser = client.auth.currentUserOrNull()
            val currentUserId = currentUser?.id
                ?: throw Exception("You must be logged in to add expense")

            // Get current date
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())

            // Create expense object
            val expense = Expense(
                userId = currentUserId,
                title = title,
                category = category,
                imageUrl = imageUrl,
                amount = amount,
                expenseDate = currentDate
            )

            // Insert to database
            client.from("expenses").insert(expense) {
                select()
            }
        } catch (e: Exception) {
            throw Exception("Failed to add expense: ${e.message}")
        }
    }

    /* ================= DELETE EXPENSE ================= */

    suspend fun deleteExpense(expenseId: Int) {
        try {
            client.from("expenses").delete {
                filter {
                    eq("id", expenseId)
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to delete expense: ${e.message}")
        }
    }
}
