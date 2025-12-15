package com.example.pam_1.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.Expense
import com.example.pam_1.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/* ================= UI STATE ================= */

sealed class ExpenseUiState {
    object Idle : ExpenseUiState()
    object Loading : ExpenseUiState()
    object Success : ExpenseUiState() // Added Success state for navigation
    data class Error(val message: String) : ExpenseUiState()
}

/* ================= DETAIL STATE ================= */

sealed class ExpenseDetailState {
    object Idle : ExpenseDetailState()
    object Loading : ExpenseDetailState()
    data class Success(val expense: Expense) : ExpenseDetailState()
    data class Error(val message: String) : ExpenseDetailState()
    object NotFound : ExpenseDetailState()
}

/* ================= VIEWMODEL ================= */

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    /* ----- UI STATE ----- */
    private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Idle)
    val uiState: StateFlow<ExpenseUiState> = _uiState

    /* ----- LIST EXPENSE (READ) ----- */
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    /* ----- DETAIL STATE ----- */
    private val _detailState = MutableStateFlow<ExpenseDetailState>(ExpenseDetailState.Idle)
    val detailState: StateFlow<ExpenseDetailState> = _detailState

    /* ----- LOADING STATE ----- */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var hasLoadedOnce = false // Track if initial load happened


    /* ================= FETCH EXPENSES (READ) ================= */

    fun fetchExpenses() {
        // ✅ Prevent multiple simultaneous fetches
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedExpenses = repository.fetchExpenses()
                _expenses.value = fetchedExpenses
                hasLoadedOnce = true
                _uiState.value = ExpenseUiState.Idle
            } catch (e: Exception) {
                // Only set error if this is initial load
                if (!hasLoadedOnce) {
                    _uiState.value = ExpenseUiState.Error(e.message ?: "Failed to fetch expenses")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /* ================= ADD EXPENSE ================= */

    fun addExpense(
            context: android.content.Context,
            title: String,
            amount: Long,
            category: String,
            imageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = ExpenseUiState.Loading

            try {
                // Upload image if exists
                var imageUrl: String? = null
                if (imageUri != null) {
                    // Convert Uri to File (suspend function)
                    val file = com.example.pam_1.utils.FileUtils.getFileFromUri(context, imageUri)
                    if (file != null) {
                        imageUrl = repository.uploadExpenseImage(file)
                        // Clean up temp file
                        file.delete()
                    }
                }

                // Add expense to database
                repository.addExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        imageUrl = imageUrl
                )

                // Refresh the list
                fetchExpenses()

                // Set Success state for navigation
                _uiState.value = ExpenseUiState.Success
            } catch (e: Exception) {
                _uiState.value = ExpenseUiState.Error(e.message ?: "Failed to add expense")
            }
        }
    }

    /* ================= DELETE EXPENSE ================= */

    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                fetchExpenses() // Refresh list
            } catch (e: Exception) {
                _uiState.value = ExpenseUiState.Error(e.message ?: "Failed to delete expense")
            }
        }
    }

    /* ================= GET EXPENSE DETAIL ================= */

    fun getExpenseDetail(expenseId: Int) {
        viewModelScope.launch {
            _detailState.value = ExpenseDetailState.Loading
            try {
                val expense = repository.getExpenseById(expenseId)
                _detailState.value =
                        if (expense != null) {
                            ExpenseDetailState.Success(expense)
                        } else {
                            ExpenseDetailState.NotFound
                        }
            } catch (e: Exception) {
                _detailState.value =
                        ExpenseDetailState.Error(e.message ?: "Failed to fetch expense detail")
            }
        }
    }

    /* ================= RESET UI STATE ================= */

    fun resetUiState() {
        _uiState.value = ExpenseUiState.Idle
    }

    fun resetDetailState() {
        _detailState.value = ExpenseDetailState.Idle
    }
}
