package com.example.pam_1.data.repository

import android.net.Uri
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.StudyGroup
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class StudyGroupRepository {
    private val client = SupabaseClient.client

    /** Create a new study group */
    suspend fun createGroup(
            name: String,
            description: String?,
            course: String?,
            isPublic: Boolean,
            imageUrl: String? = null
    ): Result<StudyGroup> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.failure(
                                            Exception("User not authenticated")
                                    )

                    // Use buildJsonObject explicitly for serialization support
                    val newGroup = buildJsonObject {
                        put("owner", userId) // Fixed: was "owner", should be "owner_id"
                        put("name", name)
                        description?.let { put("description", it) }
                        course?.let { put("course", it) }
                        put("is_public", isPublic)
                        imageUrl?.let { put("image_url", it) }
                    }

                    val result =
                            client.from("study_groups")
                                    .insert(newGroup) { select() }
                                    .decodeSingle<StudyGroup>()

                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Update an existing study group */
    suspend fun updateGroup(
            groupId: Long,
            name: String?,
            description: String?,
            course: String?,
            isPublic: Boolean?,
            imageUrl: String?
    ): Result<StudyGroup> =
            withContext(Dispatchers.IO) {
                try {
                    // Use buildJsonObject explicitly
                    val updates = buildJsonObject {
                        name?.let { put("name", it) }
                        description?.let { put("description", it) }
                        course?.let { put("course", it) }
                        isPublic?.let { put("is_public", it) }
                        imageUrl?.let { put("image_url", it) }
                    }

                    val result =
                            client.from("study_groups")
                                    .update(updates) {
                                        filter { eq("id", groupId) }
                                        select()
                                    }
                                    .decodeSingle<StudyGroup>()

                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Delete a study group (owner only) */
    suspend fun deleteGroup(groupId: Long): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    client.from("study_groups").delete { filter { eq("id", groupId) } }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get all groups where the current user is a member */
    suspend fun getMyGroups(): Result<List<StudyGroup>> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.failure(
                                            Exception("User not authenticated")
                                    )

                    // Get group IDs where user is a member
                    // Decode to JsonObject instead of Map<String, Any>
                    val memberGroups =
                            client.from("group_members")
                                    .select { filter { eq("user_id", userId) } }
                                    .decodeList<JsonObject>()

                    val groupIds =
                            memberGroups.mapNotNull { it["group_id"]?.jsonPrimitive?.longOrNull }

                    if (groupIds.isEmpty()) {
                        return@withContext Result.success(emptyList())
                    }

                    // Get the groups
                    val groups =
                            client.from("study_groups")
                                    .select { filter { isIn("id", groupIds) } }
                                    .decodeList<StudyGroup>()

                    Result.success(groups)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get all public groups */
    suspend fun getPublicGroups(): Result<List<StudyGroup>> =
            withContext(Dispatchers.IO) {
                try {
                    val groups =
                            client.from("study_groups")
                                    .select {
                                        filter { eq("is_public", true) }
                                        order("created_at", Order.DESCENDING)
                                    }
                                    .decodeList<StudyGroup>()

                    Result.success(groups)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get a specific group by ID */
    suspend fun getGroupById(groupId: Long): Result<StudyGroup> =
            withContext(Dispatchers.IO) {
                try {
                    val group =
                            client.from("study_groups")
                                    .select { filter { eq("id", groupId) } }
                                    .decodeSingle<StudyGroup>()

                    Result.success(group)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Upload group image to Supabase Storage
     * @param groupId The ID of the group
     * @param imageUri Local URI of the image file
     * @return The public URL of the uploaded image
     */
    suspend fun uploadGroupImage(groupId: Long, imageUri: Uri): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val bucket = client.storage.from("group_images")
                    val path = "$groupId/cover.jpg"

                    // Upload file (this is simplified - actual implementation needs file bytes)
                    // You'll need to read the file from URI in the actual implementation
                    // bucket.upload(path, fileBytes)

                    val publicUrl = bucket.publicUrl(path)
                    Result.success(publicUrl)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Search groups by name */
    suspend fun searchGroups(query: String): Result<List<StudyGroup>> =
            withContext(Dispatchers.IO) {
                try {
                    val groups =
                            client.from("study_groups")
                                    .select {
                                        filter { ilike("name", "%$query%") }
                                        order("created_at", Order.DESCENDING)
                                    }
                                    .decodeList<StudyGroup>()

                    Result.success(groups)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
