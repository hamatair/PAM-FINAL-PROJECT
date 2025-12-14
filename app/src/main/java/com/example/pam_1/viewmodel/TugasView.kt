package com.example.pam_1.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.pam_1.data.model.Tugas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TugasViewModel : ViewModel() {
    private val _tugasList = mutableStateListOf<Tugas>()

    var selectedDate = mutableStateOf(getCurrentDate())
    var filterType = mutableStateOf("To Do")

    val filteredTugasList: List<Tugas>
        get() {
            return _tugasList
                .filter { it.deadline == selectedDate.value } // Filter berdasarkan tanggal yang dipilih di atas
                .filter {
                    if (filterType.value == "To Do") !it.isCompleted else it.isCompleted
                }
                .sortedByDescending {
                    when (it.priority) {
                        "High" -> 3
                        "Medium" -> 2
                        else -> 1
                    }
                }
        }

    // UPDATE: Tambah parameter 'time'
    fun addTugas(title: String, desc: String, deadline: String, time: String, priority: String, imageUri: Uri?) {
        if (title.isNotEmpty()) {
            _tugasList.add(Tugas(title = title, description = desc, deadline = deadline, time = time, priority = priority, imageUri = imageUri?.toString()))
            // FITUR UX: Otomatis pindah ke tanggal deadline agar user langsung liat tugasnya
            selectedDate.value = deadline
        }
    }

    // UPDATE: Tambah parameter 'time'
    fun updateTugas(id: Long, newTitle: String, newDesc: String, newDeadline: String, newTime: String, newPriority: String, newImageUri: Uri?) {
        val index = _tugasList.indexOfFirst { it.id == id }
        if (index != -1) {
            _tugasList[index] = _tugasList[index].copy(
                title = newTitle, description = newDesc, deadline = newDeadline, time = newTime, priority = newPriority,
                imageUri = newImageUri?.toString() ?: _tugasList[index].imageUri
            )
            // Otomatis pindah tanggal juga saat edit
            selectedDate.value = newDeadline
        }
    }

    fun toggleStatus(tugas: Tugas) {
        val index = _tugasList.indexOf(tugas)
        if (index != -1) {
            _tugasList[index] = _tugasList[index].copy(isCompleted = !tugas.isCompleted)
        }
    }

    fun removeTugas(tugas: Tugas) {
        _tugasList.remove(tugas)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }
}