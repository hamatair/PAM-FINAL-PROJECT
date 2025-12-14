package com.example.pam_1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pam_1.data.repository.TugasRepository

class TugasViewModelFactory(private val repository: TugasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TugasViewModel::class.java)) {
            return TugasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}