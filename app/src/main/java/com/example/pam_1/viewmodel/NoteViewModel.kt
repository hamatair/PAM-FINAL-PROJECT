package com.example.pam_1.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.Note
import com.example.pam_1.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    // =========================
    // STATE FLOWS
    // =========================
    private val _noteListState =
        MutableStateFlow<UiState<List<Note>>>(UiState.Idle)
    val noteListState: StateFlow<UiState<List<Note>>> =
        _noteListState.asStateFlow()

    private val _noteDetailState =
        MutableStateFlow<UiState<Note?>>(UiState.Idle)
    val noteDetailState: StateFlow<UiState<Note?>> =
        _noteDetailState.asStateFlow()

    private val _actionState =
        MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionState: StateFlow<UiState<String>> =
        _actionState.asStateFlow()


    // =========================
    // LOAD NOTES
    // =========================
    fun loadNotes() {
        viewModelScope.launch {
            _noteListState.value = UiState.Loading
            try {
                val notes = repository.getNotes()
                _noteListState.value = UiState.Success(notes)
            } catch (e: Exception) {
                _noteListState.value =
                    UiState.Error("Gagal memuat note: ${e.message}")
            }
        }
    }

    // =========================
    // LOAD DETAIL
    // =========================
    fun loadNoteDetail(noteId: Long) {
        viewModelScope.launch {
            _noteDetailState.value = UiState.Loading
            try {
                val note = repository.getNoteById(noteId)
                _noteDetailState.value = UiState.Success(note)
            } catch (e: Exception) {
                _noteDetailState.value =
                    UiState.Error("Gagal memuat detail note")
            }
        }
    }

    fun clearNoteDetail() {
        _noteDetailState.value = UiState.Idle
    }

    // =========================
    // ADD NOTE
    // =========================
    fun addNote(
        title: String,
        description: String,
        isPinned: Boolean,
        imageBytes: ByteArray? // Dari Galeri
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                // Upload Image
                val imageUrl = imageBytes?.let {
                    repository.uploadNoteImage(it)
                }

                val note = Note(
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    isPinned = isPinned
                )

                repository.createNote(note)
                _actionState.value = UiState.Success("Note berhasil dibuat")

            } catch (e: Exception) {
                Log.e("DEBUG_VM", "Error di addNote: ${e.message}", e)
                _actionState.value = UiState.Error("Gagal menambah note: ${e.message}")
            }
        }
    }

    // =========================
    // UPDATE NOTE (Termasuk Hapus Gambar Lama)
    // =========================
    fun updateNote(
        noteId: Long,
        title: String,
        description: String,
        isPinned: Boolean,
        imageBytes: ByteArray?, // Gambar baru dari Galeri
        currentImageUrl: String? // URL gambar lama dari DB
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading

            var newImageUrl: String? = currentImageUrl

            try {
                // Jika ada gambar baru yang diupload
                if (imageBytes != null) {
                    // 1. Hapus gambar lama (jika ada)
                    if (currentImageUrl != null) {
                        repository.deleteNoteImage(currentImageUrl)
                    }

                    // 2. Upload gambar baru
                    newImageUrl = repository.uploadNoteImage(imageBytes)
                }

                // Jika user memilih untuk menghapus gambar, perlu ada logika di sini (saat ini diasumsikan tidak bisa menghapus, hanya mengganti atau tidak mengganti)

                val updatedNote = Note(
                    id = noteId,
                    title = title,
                    description = description,
                    imageUrl = newImageUrl,
                    isPinned = isPinned,
                    userId = null, createdAt = null, updatedAt = null
                )

                repository.updateNote(noteId, updatedNote)
                _actionState.value = UiState.Success("Note berhasil diperbarui")

            } catch (e: Exception) {
                Log.e("DEBUG_VM", "Error di updateNote: ${e.message}", e)
                _actionState.value = UiState.Error("Gagal update note: ${e.message}")
            }
        }
    }

    // =========================
    // UPDATE PIN STATUS (Ringan)
    // =========================
    fun updatePinStatus(noteId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePinned(noteId, isPinned)

                // Refresh data list agar urutan berubah
                loadNotes()

                // Update state detail
                val currentDetail = _noteDetailState.value
                if (currentDetail is UiState.Success && currentDetail.data?.id == noteId) {
                    _noteDetailState.value = UiState.Success(
                        currentDetail.data.copy(isPinned = isPinned)
                    )
                }
            } catch (e: Exception) {
                Log.e("DEBUG_VM", "Gagal update pin: ${e.message}")
            }
        }
    }


    // =========================
    // DELETE NOTE (Termasuk Hapus Gambar)
    // =========================
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                // Ambil detail dulu untuk mendapatkan URL gambar
                val noteToDelete = repository.getNoteById(noteId)

                // 1. Hapus gambar dari Storage (jika ada)
                noteToDelete?.imageUrl?.let {
                    repository.deleteNoteImage(it)
                }

                // 2. Hapus entry dari PostgREST
                repository.deleteNote(noteId)

                _actionState.value =
                    UiState.Success("Note berhasil dihapus")
                loadNotes()
            } catch (e: Exception) {
                Log.e("DEBUG_VM", "Error di deleteNote: ${e.message}", e)
                _actionState.value =
                    UiState.Error("Gagal menghapus note: ${e.message}")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}