package com.example.pam_1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam_1.data.model.GroupInvite
import com.example.pam_1.data.model.GroupMember
import com.example.pam_1.data.model.GroupRole
import com.example.pam_1.data.model.StudyGroup
import com.example.pam_1.data.repository.GroupInviteRepository
import com.example.pam_1.data.repository.GroupMemberRepository
import com.example.pam_1.data.repository.StudyGroupRepository
import kotlinx.coroutines.launch

sealed class StudyGroupUIState {
    object Idle : StudyGroupUIState()
    object Loading : StudyGroupUIState()
    data class Success(val message: String = "") : StudyGroupUIState()
    data class Error(val message: String) : StudyGroupUIState()
}

class StudyGroupViewModel(
        private val groupRepository: StudyGroupRepository,
        private val memberRepository: GroupMemberRepository,
        private val inviteRepository: GroupInviteRepository
) : ViewModel() {

    var uiState by mutableStateOf<StudyGroupUIState>(StudyGroupUIState.Idle)
        private set

    var myGroups by mutableStateOf<List<StudyGroup>>(emptyList())
        private set

    var publicGroups by mutableStateOf<List<StudyGroup>>(emptyList())
        private set

    var selectedGroup by mutableStateOf<StudyGroup?>(null)
        private set

    var groupMembers by mutableStateOf<List<GroupMember>>(emptyList())
        private set

    var groupInvites by mutableStateOf<List<GroupInvite>>(emptyList())
        private set

    var currentUserRole by mutableStateOf<GroupRole?>(null)
        private set

    var memberCount by mutableStateOf(0)
        private set

    fun resetState() {
        uiState = StudyGroupUIState.Idle
    }

    /** Load groups where user is a member */
    fun loadMyGroups() {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .getMyGroups()
                    .onSuccess { groups ->
                        myGroups = groups
                        uiState = StudyGroupUIState.Idle
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to load groups")
                    }
        }
    }

    /** Load public groups */
    fun loadPublicGroups() {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .getPublicGroups()
                    .onSuccess { groups ->
                        publicGroups = groups
                        uiState = StudyGroupUIState.Idle
                    }
                    .onFailure { e ->
                        uiState =
                                StudyGroupUIState.Error(e.message ?: "Failed to load public groups")
                    }
        }
    }

    /** Create a new study group */
    fun createGroup(name: String, description: String?, course: String?, isPublic: Boolean) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .createGroup(
                            name = name,
                            description = description,
                            course = course,
                            isPublic = isPublic
                    )
                    .onSuccess { group ->
                        selectedGroup = group
                        uiState = StudyGroupUIState.Success("Group created successfully")
                        loadMyGroups() // Refresh the list
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to create group")
                    }
        }
    }

    /** Update an existing group */
    fun updateGroup(
            groupId: String,
            name: String?,
            description: String?,
            course: String?,
            isPublic: Boolean?
    ) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .updateGroup(
                            groupId = groupId,
                            name = name,
                            description = description,
                            course = course,
                            isPublic = isPublic,
                            imageUrl = null
                    )
                    .onSuccess { group ->
                        selectedGroup = group
                        uiState = StudyGroupUIState.Success("Group updated successfully")
                        loadMyGroups()
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to update group")
                    }
        }
    }

    /** Delete a group */
    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .deleteGroup(groupId)
                    .onSuccess {
                        uiState = StudyGroupUIState.Success("Group deleted successfully")
                        selectedGroup = null
                        loadMyGroups()
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to delete group")
                    }
        }
    }

    /** Load a specific group by ID */
    fun loadGroupById(groupId: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .getGroupById(groupId)
                    .onSuccess { group ->
                        selectedGroup = group
                        loadGroupMembers(groupId)
                        loadCurrentUserRole(groupId)
                        loadMemberCount(groupId)
                        uiState = StudyGroupUIState.Idle
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to load group")
                    }
        }
    }

    /** Load members of a group */
    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            memberRepository
                    .getGroupMembers(groupId)
                    .onSuccess { members -> groupMembers = members }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to load members")
                    }
        }
    }

    /** Load current user's role in the group */
    private fun loadCurrentUserRole(groupId: String) {
        viewModelScope.launch {
            memberRepository.getUserRole(groupId).onSuccess { role -> currentUserRole = role }
        }
    }

    /** Load member count */
    private fun loadMemberCount(groupId: String) {
        viewModelScope.launch {
            memberRepository.getMemberCount(groupId).onSuccess { count -> memberCount = count }
        }
    }

    /** Leave a group */
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            memberRepository
                    .leaveGroup(groupId)
                    .onSuccess {
                        uiState = StudyGroupUIState.Success("Left group successfully")
                        selectedGroup = null
                        loadMyGroups()
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to leave group")
                    }
        }
    }

    /** Remove a member from the group */
    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            memberRepository
                    .removeMember(groupId, userId)
                    .onSuccess {
                        uiState = StudyGroupUIState.Success("Member removed successfully")
                        loadGroupMembers(groupId)
                        loadMemberCount(groupId)
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to remove member")
                    }
        }
    }

    /** Update member role */
    fun updateMemberRole(groupId: String, userId: String, newRole: GroupRole) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            memberRepository
                    .updateMemberRole(groupId, userId, newRole)
                    .onSuccess {
                        uiState = StudyGroupUIState.Success("Role updated successfully")
                        loadGroupMembers(groupId)
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to update role")
                    }
        }
    }

    /** Create an invite */
    fun createInvite(groupId: String, maxUses: Int, expiresInDays: Int?) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            inviteRepository
                    .createInvite(groupId, maxUses, expiresInDays)
                    .onSuccess { invite ->
                        uiState = StudyGroupUIState.Success("Invite created: ${invite.code}")
                        loadGroupInvites(groupId)
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to create invite")
                    }
        }
    }

    /** Load invites for a group */
    fun loadGroupInvites(groupId: String) {
        viewModelScope.launch {
            inviteRepository
                    .getGroupInvites(groupId)
                    .onSuccess { invites -> groupInvites = invites }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to load invites")
                    }
        }
    }

    /** Join a group by invite code */
    fun joinByInviteCode(code: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            inviteRepository
                    .joinByCode(code)
                    .onSuccess { groupId ->
                        uiState = StudyGroupUIState.Success("Joined group successfully")
                        loadGroupById(groupId)
                        loadMyGroups()
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Failed to join group")
                    }
        }
    }

    /** Deactivate an invite */
    fun deactivateInvite(inviteId: String, groupId: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            inviteRepository
                    .deactivateInvite(inviteId)
                    .onSuccess {
                        uiState = StudyGroupUIState.Success("Invite deactivated")
                        loadGroupInvites(groupId)
                    }
                    .onFailure { e ->
                        uiState =
                                StudyGroupUIState.Error(e.message ?: "Failed to deactivate invite")
                    }
        }
    }

    /** Search groups by name */
    fun searchGroups(query: String) {
        viewModelScope.launch {
            uiState = StudyGroupUIState.Loading
            groupRepository
                    .searchGroups(query)
                    .onSuccess { groups ->
                        publicGroups = groups
                        uiState = StudyGroupUIState.Idle
                    }
                    .onFailure { e ->
                        uiState = StudyGroupUIState.Error(e.message ?: "Search failed")
                    }
        }
    }
}
