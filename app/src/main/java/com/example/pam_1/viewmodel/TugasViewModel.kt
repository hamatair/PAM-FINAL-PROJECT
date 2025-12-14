package com.example.pam_1.viewmodel

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

class TugasViewModel(private val repository: TugasRepository) : ViewModel() {

    private val _tugasList = mutableStateListOf<Tugas>()

    // Format Tanggal
    private val uiDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var selectedDate = mutableStateOf(getCurrentDateUi())
    var filterType = mutableStateOf("To Do")
    var errorMessage = mutableStateOf<String?>(null)

    init {
        loadTugas()
    }

    val filteredTugasList: List<Tugas>
        get() {
            val selectedDateInDbFormat = convertUiDateToDb(selectedDate.value)
            return _tugasList
                .filter { it.deadline == selectedDateInDbFormat }
                .filter {
                    if (filterType.value == "To Do") !it.isCompleted else it.isCompleted
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
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error loading tugas: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addTugas(title: String, desc: String, deadlineUi: String, time: String, priority: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                // DEBUGGING: Cek tanggal apa yang sebenarnya dikirim ke DB
                val deadlineDb = convertUiDateToDb(deadlineUi)
                Log.d("DEBUG_DATE", "UI Date: $deadlineUi -> DB Date: $deadlineDb")

                // Validasi format tanggal sebelum kirim (Pencegahan Error A)
                if (!deadlineDb.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    throw Exception("Format tanggal salah: $deadlineDb. Harusnya yyyy-MM-dd")
                }

                val newTugas = Tugas(
                    title = title,
                    description = desc,
                    deadline = deadlineDb,
                    time = time,
                    priority = priority,
                    imageUri = imageUri?.toString()
                )

                repository.createTugas(newTugas)

                Log.d("TugasViewModel", "Berhasil input data!")
                loadTugas()
                selectedDate.value = deadlineUi

            } catch (e: Exception) {
                // PENTING: Log error aslinya agar terbaca di Logcat
                Log.e("TugasViewModel", "GAGAL INPUT TUGAS: ${e.message}", e)

                // Opsional: Set pesan error ke variable agar bisa ditToast di UI
                errorMessage.value = "Gagal simpan: ${e.message}"
            }
        }
    }

    fun updateTugas(id: String, newTitle: String, newDesc: String, newDeadlineUi: String, newTime: String, newPriority: String, newImageUri: Uri?) {
        viewModelScope.launch {
            try {
                val deadlineDb = convertUiDateToDb(newDeadlineUi)
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
                    selectedDate.value = newDeadlineUi
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
                // Update UI optimistik (langsung berubah sebelum server respon)
                val index = _tugasList.indexOf(tugas)
                if (index != -1) {
                    _tugasList[index] = tugas.copy(isCompleted = newStatus)
                }

                // Update ke DB
                tugas.id?.let { repository.updateStatus(it, newStatus) }
                // Opsional: loadTugas() lagi untuk memastikan sinkron
            } catch (e: Exception) {
                Log.e("TugasViewModel", "Error toggle status: ${e.message}")
                loadTugas() // Revert jika gagal
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

    private fun getCurrentDateUi(): String {
        return uiDateFormat.format(Date())
    }

    private fun convertUiDateToDb(uiDate: String): String {
        return try {
            val date = uiDateFormat.parse(uiDate)
            dbDateFormat.format(date ?: Date())
        } catch (e: Exception) {
            uiDate // Return as is if parse fails
        }
    }
}