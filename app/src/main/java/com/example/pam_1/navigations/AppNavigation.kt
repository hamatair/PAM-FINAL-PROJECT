package com.example.pam_1.navigations

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.EventRepository
import com.example.pam_1.data.repository.NoteRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.data.repository.*
import com.example.pam_1.ui.screens.*
import com.example.pam_1.ui.screens.features.auth.*
import com.example.pam_1.ui.screens.features.events.*
import com.example.pam_1.ui.screens.features.group_chat.CreateEditGroupScreen
import com.example.pam_1.ui.screens.features.group_chat.GroupChatScreen
import com.example.pam_1.ui.screens.features.group_chat.GroupDetailScreen
import com.example.pam_1.ui.screens.features.group_chat.InviteManagementScreen
import com.example.pam_1.ui.screens.features.group_chat.JoinGroupScreen
import com.example.pam_1.ui.screens.features.group_chat.StudyGroupListScreen
import com.example.pam_1.ui.screens.features.tugas.TugasScreen
import com.example.pam_1.viewmodel.*
import com.example.pam_1.ui.screens.features.notes.AddEditNoteScreen
import com.example.pam_1.ui.screens.features.notes.ReadNoteScreen
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.AuthViewModelFactory
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.EventViewModelFactory
import com.example.pam_1.viewmodel.NoteViewModel
import com.example.pam_1.viewmodel.NoteViewModelFactory
import com.example.pam_1.viewmodel.UiState
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ===============================
    // AUTH SETUP
    // ===============================
    val authRepository = remember { AuthRepository(context) }
    val userRepository = remember { UserRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            authRepository = authRepository,
            userRepository = userRepository
        )
    )

    // ===============================
    // EVENT SETUP
    // ===============================
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModelFactory = remember { EventViewModelFactory(eventRepository) }

    // Factory Note & Shared ViewModel
    val noteRepository = remember { NoteRepository(SupabaseClient.client) }
    val noteViewModelFactory = remember { NoteViewModelFactory(noteRepository) }
    val sharedNoteViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)


    // ===============================
    // STUDY GROUP SETUP
    // ===============================
    val groupRepository = remember { StudyGroupRepository() }
    val memberRepository = remember { GroupMemberRepository() }
    val inviteRepository = remember { GroupInviteRepository() }

    val studyGroupViewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(
            groupRepository = groupRepository,
            memberRepository = memberRepository,
            inviteRepository = inviteRepository
        )
    )

    // ===============================
    // NAVIGATION GRAPH
    // ===============================
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ---------- AUTH ----------
        composable("splash") {
            SplashScreen(navController, authRepository)
        }
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("register") {
            RegisterScreen(navController, authViewModel)
        }
        composable("otp_verification") {
            OTPVerificationScreen(navController, authViewModel)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController, authViewModel)
        }
        composable("new_password") {
            NewPasswordScreen(navController, authViewModel)
        }
        composable("profile") {
            ProfileScreen(navController, authViewModel)
        }

        // ---------- HOME ----------
        composable("home") {
            MainAppScreen(
                navController = navController,
                authViewModel = authViewModel,
                studyGroupViewModel = studyGroupViewModel,
                viewModel = authViewModel,
                noteViewModel = sharedNoteViewModel
            )
        }


        // ---------- EVENT ----------
        composable("add_event") {
            val eventViewModel: EventViewModel =
                viewModel(factory = eventViewModelFactory)

            AddEventScreen(
                viewModel = eventViewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        composable("my_events") {
            val eventViewModel: EventViewModel =
                viewModel(factory = eventViewModelFactory)

            val currentUserId =
                SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

            MyEventsScreen(
                viewModel = eventViewModel,
                currentUserId = currentUserId,
                onNavigateToDetail = { eventId ->
                    navController.navigateSafe("event_detail/$eventId")
                },
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        composable(
            route = "event_detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventViewModel =
                viewModel(factory = eventViewModelFactory)

            DetailEventScreen(
                eventId = eventId,
                viewModel = eventViewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        // ---------- STUDY GROUP ----------
        composable("study_groups") {
            StudyGroupListScreen(navController, studyGroupViewModel)
        }

        composable("create_group") {
            CreateEditGroupScreen(navController, studyGroupViewModel)
        }

        composable(
            route = "edit_group/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            groupId?.let {
                CreateEditGroupScreen(navController, studyGroupViewModel, it)
            }
        }

        composable(
            route = "group_detail/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            groupId?.let {
                GroupDetailScreen(navController, studyGroupViewModel, it)
            }
        }

        composable(
            route = "manage_invites/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            groupId?.let {
                InviteManagementScreen(navController, studyGroupViewModel, it)
            }
        }

        composable(
            route = "group_chat/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")
            groupId?.let {
                GroupChatScreen(navController, it)
            }
        }

        composable("join_group") {
            JoinGroupScreen(navController, studyGroupViewModel)
        }

        // ---------- TUGAS ----------
        composable("task_schedule") {
            // PERBAIKAN DISINI: Gunakan Factory + Repository
            val tugasRepository = remember { TugasRepository() }
            val tugasViewModel: TugasViewModel = viewModel(
                factory = TugasViewModelFactory(tugasRepository)
            )

            TugasScreen(
                viewModel = tugasViewModel
            )
        }

        // ================= NOTES =================

        composable(
            route = "note/read/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments!!.getLong("noteId")

            // Gunakan sharedNoteViewModel
            val noteViewModel = sharedNoteViewModel

            val noteState by noteViewModel.noteDetailState.collectAsState()
            val actionState by noteViewModel.actionState.collectAsState()

            LaunchedEffect(noteId) {
                noteViewModel.loadNoteDetail(noteId)
            }

            // Listener Action (Hapus)
            LaunchedEffect(actionState) {
                if (actionState is UiState.Success) {
                    val msg = (actionState as UiState.Success).data
                    if (msg.contains("dihapus", ignoreCase = true)) {
                        noteViewModel.resetActionState()
                        navController.popBackStackSafe()
                    }
                }
            }

            if (noteState is UiState.Success) {
                val note = (noteState as UiState.Success).data
                if (note != null) {
                    ReadNoteScreen(
                        note = note,
                        onBack = { navController.popBackStackSafe() },
                        onEditNote = { id -> navController.navigateSafe("note/edit/$id") },
                        // FIX: Gunakan updatePinStatus agar lebih aman & ringan
                        onPinToggle = { pinned ->
                            noteViewModel.updatePinStatus(note.id!!, pinned)
                        },
                        onDelete = { id -> noteViewModel.deleteNote(id) }
                    )
                }
            }
        }

        composable("note/add") {
            val noteViewModel = sharedNoteViewModel
            val actionState by noteViewModel.actionState.collectAsState()
            val context = LocalContext.current

            // Listener: SUKSES
            LaunchedEffect(actionState) {
                if (actionState is UiState.Success) {
                    android.widget.Toast.makeText(context, "Berhasil Simpan!", android.widget.Toast.LENGTH_SHORT).show()
                    noteViewModel.loadNotes()
                    noteViewModel.resetActionState()
                    navController.popBackStackSafe()
                }
            }

            // Listener: ERROR
            LaunchedEffect(actionState) {
                if (actionState is UiState.Error) {
                    val errorMsg = (actionState as UiState.Error).message
                    Log.e("DEBUG_NAV", "UI State Error: $errorMsg")
                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            }

            AddEditNoteScreen(
                note = null,
                onBack = { navController.popBackStackSafe() },
                // FIX: Menambahkan parameter imageBytes
                onSave = { title, description, isPinned, imageBytes ->
                    if (title.isBlank()) {
                        android.widget.Toast.makeText(context, "Judul tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        noteViewModel.addNote(
                            title = title,
                            description = description,
                            isPinned = isPinned,
                            imageBytes = imageBytes // Kirim byte gambar ke ViewModel
                        )
                    }
                }
            )
        }

        composable(
            route = "note/edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments!!.getLong("noteId")
            val noteViewModel = sharedNoteViewModel

            val noteState = noteViewModel.noteDetailState.collectAsState()
            val actionState by noteViewModel.actionState.collectAsState()

            LaunchedEffect(noteId) { noteViewModel.loadNoteDetail(noteId) }

            // Jika Update Berhasil -> Refresh List -> Kembali
            LaunchedEffect(actionState) {
                if (actionState is UiState.Success) {
                    noteViewModel.loadNotes()
                    noteViewModel.resetActionState()
                    navController.popBackStackSafe()
                }
            }

            if (noteState.value is UiState.Success) {
                val note = (noteState.value as UiState.Success).data ?: return@composable
                AddEditNoteScreen(
                    note = note,
                    onBack = {
                        noteViewModel.clearNoteDetail()
                        navController.popBackStackSafe()
                    },
                    // FIX: Menambahkan parameter imageBytes
                    onSave = { title, desc, pinned, imageBytes ->
                        noteViewModel.updateNote(
                            noteId = noteId,
                            title = title,
                            description = desc,
                            isPinned = pinned,
                            imageBytes = imageBytes, // Kirim byte gambar baru (jika ada)
                            currentImageUrl = note.imageUrl
                        )
                    }
                )
            }
        }
    }
}