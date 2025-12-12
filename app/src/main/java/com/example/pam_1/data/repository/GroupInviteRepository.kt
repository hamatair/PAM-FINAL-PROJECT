package com.example.pam_1.data.repository

import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.model.GroupInvite
import com.example.pam_1.data.model.GroupRole
import com.example.pam_1.utils.InviteCodeGenerator
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupInviteRepository {
    private val client = SupabaseClient.client
    private val memberRepository = GroupMemberRepository()

    /** Create a new invite for a group */
    suspend fun createInvite(
            groupId: String,
            maxUses: Int = 1,
            expiresInDays: Int? = 7,
            note: String? = null
    ): Result<GroupInvite> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.failure(
                                            Exception("User not authenticated")
                                    )

                    val code = InviteCodeGenerator.generateCode()
                    val expiresAt =
                            expiresInDays?.let {
                                Instant.now().plus(it.toLong(), ChronoUnit.DAYS).toString()
                            }

                    val newInvite =
                            mapOf(
                                    "group_id" to groupId,
                                    "code" to code,
                                    "created_by" to userId,
                                    "expires_at" to expiresAt,
                                    "max_uses" to maxUses,
                                    "note" to note
                            )

                    val result =
                            client.from("group_invites")
                                    .insert(newInvite) { select() }
                                    .decodeSingle<GroupInvite>()

                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Validate an invite code before joining Returns the invite if valid, or error if
     * invalid/expired/used up
     */
    suspend fun validateInvite(code: String): Result<GroupInvite> =
            withContext(Dispatchers.IO) {
                try {
                    // Find the invite by code
                    val invite =
                            client.from("group_invites")
                                    .select {
                                        filter {
                                            eq("code", code)
                                            eq("is_active", true)
                                        }
                                    }
                                    .decodeList<GroupInvite>()
                                    .firstOrNull()
                                    ?: return@withContext Result.failure(
                                            Exception("Invalid invite code")
                                    )

                    // Check if expired
                    invite.expiresAt?.let { expiresAt ->
                        val expiry = Instant.parse(expiresAt)
                        if (Instant.now().isAfter(expiry)) {
                            return@withContext Result.failure(Exception("Invite code has expired"))
                        }
                    }

                    // Check if max uses reached
                    if (invite.maxUses > 0 && invite.usedCount >= invite.maxUses) {
                        return@withContext Result.failure(
                                Exception("Invite code has been fully used")
                        )
                    }

                    Result.success(invite)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Join a group using an invite code Validates the code, adds the user as a member, and
     * increments usage
     */
    suspend fun joinByCode(code: String): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                            client.auth.currentUserOrNull()?.id
                                    ?: return@withContext Result.failure(
                                            Exception("User not authenticated")
                                    )

                    // Validate the invite
                    val invite =
                            validateInvite(code).getOrElse { e ->
                                return@withContext Result.failure(e)
                            }

                    // Check if user is already a member
                    val isMember = memberRepository.isMember(invite.groupId).getOrElse { false }
                    if (isMember) {
                        return@withContext Result.failure(
                                Exception("You are already a member of this group")
                        )
                    }

                    // Add user as member
                    memberRepository.addMember(
                                    groupId = invite.groupId,
                                    userId = userId,
                                    role = GroupRole.MEMBER
                            )
                            .getOrElse { e ->
                                return@withContext Result.failure(e)
                            }

                    // Increment used_count
                    val newUsedCount = invite.usedCount + 1
                    val shouldDeactivate = invite.maxUses > 0 && newUsedCount >= invite.maxUses

                    client.from("group_invites").update(
                                    mapOf(
                                            "used_count" to newUsedCount,
                                            "is_active" to !shouldDeactivate
                                    )
                            ) { filter { eq("id", invite.id!!) } }

                    Result.success(invite.groupId)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Get all invites for a group Only accessible by owner/moderator */
    suspend fun getGroupInvites(groupId: String): Result<List<GroupInvite>> =
            withContext(Dispatchers.IO) {
                try {
                    val invites =
                            client.from("group_invites")
                                    .select {
                                        filter { eq("group_id", groupId) }
                                        order("created_at", order = Order.DESCENDING)
                                    }
                                    .decodeList<GroupInvite>()

                    Result.success(invites)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Deactivate an invite */
    suspend fun deactivateInvite(inviteId: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    client.from("group_invites").update(mapOf("is_active" to false)) {
                        filter { eq("id", inviteId) }
                    }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /** Delete an invite permanently */
    suspend fun deleteInvite(inviteId: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    client.from("group_invites").delete { filter { eq("id", inviteId) } }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
