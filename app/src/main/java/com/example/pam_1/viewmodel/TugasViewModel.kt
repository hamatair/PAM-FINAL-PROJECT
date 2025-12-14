package com.example.pam_1.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.Tugas
import com.example.pam_1.data.repository.TugasRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.pam_1.data.model.repository.ImageRepository

class TugasViewModel(private val repository: TugasRepository) : ViewModel() {

    private val _tugasList = mutableStateListOf<Tugas>()
    private val imageRepository = ImageRepository()

    // Format Tanggal
    private val uiFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // DB date (untuk filter & query) -> default hari ini
    var selectedDateDb = mutableStateOf(getTodayDb())

    // UI date (hanya untuk tampilan)
    var selectedDateUi = mutableStateOf(formatDbToUi(selectedDateDb.value))

    // Filter default: Bahasa Indonesia sesuai FilterTabs
    var filterType = mutableStateOf("Belum Selesai")
    var errorMessage = mutableStateOf<String?>(null)

    init {
        loadTugas()
    }

    private fun getTodayDb(): String =
        dbFormat.format(Date())

    fun formatDbToUi(dbDate: String): String {
        return try {
            val d = dbFormat.parse(dbDate)
            if (d != null) uiFormat.format(d) else dbDate
        } catch (e: Exception) {
            Log.w("TugasViewModel", "formatDbToUi parse gagal for '$dbDate': ${e.message}")
            dbDate
        }
    }

    fun convertUiToDb(uiDate: String): String {
        return try {
            val d = uiFormat.parse(uiDate)
            if (d != null) dbFormat.format(d) else uiDate
        } catch (e: Exception) {
            Log.w("TugasViewModel", "convertUiToDb parse gagal for '$uiDate': ${e.message}")
            uiDate
        }
    }

    val filteredTugasList: List<Tugas>
        get() {
            Log.d("TugasViewModel", "Filtering for DB date = ${selectedDateDb.value}, filterType = '${filterType.value}'")
            return _tugasList
                .filter { it.deadline == selectedDateDb.value }
                .filter {
                    when (filterType.value) {
                        "Belum Selesai" -> !it.isCompleted
                        "Selesai" -> it.isCompleted
                        else -> true
                    }
                }
                .sortedByDescending {
                    when (it.priority) {
                        "Tinggi" -> 3
                        "Sedang" -> 2
                        "Rendah" -> 1
                        else -> 0
                    }
                }
        }

    fun loadTugas() {
        viewModelScope.launch {
            try {
                val result = repository.getTugas()
                _tugasList.clear()
                _tugasList.addAll(result)
                Log.d("TugasViewModel", "Loaded ${result.size} tugas")
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error loading tugas: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addTugas(
        context: Context,
        title: String,
        desc: String,
        deadlineUi: String,
        time: String,
        priority: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                val deadlineDb = convertUiToDb(deadlineUi)

                // 🔥 UPLOAD IMAGE JIKA ADA
                val uploadedImageUrl = imageUri?.let {
                    imageRepository.uploadTugasImage(context, it)
                }

                val newTugas = Tugas(
                    title = title,
                    description = desc,
                    deadline = deadlineDb,
                    time = time,
                    priority = priority,
                    imageUri = uploadedImageUrl
                )

                repository.createTugas(newTugas)
                loadTugas()

            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }

    fun updateTugas(id: String, newTitle: String, newDesc: String, newDeadlineUi: String, newTime: String, newPriority: String, newImageUri: Uri?) {
        viewModelScope.launch {
            try {
                val deadlineDb = convertUiToDb(newDeadlineUi)
                val oldTugas = _tugasList.find { it.id == id }

                val updatedTugas = oldTugas?.copy(
                    title = newTitle,
                    description = newDesc,
                    deadline = deadlineDb,
                    time = newTime,
                    priority = newPriority,
                    imageUri = newImageUri?.toString() ?: oldTugas.imageUri
                )

                if (updatedTugas != null) {
                    repository.updateTugas(updatedTugas)
                    loadTugas()
                    selectedDateDb.value = deadlineDb
                    selectedDateUi.value = formatDbToUi(deadlineDb)
                }
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error updating tugas: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun toggleStatus(tugas: Tugas) {
        viewModelScope.launch {
            try {
                val newStatus = !tugas.isCompleted
                val index = _tugasList.indexOf(tugas)
                if (index != -1) {
                    _tugasList[index] = tugas.copy(isCompleted = newStatus)
                }
                tugas.id?.let { repository.updateStatus(it, newStatus) }
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error toggle status: ${e.message}")
                loadTugas()
            }
        }
    }

    fun removeTugas(tugas: Tugas) {
        viewModelScope.launch {
            try {
                tugas.id?.let { repository.deleteTugas(it) }
                _tugasList.remove(tugas)
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error deleting tugas: ${e.message}")
            }
        }
    }
}
