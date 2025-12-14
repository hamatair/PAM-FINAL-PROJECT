package com.example.pam_1.viewmodel

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
    // STATE LIST NOTE
    // =========================
    private val _noteListState =
        MutableStateFlow<UiState<List<Note>>>(UiState.Idle)
    val noteListState: StateFlow<UiState<List<Note>>> =
        _noteListState.asStateFlow()

    // =========================
    // STATE DETAIL NOTE
    // =========================
    private val _noteDetailState =
        MutableStateFlow<UiState<Note?>>(UiState.Idle)
    val noteDetailState: StateFlow<UiState<Note?>> =
        _noteDetailState.asStateFlow()

    // =========================
    // STATE ACTION (ADD / UPDATE / DELETE)
    // =========================
    private val _actionState =
        MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionState: StateFlow<UiState<String>> =
        _actionState.asStateFlow()

    // =========================
    // LOAD ALL NOTES
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
    // LOAD NOTE BY ID (DETAIL)
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
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val imageUrl =
                    imageBytes?.let { repository.uploadNoteImage(it) }

                val note = Note(
                    id = null,
                    userId = null,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    isPinned = isPinned,
                    createdAt = null,
                    updatedAt = null
                )

                repository.createNote(note)
                _actionState.value =
                    UiState.Success("Note berhasil dibuat")
                loadNotes()

            } catch (e: Exception) {
                _actionState.value =
                    UiState.Error("Gagal menambah note: ${e.message}")
            }
        }
    }

    // =========================
    // UPDATE NOTE
    // =========================
    fun updateNote(
        noteId: Long,
        title: String,
        description: String,
        isPinned: Boolean,
        imageBytes: ByteArray?,
        currentImageUrl: String?
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val imageUrl =
                    imageBytes?.let { repository.uploadNoteImage(it) }
                        ?: currentImageUrl

                val updatedNote = Note(
                    id = noteId,
                    userId = null,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    isPinned = isPinned,
                    createdAt = null,
                    updatedAt = null
                )

                repository.updateNote(noteId, updatedNote)
                _actionState.value =
                    UiState.Success("Note berhasil diperbarui")
                loadNotes()

            } catch (e: Exception) {
                _actionState.value =
                    UiState.Error("Gagal update note: ${e.message}")
            }
        }
    }

    // =========================
    // DELETE NOTE
    // =========================
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                repository.deleteNote(noteId)
                _actionState.value =
                    UiState.Success("Note berhasil dihapus")
                loadNotes()
            } catch (e: Exception) {
                _actionState.value =
                    UiState.Error("Gagal menghapus note")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
