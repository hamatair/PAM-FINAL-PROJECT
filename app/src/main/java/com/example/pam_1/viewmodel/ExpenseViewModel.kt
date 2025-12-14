package com.example.pam_1.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.Expense
import com.example.pam_1.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/* ================= UI STATE ================= */

sealed class ExpenseUiState {
    object Idle : ExpenseUiState()
    object Loading : ExpenseUiState()
    data class Error(val message: String) : ExpenseUiState()
    // REMOVED: Success - moved to navigation event
}

/* ================= NAVIGATION EVENT ================= */

sealed class ExpenseNavigationEvent {
    object NavigateBack : ExpenseNavigationEvent()
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

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    /* ----- UI STATE ----- */
    private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Idle)
    val uiState: StateFlow<ExpenseUiState> = _uiState

    /* ----- NAVIGATION EVENT (SharedFlow for one-time events) ----- */
    private val _navigationEvent = MutableSharedFlow<ExpenseNavigationEvent>()
    val navigationEvent: SharedFlow<ExpenseNavigationEvent> = _navigationEvent.asSharedFlow()

    /* ----- LIST EXPENSE (READ) ----- */
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    /* ----- DETAIL STATE ----- */
    private val _detailState = MutableStateFlow<ExpenseDetailState>(ExpenseDetailState.Idle)
    val detailState: StateFlow<ExpenseDetailState> = _detailState

    init {
        // Fetch expenses when ViewModel is created
        fetchExpenses()
    }

    /* ================= FETCH EXPENSES (READ) ================= */

    fun fetchExpenses() {
        viewModelScope.launch {
            try {
                val fetchedExpenses = repository.fetchExpenses()
                _expenses.value = fetchedExpenses
                _uiState.value = ExpenseUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ExpenseUiState.Error(
                    e.message ?: "Failed to fetch expenses"
                )
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
                    // Convert Uri to File
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

                // IMPORTANT: Emit navigation event ONCE (not via state)
                _navigationEvent.emit(ExpenseNavigationEvent.NavigateBack)
                
                // Reset to Idle (not Success - no recomposition trigger)
                _uiState.value = ExpenseUiState.Idle

            } catch (e: Exception) {
                _uiState.value = ExpenseUiState.Error(
                    e.message ?: "Failed to add expense"
                )
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
                _uiState.value = ExpenseUiState.Error(
                    e.message ?: "Failed to delete expense"
                )
            }
        }
    }

    /* ================= GET EXPENSE DETAIL ================= */

    fun getExpenseDetail(expenseId: Int) {
        viewModelScope.launch {
            _detailState.value = ExpenseDetailState.Loading
            try {
                val expense = repository.getExpenseById(expenseId)
                _detailState.value = if (expense != null) {
                    ExpenseDetailState.Success(expense)
                } else {
                    ExpenseDetailState.NotFound
                }
            } catch (e: Exception) {
                _detailState.value = ExpenseDetailState.Error(
                    e.message ?: "Failed to fetch expense detail"
                )
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
