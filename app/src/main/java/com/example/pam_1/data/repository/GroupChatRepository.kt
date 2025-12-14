package com.example.pam_1.data.repository

import android.content.Context
import android.net.Uri
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.GroupMessage
import com.example.pam_1.data.model.MessageAttachment
import com.example.pam_1.utils.ImageCompressor
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupChatRepository(private val context: Context) {
        private val client = SupabaseClient.client

        /**
         * Subscribe to real-time message updates for a group
         * @param groupId Group ID to subscribe to
         * @return Flow of new messages
         */
        fun subscribeToMessages(groupId: Long): Flow<GroupMessage> {
                val channelId = "group_messages_$groupId"
                val channel = client.channel(channelId)

                return channel
                        .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                                table = "group_messages"
                        }
                        .map { action -> action.decodeRecord<GroupMessage>() }
                        .filter { message ->
                                // Filter by group_id client-side
                                message.groupId == groupId
                        }
        }

        /**
         * Get messages with pagination
         * @param groupId Group ID
         * @param limit Number of messages per page
         * @param offset Offset for pagination
         * @return List of messages (newest first)
         */
        suspend fun getMessages(
                groupId: Long,
                limit: Int = 20,
                offset: Int = 0
        ): Result<List<GroupMessage>> =
                withContext(Dispatchers.IO) {
                        try {
                                // Note: Not joining profiles here because sender_id references
                                // auth.users
                                // Profile info can be fetched separately if needed
                                val messages =
                                        client.from("group_messages")
                                                .select {
                                                        filter { eq("group_id", groupId) }
                                                        order("created_at", Order.DESCENDING)
                                                        limit(limit.toLong())
                                                        range(
                                                                offset.toLong() until
                                                                        (offset + limit).toLong()
                                                        )
                                                }
                                                .decodeList<GroupMessage>()

                                Result.success(messages)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }

        /**
         * Send a text message
         * @param groupId Group ID
         * @param content Message content
         * @param replyTo Optional message ID to reply to
         * @return Sent message
         */
        suspend fun sendMessage(
                groupId: Long,
                content: String,
                replyTo: Long? = null
        ): Result<GroupMessage> =
                withContext(Dispatchers.IO) {
                        try {
                                val userId =
                                        client.auth.currentUserOrNull()?.id
                                                ?: return@withContext Result.failure(
                                                        Exception("User not authenticated")
                                                )

                                val newMessage = buildJsonObject {
                                        put("group_id", groupId)
                                        put("sender_id", userId)
                                        put("content", content)
                                        put("message_type", "text")
                                        replyTo?.let { put("reply_to", it) }
                                }

                                val result =
                                        client.from("group_messages")
                                                .insert(newMessage) { select() }
                                                .decodeSingle<GroupMessage>()

                                Result.success(result)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }

        /**
         * Send an image message
         * @param groupId Group ID
         * @param imageUri Local image URI
         * @param caption Optional caption
         * @param replyTo Optional message ID to reply to
         * @return Sent message with attachment
         */
        suspend fun sendImageMessage(
                groupId: Long,
                imageUri: Uri,
                caption: String? = null,
                replyTo: Long? = null
        ): Result<GroupMessage> =
                withContext(Dispatchers.IO) {
                        try {
                                val userId =
                                        client.auth.currentUserOrNull()?.id
                                                ?: return@withContext Result.failure(
                                                        Exception("User not authenticated")
                                                )

                                // 1. Upload image
                                val imagePath =
                                        uploadImage(groupId, imageUri).getOrNull()
                                                ?: return@withContext Result.failure(
                                                        Exception("Failed to upload image")
                                                )

                                // 2. Create message
                                val newMessage = buildJsonObject {
                                        put("group_id", groupId)
                                        put("sender_id", userId)
                                        caption?.let { put("content", it) }
                                        put("message_type", "image")
                                        replyTo?.let { put("reply_to", it) }
                                }

                                val message =
                                        client.from("group_messages")
                                                .insert(newMessage) { select() }
                                                .decodeSingle<GroupMessage>()

                                // 3. Create attachment record
                                val messageId =
                                        message.id
                                                ?: return@withContext Result.failure(
                                                        Exception("Failed to get message ID")
                                                )

                                val attachment = buildJsonObject {
                                        put("message_id", messageId)
                                        put("file_path", imagePath)
                                        put("file_type", "image/jpeg")
                                }

                                client.from("message_attachments").insert(attachment)

                                Result.success(message)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }

        /**
         * Upload image to storage
         * @param groupId Group ID
         * @param imageUri Image URI
         * @return Path in storage bucket
         */
        private suspend fun uploadImage(groupId: Long, imageUri: Uri): Result<String> {
                try {
                        // Compress image
                        val compressedData =
                                ImageCompressor.compressImage(context, imageUri)
                                        ?: return Result.failure(
                                                Exception("Failed to compress image")
                                        )

                        // Generate unique filename
                        val timestamp = System.currentTimeMillis()
                        val filename = "$timestamp.jpg"
                        val path = "$groupId/$filename"

                        // Upload to storage
                        val bucket = client.storage.from("group_chat_images")
                        bucket.upload(path, compressedData)

                        // Return path
                        return Result.success(path)
                } catch (e: Exception) {
                        return Result.failure(e)
                }
        }

        /**
         * Get public URL for image
         * @param path Image path in storage
         * @return Public URL
         */
        fun getImageUrl(path: String): String {
                return client.storage.from("group_chat_images").publicUrl(path)
        }

        /**
         * Get attachments for a message
         * @param messageId Message ID
         * @return List of attachments
         */
        suspend fun getAttachments(messageId: Long): Result<List<MessageAttachment>> =
                withContext(Dispatchers.IO) {
                        try {
                                val attachments =
                                        client.from("message_attachments")
                                                .select { filter { eq("message_id", messageId) } }
                                                .decodeList<MessageAttachment>()

                                Result.success(attachments)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }

        /**
         * Edit a message
         * @param messageId Message ID
         * @param newContent New content
         * @return Updated message
         */
        suspend fun editMessage(messageId: Long, newContent: String): Result<GroupMessage> =
                withContext(Dispatchers.IO) {
                        try {
                                val updates = buildJsonObject {
                                        put("content", newContent)
                                        put("is_edited", true)
                                }

                                val result =
                                        client.from("group_messages")
                                                .update(updates) {
                                                        filter { eq("id", messageId) }
                                                        select()
                                                }
                                                .decodeSingle<GroupMessage>()

                                Result.success(result)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }

        /**
         * Delete a message
         * @param messageId Message ID
         */
        suspend fun deleteMessage(messageId: Long): Result<Unit> =
                withContext(Dispatchers.IO) {
                        try {
                                client.from("group_messages").delete {
                                        filter { eq("id", messageId) }
                                }

                                Result.success(Unit)
                        } catch (e: Exception) {
                                Result.failure(e)
                        }
                }
}
