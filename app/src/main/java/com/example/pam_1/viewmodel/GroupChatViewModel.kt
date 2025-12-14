package com.example.pam_1.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.GroupMessage
import com.example.pam_1.data.repository.GroupChatRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GroupChatViewModel(context: Context) : ViewModel() {
    private val repository = GroupChatRepository(context)

    // UI State
    var uiState: ChatUIState by mutableStateOf(ChatUIState.Idle)
        private set

    // Messages list (mutable to support realtime updates)
    val messages = mutableStateListOf<GroupMessage>()

    // Pagination state
    var hasMoreMessages by mutableStateOf(true)
        private set

    private var currentGroupId: Long? = null
    private var isLoadingMore = false

    /** Initialize chat for a group */
    fun initializeChat(groupId: Long) {
        if (currentGroupId == groupId) return

        currentGroupId = groupId
        messages.clear()
        hasMoreMessages = true

        // Load initial messages
        loadMessages(groupId)

        // Subscribe to realtime updates
        subscribeToMessages(groupId)
    }

    /** Load messages with pagination */
    fun loadMessages(groupId: Long, loadMore: Boolean = false) {
        if (isLoadingMore && loadMore) return

        if (loadMore) {
            isLoadingMore = true
        } else {
            uiState = ChatUIState.Loading
        }

        viewModelScope.launch {
            val offset = if (loadMore) messages.size else 0

            repository
                    .getMessages(groupId, limit = 50, offset = offset)
                    .onSuccess { newMessages ->
                        if (loadMore) {
                            messages.addAll(newMessages)
                            hasMoreMessages = newMessages.size == 50
                            isLoadingMore = false
                        } else {
                            messages.clear()
                            messages.addAll(newMessages)
                            hasMoreMessages = newMessages.size == 50
                            uiState = ChatUIState.Idle
                        }
                    }
                    .onFailure { error ->
                        uiState = ChatUIState.Error(error.message ?: "Failed to load messages")
                        isLoadingMore = false
                    }
        }
    }

    /** Subscribe to realtime message updates */
    private fun subscribeToMessages(groupId: Long) {
        viewModelScope.launch {
            try {
                repository
                        .subscribeToMessages(groupId)
                        .catch { e ->
                            // Handle subscription error gracefully
                            println("Realtime subscription error: ${e.message}")
                            e.printStackTrace()
                            // Don't show error to user, just log it
                            // Chat will still work without realtime (manual refresh)
                        }
                        .collect { newMessage ->
                            // Add new message to the top (newest first)
                            messages.add(0, newMessage)
                        }
            } catch (e: Exception) {
                // Catch any initialization errors
                println("Failed to initialize realtime: ${e.message}")
                e.printStackTrace()
                // Don't crash the app, realtime is optional feature
            }
        }
    }

    /** Send a text message */
    fun sendMessage(groupId: Long, content: String, replyTo: Long? = null) {
        if (content.isBlank()) return

        uiState = ChatUIState.Sending

        viewModelScope.launch {
            repository
                    .sendMessage(groupId, content.trim(), replyTo)
                    .onSuccess {
                        uiState = ChatUIState.Idle
                        // Message will appear via realtime subscription
                    }
                    .onFailure { error ->
                        uiState = ChatUIState.Error(error.message ?: "Failed to send message")
                    }
        }
    }

    /** Send an image message */
    fun sendImageMessage(
            groupId: Long,
            imageUri: Uri,
            caption: String? = null,
            replyTo: Long? = null
    ) {
        uiState = ChatUIState.UploadingImage(0f)

        viewModelScope.launch {
            repository
                    .sendImageMessage(groupId, imageUri, caption, replyTo)
                    .onSuccess {
                        uiState = ChatUIState.Idle
                        // Message will appear via realtime subscription
                    }
                    .onFailure { error ->
                        uiState = ChatUIState.Error(error.message ?: "Failed to send image")
                    }
        }
    }

    /** Get image URL for display */
    fun getImageUrl(path: String): String {
        return repository.getImageUrl(path)
    }

    /** Get attachments for a message */
    suspend fun getAttachments(messageId: Long) = repository.getAttachments(messageId)

    /** Edit a message */
    fun editMessage(messageId: Long, newContent: String) {
        if (newContent.isBlank()) return

        uiState = ChatUIState.Loading

        viewModelScope.launch {
            repository
                    .editMessage(messageId, newContent.trim())
                    .onSuccess { updatedMessage ->
                        // Update message in list
                        val index = messages.indexOfFirst { it.id == messageId }
                        if (index != -1) {
                            messages[index] = updatedMessage
                        }
                        uiState = ChatUIState.Success("Message edited")
                    }
                    .onFailure { error ->
                        uiState = ChatUIState.Error(error.message ?: "Failed to edit message")
                    }
        }
    }

    /** Delete a message */
    fun deleteMessage(messageId: Long) {
        uiState = ChatUIState.Loading

        viewModelScope.launch {
            repository
                    .deleteMessage(messageId)
                    .onSuccess {
                        // Remove message from list
                        messages.removeIf { it.id == messageId }
                        uiState = ChatUIState.Success("Message deleted")
                    }
                    .onFailure { error ->
                        uiState = ChatUIState.Error(error.message ?: "Failed to delete message")
                    }
        }
    }

    /** Load more messages (pagination) */
    fun loadMoreMessages() {
        currentGroupId?.let { groupId ->
            if (hasMoreMessages && !isLoadingMore) {
                loadMessages(groupId, loadMore = true)
            }
        }
    }

    /** Reset UI state */
    fun resetState() {
        uiState = ChatUIState.Idle
    }
}

// Chat UI States
sealed class ChatUIState {
    object Idle : ChatUIState()
    object Loading : ChatUIState()
    object Sending : ChatUIState()
    data class UploadingImage(val progress: Float) : ChatUIState()
    data class Success(val message: String) : ChatUIState()
    data class Error(val message: String) : ChatUIState()
}
