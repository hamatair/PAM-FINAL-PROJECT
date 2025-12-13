package com.example.pam_1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.Event
import com.example.pam_1.data.model.EventCategory
import com.example.pam_1.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State Wrapper
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class EventViewModel(
    private val repository: EventRepository) : ViewModel() {

    // --- States ---
// State Utama
    private val _eventListState = MutableStateFlow<UiState<List<Event>>>(UiState.Idle)
    val eventListState: StateFlow<UiState<List<Event>>> = _eventListState.asStateFlow()

    private val _categoryListState = MutableStateFlow<List<EventCategory>>(emptyList())
    val categoryListState: StateFlow<List<EventCategory>> = _categoryListState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionState: StateFlow<UiState<String>> = _actionState.asStateFlow()

    // Variable Pagination (YANG HILANG SEBELUMNYA)
    private val _currentEvents = mutableListOf<Event>() // Penampung data sementara
    private var currentPage = 1
    private val pageSize = 5
    var isLastPage = false
    var isLoadingMore = false

    private val _eventDetailState = MutableStateFlow<UiState<Event?>>(UiState.Idle)
    val eventDetailState: StateFlow<UiState<Event?>> = _eventDetailState.asStateFlow()


    init {
        loadCategories()
        loadEvents(isRefresh = true) // Load awal
    }
    fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = repository.getCategories()
                _categoryListState.value = cats
            } catch (e: Exception) {
                // Handle error silent
                e.printStackTrace()
            }
        }
    }
    fun loadEvents(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _eventListState.value = UiState.Loading
                currentPage = 1
                isLastPage = false
                _currentEvents.clear() // Reset list saat refresh
            }

            if (!isRefresh && (isLastPage || isLoadingMore)) return@launch

            isLoadingMore = true

            try {
                // Panggil Repo dengan Parameter (Sudah sesuai sekarang)
                val newEvents = repository.getEvents(currentPage, pageSize)

                if (newEvents.size < pageSize) {
                    isLastPage = true
                }

                _currentEvents.addAll(newEvents)
                // Emit copy list baru agar Compose mendeteksi perubahan state
                _eventListState.value = UiState.Success(_currentEvents.toList())

                if (!isLastPage) currentPage++

            } catch (e: Exception) {
                if (isRefresh) {
                    _eventListState.value = UiState.Error("Gagal memuat: ${e.message}")
                }
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun getActiveEvents(): List<Event> {
        val currentList = (_eventListState.value as? UiState.Success)?.data ?: emptyList()
        return currentList.filter {
            it.eventStatus == "scheduled" || it.eventStatus == "in_progress"
        }
    }

    // --- CREATE LOGIC ---
    fun addEvent(
        userId: String,
        name: String,
        desc: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        selectedCategoryIds: List<String>,
        imageBytes: ByteArray? // Data gambar dari gallery picker
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                // 1. Upload Gambar jika ada
                var imageUrl: String? = null
                if (imageBytes != null) {
                    imageUrl = repository.uploadEventImage(imageBytes)
                }

                // 2. Buat Object Event
                val newEvent = Event(
                    userId = userId,
                    eventName = name,
                    eventDescription = desc,
                    eventDate = date,
                    startTime = startTime,
                    endTime = endTime,
                    eventLocation = location,
                    eventImageUrl = imageUrl // Masukkan URL hasil upload
                )

                // 3. Simpan ke DB
                repository.createEvent(newEvent, selectedCategoryIds)

                _actionState.value = UiState.Success("Event berhasil dibuat!")
                loadEvents() // Refresh data
            } catch (e: Exception) {
                _actionState.value = UiState.Error("Error: ${e.message}")
            }
        }
    }

    fun updateEvent(
        eventId: String,
        name: String,
        desc: String,
        location: String,
        date: String,
        startTime: String,
        endTime: String,
        selectedCategoryIds: List<String>,
        imageBytes: ByteArray?,
        // Asumsi: Anda sudah punya URL lama untuk event yang sudah ada, jika ingin replace/update gambar
        currentImageUrl: String?
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                var imageUrl = currentImageUrl
                // 1. Upload foto baru jika ada
                if (imageBytes != null) {
                    imageUrl = repository.uploadEventImage(imageBytes)
                }

                // 2. Buat objek Event yang sudah diupdate
                val updatedEvent = Event(
                    eventId = eventId,
                    userId = null, // Tidak diubah
                    eventName = name,
                    eventDescription = desc,
                    eventLocation = location,
                    eventDate = date,
                    startTime = startTime,
                    endTime = endTime,
                    eventImageUrl = imageUrl,
                    eventStatus = null, // Tidak diubah
                )

                // 3. Update data di repo
                repository.updateEvent(eventId, updatedEvent, selectedCategoryIds)

                _actionState.value = UiState.Success("Event berhasil diupdate!")
                loadEvents(isRefresh = true) // Refresh list event
            } catch (e: Exception) {
                _actionState.value = UiState.Error("Gagal update event: ${e.message}")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                repository.deleteEvent(eventId)
                _actionState.value = UiState.Success("Event berhasil dihapus!")
                loadEvents(isRefresh = true) // Refresh list event
            } catch (e: Exception) {
                _actionState.value = UiState.Error("Gagal menghapus event: ${e.message}")
            }
        }
    }

    fun resetActionState() { _actionState.value = UiState.Idle }

    // Fungsi Load Detail
    fun loadEventDetail(eventId: String) {
        viewModelScope.launch {
            _eventDetailState.value = UiState.Loading
            try {
                // Repository sudah diperbaiki sebelumnya untuk include users(*)
                val event = repository.getEventById(eventId)
                if (event != null) {
                    _eventDetailState.value = UiState.Success(event)
                } else {
                    _eventDetailState.value = UiState.Error("Event tidak ditemukan")
                }
            } catch (e: Exception) {
                _eventDetailState.value = UiState.Error("Gagal memuat detail: ${e.message}")
            }
        }
    }
    fun clearEventDetail() {
        _eventDetailState.value = UiState.Idle
    }
}