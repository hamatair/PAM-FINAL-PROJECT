package com.example.pam_1.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.GroupMessage
import com.example.pam_1.utils.ChatDateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
        message: GroupMessage,
        isOwnMessage: Boolean,
        imageUrl: String? = null,
        onLongPress: (GroupMessage) -> Unit = {},
        onImageClick: (String) -> Unit = {},
        showSenderInfo: Boolean = true,
        modifier: Modifier = Modifier
) {
        val bubbleColor =
                if (isOwnMessage) {
                        MaterialTheme.colorScheme.primaryContainer
                } else {
                        MaterialTheme.colorScheme.surfaceVariant
                }

        val textColor =
                if (isOwnMessage) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                }

        val topPadding = if (showSenderInfo) 16.dp else 2.dp

        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = topPadding,
                                        bottom = 2.dp
                                ),
                horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
                Column(
                        modifier =
                                Modifier.widthIn(max = 300.dp)
                                        .clip(
                                                RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart =
                                                                if (isOwnMessage) 16.dp else 4.dp,
                                                        bottomEnd =
                                                                if (isOwnMessage) 4.dp else 16.dp
                                                )
                                        )
                                        .background(bubbleColor)
                                        .combinedClickable(
                                                onClick = {},
                                                onLongClick = { onLongPress(message) }
                                        )
                                        .padding(12.dp),
                        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
                ) {
                        // Reply indicator (if replying to another message)
                        message.replyTo?.let {
                                Surface(
                                        color =
                                                MaterialTheme.colorScheme.surface.copy(
                                                        alpha = 0.5f
                                                ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                        Text(
                                                text = "↩️ Replying to message",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                modifier = Modifier.padding(6.dp)
                                        )
                                }
                        }

                        // Display sender name for messages from others
                        if (!isOwnMessage && showSenderInfo) {
                                val displayName =
                                        message.senderFullName
                                                ?: message.senderUsername
                                                        ?: message.senderId.take(
                                                        8
                                                ) // Fallback to sender ID

                                Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                )
                        }

                        // Image (if message contains image)
                        imageUrl?.let { url ->
                                AsyncImage(
                                        model = url,
                                        contentDescription = "Message image",
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .heightIn(max = 250.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .combinedClickable(
                                                                onClick = { onImageClick(url) }
                                                        ),
                                        contentScale = ContentScale.Crop
                                )

                                if (!message.content.isNullOrBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                }
                        }

                        // Message content (text)
                        message.content?.let { content ->
                                Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                )
                        }

                        // Timestamp and edited indicator
                        Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                message.createdAt?.let { timestamp ->
                                        Text(
                                                text =
                                                        ChatDateTimeFormatter.formatMessageTime(
                                                                timestamp
                                                        ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.7f),
                                                fontSize = 10.sp
                                        )
                                }

                                if (message.isEdited) {
                                        Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edited",
                                                modifier = Modifier.size(12.dp),
                                                tint = textColor.copy(alpha = 0.5f)
                                        )
                                }
                        }
                }
        }
}
