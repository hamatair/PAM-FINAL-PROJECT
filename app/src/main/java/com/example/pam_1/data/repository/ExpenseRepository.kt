package com.example.pam_1.data.repository

import com.example.pam_1.data.model.Expense
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository(private val supabaseClient: SupabaseClient) {

    /**
     * Fetch all expenses for the current user
     */
    suspend fun fetchExpenses(): List<Expense> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            supabaseClient
                .from("expenses")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Expense>()
        } catch (e: Exception) {
            throw Exception("Failed to fetch expenses: ${e.message}")
        }
    }

    /**
     * Add a new expense
     */
    suspend fun addExpense(
        title: String,
        amount: Long,
        category: String,
        imageUrl: String? = null
    ): Expense {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())

            val expense = Expense(
                userId = userId,
                title = title,
                amount = amount,
                category = category,
                expenseDate = currentDate,
                imageUrl = imageUrl
            )

            supabaseClient
                .from("expenses")
                .insert(expense) {
                    select()
                }
                .decodeSingle<Expense>()
        } catch (e: Exception) {
            throw Exception("Failed to add expense: ${e.message}")
        }
    }

    /**
     * Upload image to Supabase Storage and return the public URL
     */
    suspend fun uploadExpenseImage(file: File): String {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            val fileName = "${userId}_${System.currentTimeMillis()}_${file.name}"
            val bucket = supabaseClient.storage.from("expense_images")

            // Upload file
            bucket.upload(fileName, file.readBytes())

            // Get public URL
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            throw Exception("Failed to upload image: ${e.message}")
        }
    }

    /**
     * Delete an expense by ID
     */
    suspend fun deleteExpense(expenseId: Int) {
        try {
            supabaseClient
                .from("expenses")
                .delete {
                    filter {
                        eq("id", expenseId)
                    }
                }
        } catch (e: Exception) {
            throw Exception("Failed to delete expense: ${e.message}")
        }
    }

    /**
     * Get a single expense by ID
     */
    suspend fun getExpenseById(expenseId: Int): Expense? {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: throw Exception("User not authenticated")

            val expenses = supabaseClient
                .from("expenses")
                .select {
                    filter {
                        eq("id", expenseId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<Expense>()

            expenses.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(expense: Expense) {
        try {
            supabaseClient
                .from("expenses")
                .update(expense) {
                    filter {
                        eq("id", expense.id ?: 0)
                    }
                }
        } catch (e: Exception) {
            throw Exception("Failed to update expense: ${e.message}")
        }
    }
}
