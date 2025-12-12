package com.example.pam_1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pam_1.data.repository.GroupInviteRepository
import com.example.pam_1.data.repository.GroupMemberRepository
import com.example.pam_1.data.repository.StudyGroupRepository

class StudyGroupViewModelFactory(
        private val groupRepository: StudyGroupRepository,
        private val memberRepository: GroupMemberRepository,
        private val inviteRepository: GroupInviteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyGroupViewModel(groupRepository, memberRepository, inviteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
