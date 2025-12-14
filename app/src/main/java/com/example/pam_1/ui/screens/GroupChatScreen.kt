package com.example.pam_1.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.GroupMessage
import com.example.pam_1.ui.components.MessageBubble
import com.example.pam_1.utils.ChatDateTimeFormatter
import com.example.pam_1.viewmodel.ChatUIState
import com.example.pam_1.viewmodel.GroupChatViewModel
import com.example.pam_1.viewmodel.GroupChatViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(navController: NavController, groupId: Long) {
    val context = LocalContext.current
    val viewModel: GroupChatViewModel = viewModel(factory = GroupChatViewModelFactory(context))

    val uiState = viewModel.uiState
    val messages = viewModel.messages
    val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

    var messageText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var replyToMessage by remember { mutableStateOf<GroupMessage?>(null) }

    // Track message attachments (messageId -> imageUrl)
    var messageAttachments by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    // Message context menu
    var selectedMessage by remember { mutableStateOf<GroupMessage?>(null) }
    var showMessageMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    // Fullscreen image viewer
    var showImageViewer by remember { mutableStateOf(false) }
    var viewerImageUrl by remember { mutableStateOf<String?>(null) }

    // Image picker
    val imagePickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri: Uri? ->
                uri?.let {
                    selectedImage = it
                    showImagePreview = true
                }
            }

    // Lazy list state for scroll control
    val listState = rememberLazyListState()

    // Initialize chat on first load
    LaunchedEffect(groupId) { viewModel.initializeChat(groupId) }

    // Fetch attachments for image messages
    LaunchedEffect(messages.size) {
        try {
            val imageMessages = messages.filter { it.messageType == "image" && it.id != null }
            imageMessages.forEach { message ->
                if (!messageAttachments.containsKey(message.id)) {
                    try {
                        // Fetch attachment
                        viewModel.getAttachments(message.id!!).onSuccess { attachments ->
                            if (attachments.isNotEmpty()) {
                                val imagePath = attachments.first().filePath
                                val imageUrl = viewModel.getImageUrl(imagePath)
                                messageAttachments = messageAttachments + (message.id!! to imageUrl)
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but don't crash
                        println("Error fetching attachment for message ${message.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // Don't crash on error
            println("Error processing attachments: ${e.message}")
        }
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is ChatUIState.Success -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is ChatUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Group Chat") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Reply indicator
            replyToMessage?.let { replyMsg ->
                Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replying to", style = MaterialTheme.typography.labelSmall)
                            Text(
                                    replyMsg.content ?: "Image",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                            )
                        }
                        IconButton(onClick = { replyToMessage = null }) {
                            Icon(Icons.Default.Close, "Cancel reply")
                        }
                    }
                }
            }

            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true, // Newest messages at bottom
                        contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                            items = messages,
                            key = { message ->
                                "${message.id}_${message.createdAt}_${message.senderId}"
                            }
                    ) { message ->
                        val isOwnMessage = message.senderId == currentUserId

                        // Get image URL if message has image
                        val imageUrl =
                                if (message.messageType == "image" && message.id != null) {
                                    messageAttachments[message.id]
                                } else null

                        MessageBubble(
                                message = message,
                                isOwnMessage = isOwnMessage,
                                imageUrl = imageUrl,
                                onLongPress = { msg ->
                                    selectedMessage = msg
                                    showMessageMenu = true
                                },
                                onImageClick = { url ->
                                    viewerImageUrl = url
                                    showImageViewer = true
                                }
                        )
                    }

                    // Loading indicator for pagination
                    if (viewModel.hasMoreMessages) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMoreMessages() }
                            Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                    }
                }

                // Loading overlay
                if (uiState is ChatUIState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Message input area
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image picker button
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, "Add image")
                    }

                    // Text input
                    OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4
                    )

                    // Send button
                    FilledIconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(groupId, messageText, replyToMessage?.id)
                                    messageText = ""
                                    replyToMessage = null
                                }
                            },
                            enabled = messageText.isNotBlank() && uiState !is ChatUIState.Sending
                    ) {
                        if (uiState is ChatUIState.Sending) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                }
            }
        }
    }

    // Message context menu
    if (showMessageMenu && selectedMessage != null) {
        AlertDialog(
                onDismissRequest = { showMessageMenu = false },
                title = { Text("Message Actions") },
                text = {
                    Column {
                        // Reply
                        TextButton(
                                onClick = {
                                    replyToMessage = selectedMessage
                                    showMessageMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Reply, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reply")
                        }

                        // Edit (only own messages within time window)
                        if (selectedMessage?.senderId == currentUserId) {
                            val canEdit =
                                    selectedMessage?.createdAt?.let {
                                        ChatDateTimeFormatter.isWithinEditWindow(it)
                                    }
                                            ?: false

                            if (canEdit) {
                                TextButton(
                                        onClick = {
                                            editText = selectedMessage?.content ?: ""
                                            showEditDialog = true
                                            showMessageMenu = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit")
                                }
                            }

                            // Delete (own messages)
                            TextButton(
                                    onClick = {
                                        selectedMessage?.id?.let { viewModel.deleteMessage(it) }
                                        showMessageMenu = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Delete")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMessageMenu = false }) { Text("Cancel") }
                }
        )
    }

    // Edit message dialog
    if (showEditDialog && selectedMessage != null) {
        AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Message") },
                text = {
                    OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                selectedMessage?.id?.let { viewModel.editMessage(it, editText) }
                                showEditDialog = false
                            }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                }
        )
    }

    // Image preview dialog
    if (showImagePreview && selectedImage != null) {
        AlertDialog(
                onDismissRequest = {
                    showImagePreview = false
                    selectedImage = null
                },
                title = { Text("Send Image") },
                text = {
                    Column {
                        // TODO: Show image preview
                        Text("Image selected. Add caption?")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = { Text("Caption (optional)") },
                                modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                            onClick = {
                                selectedImage?.let { uri ->
                                    viewModel.sendImageMessage(
                                            groupId,
                                            uri,
                                            messageText.ifBlank { null },
                                            replyToMessage?.id
                                    )
                                    selectedImage = null
                                    messageText = ""
                                    replyToMessage = null
                                    showImagePreview = false
                                }
                            }
                    ) {
                        if (uiState is ChatUIState.UploadingImage) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showImagePreview = false
                                selectedImage = null
                            }
                    ) { Text("Cancel") }
                }
        )
    }

    // Fullscreen Image Viewer
    if (showImageViewer && viewerImageUrl != null) {
        Dialog(onDismissRequest = { showImageViewer = false }) {
            Box(
                    modifier =
                            Modifier.fillMaxSize().background(Color.Black).clickable {
                                showImageViewer = false
                            }
            ) {
                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)

                    val maxX = (context.resources.displayMetrics.widthPixels * (scale - 1) / 2)
                    val maxY = (context.resources.displayMetrics.heightPixels * (scale - 1) / 2)

                    offsetX = (offsetX + offsetChange.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + offsetChange.y).coerceIn(-maxY, maxY)
                }

                // Reset when closed
                LaunchedEffect(showImageViewer) {
                    if (!showImageViewer) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }

                AsyncImage(
                        model = viewerImageUrl,
                        contentDescription = "Fullscreen image",
                        modifier =
                                Modifier.fillMaxSize()
                                        .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                        )
                                        .transformable(state = state),
                        contentScale = ContentScale.Fit
                )

                // Close button
                IconButton(
                        onClick = { showImageViewer = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
