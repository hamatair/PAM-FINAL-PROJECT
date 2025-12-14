package com.example.pam_1.data.repository

import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.GroupMember
import com.example.pam_1.data.model.GroupRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class GroupMemberRepository {
    private val client = SupabaseClient.client

    /** Get all members of a group */
    suspend fun getGroupMembers(groupId: Long): Result<List<GroupMember>> =
            withContext(Dispatchers.IO) {
                try {
                    val members =
                            client.from("group_members")
                                    .select(
                                            columns =
                                                    Columns.raw(
                                                            "*,profiles:user_id(full_name,username,email)"
                                                    )
                                    ) {
                                        filter { eq("group_id", groupId) }
                                        order("joined_at", Order.ASCENDING)
                                    }
                                    .decodeList<JsonObject>()
                                    .map { json ->
                                        // Extract member fields
                                        val id = json["id"]?.jsonPrimitive?.longOrNull
                                        val groupIdVal =
                                                json["group_id"]?.jsonPrimitive?.longOrNull ?: 0L
                                        val userId = json["user_id"]?.jsonPrimitive?.content ?: ""
                                        val role = json["role"]?.jsonPrimitive?.content ?: "member"
                                        val joinedAt = json["joined_at"]?.jsonPrimitive?.content

                                        // Extract profile info from joined data
                                        val profile = json["profiles"]?.jsonObject
                                        val fullName =
                                                profile?.get("full_name")?.jsonPrimitive?.content
                                        val username =
                                                profile?.get("username")?.jsonPrimitive?.content
                                        val email = profile?.get("email")?.jsonPrimitive?.content

                                        GroupMember(
                                                id = id,
                                                groupId = groupIdVal,
                                                userId = userId,
                                                role = role,
                                                joinedAt = joinedAt,
                                                fullName = fullName,
                                                username = username,
                                                email = email
                                        )
                                    }

                    Result.success(members)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Add a member to a group This is typically called after invite validation */
    suspend fun addMember(
            groupId: Long,
            userId: String,
            role: GroupRole = GroupRole.MEMBER
    ): Result<GroupMember> =
            withContext(Dispatchers.IO) {
                try {
                    // Get current user if userId is empty
                    val actualUserId =
                            if (userId.isBlank()) {
                                client.auth.currentUserOrNull()?.id
                                        ?: return@withContext Result.failure(
                                                Exception("User not authenticated")
                                        )
                            } else {
                                userId
                            }

                    val newMember = buildJsonObject {
                        put("group_id", groupId)
                        put("user_id", actualUserId)
                        put("role", role.value)
                    }

                    val result =
                            client.from("group_members")
                                    .insert(newMember) { select() }
                                    .decodeSingle<GroupMember>()

                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Remove a member from a group Can be called by owner/moderator to remove others, or by member
     * to leave
     */
    suspend fun removeMember(groupId: Long, userId: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    client.from("group_members").delete {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", userId)
                        }
                    }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Update a member's role Only owner/moderator can do this */
    suspend fun updateMemberRole(
            groupId: Long,
            userId: String,
            newRole: GroupRole
    ): Result<GroupMember> =
            withContext(Dispatchers.IO) {
                try {
                    val result =
                            client.from("group_members")
                                    .update(buildJsonObject { put("role", newRole.value) }) {
                                        filter {
                                            eq("group_id", groupId)
                                            eq("user_id", userId)
                                        }
                                        select()
                                    }
                                    .decodeSingle<GroupMember>()

                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Leave a group (remove self) */
    suspend fun leaveGroup(groupId: Long): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.failure(
                                            Exception("User not authenticated")
                                    )

                    removeMember(groupId, userId)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Check if current user is a member of the group */
    suspend fun isMember(groupId: Long): Result<Boolean> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.success(false)

                    val members =
                            client.from("group_members")
                                    .select {
                                        filter {
                                            eq("group_id", groupId)
                                            eq("user_id", userId)
                                        }
                                    }
                                    .decodeList<GroupMember>()

                    Result.success(members.isNotEmpty())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get current user's role in the group */
    suspend fun getUserRole(groupId: Long): Result<GroupRole?> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.success(null)

                    val member =
                            client.from("group_members")
                                    .select {
                                        filter {
                                            eq("group_id", groupId)
                                            eq("user_id", userId)
                                        }
                                    }
                                    .decodeList<GroupMember>()
                                    .firstOrNull()

                    val role = member?.let { GroupRole.fromString(it.role) }
                    Result.success(role)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get member count for a group */
    suspend fun getMemberCount(groupId: Long): Result<Int> =
            withContext(Dispatchers.IO) {
                try {
                    val members = getGroupMembers(groupId).getOrThrow()
                    Result.success(members.size)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
