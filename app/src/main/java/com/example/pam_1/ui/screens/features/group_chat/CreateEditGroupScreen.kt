package com.example.pam_1.ui.screens.features.group_chat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.viewmodel.StudyGroupUIState
import com.example.pam_1.viewmodel.StudyGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditGroupScreen(
        navController: NavController,
        viewModel: StudyGroupViewModel,
        groupId: Long? = null // If null, create mode; if not null, edit mode
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uiState = viewModel.uiState
    val isEditMode = groupId != null

    // Load existing group data if in edit mode
    LaunchedEffect(groupId) { groupId?.let { viewModel.loadGroupById(it) } }

    // Populate fields when group is loaded
    LaunchedEffect(viewModel.selectedGroup) {
        viewModel.selectedGroup?.let { group ->
            if (isEditMode) {
                name = group.name
                description = group.description ?: ""
                course = group.course ?: ""
                isPublic = group.isPublic
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is StudyGroupUIState.Success -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                // Navigate back to study groups list
                navController.popBackStack(route = "study_groups", inclusive = false)
            }
            is StudyGroupUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(if (isEditMode) "Edit Group" else "Create Group") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
        ) {
            Text(
                    text =
                            if (isEditMode) "Update your study group"
                            else "Create a new study group",
                    style = MaterialTheme.typography.headlineSmall
            )
            Text(
                    text = "Fill in the details below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Group Name
            OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError,
                    singleLine = true,
                    supportingText = {
                        if (nameError)
                                Text(
                                        text = "Group name is required",
                                        color = MaterialTheme.colorScheme.error
                                )
                    }
            )
            Spacer(Modifier.height(16.dp))

            // Course
            OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Pemrograman Mobile") }
            )
            Spacer(Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("Describe what this group is about...") }
            )
            Spacer(Modifier.height(16.dp))

            // Public Switch
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Public Group", style = MaterialTheme.typography.titleMedium)
                    Text(
                            text = "Anyone can join without an invite code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
            }

            Spacer(Modifier.height(32.dp))

            // Submit Button
            if (uiState is StudyGroupUIState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                        onClick = {
                            // Validation
                            if (name.isBlank()) {
                                nameError = true
                                Toast.makeText(
                                                context,
                                                "Please enter a group name",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@Button
                            }

                            // Create or Update
                            if (isEditMode && groupId != null) {
                                viewModel.updateGroup(
                                        groupId = groupId,
                                        name = name,
                                        description = description.ifBlank { null },
                                        course = course.ifBlank { null },
                                        isPublic = isPublic
                                )
                            } else {
                                viewModel.createGroup(
                                        name = name,
                                        description = description.ifBlank { null },
                                        course = course.ifBlank { null },
                                        isPublic = isPublic
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium
                ) { Text(if (isEditMode) "Update Group" else "Create Group") }
            }
        }
    }
}
