package com.example.pam_1.navigations

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.*
import com.example.pam_1.ui.screens.*
import com.example.pam_1.ui.screens.features.auth.*
import com.example.pam_1.ui.screens.features.events.*
import com.example.pam_1.ui.screens.features.group_chat.*
import com.example.pam_1.ui.screens.features.notes.*
import com.example.pam_1.ui.screens.features.tugas.TugasScreen
import com.example.pam_1.ui.screens.features.finance.*
import com.example.pam_1.viewmodel.*
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ================= AUTH =================
    val authRepository = remember { AuthRepository(context) }
    val userRepository = remember { UserRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository, userRepository)
    )

    // ================= EVENT =================
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModelFactory = remember { EventViewModelFactory(eventRepository) }

    // ================= NOTES (SHARED) =================
    val noteRepository = remember { NoteRepository(SupabaseClient.client) }
    val noteViewModelFactory = remember { NoteViewModelFactory(noteRepository) }
    val noteViewModel: NoteViewModel = viewModel(factory = noteViewModelFactory)

    // ================= STUDY GROUP =================
    val groupRepository = remember { StudyGroupRepository() }
    val memberRepository = remember { GroupMemberRepository() }
    val inviteRepository = remember { GroupInviteRepository() }

    val studyGroupViewModel: StudyGroupViewModel = viewModel(
        factory = StudyGroupViewModelFactory(
            groupRepository,
            memberRepository,
            inviteRepository
        )
    )

    // ================= EXPENSE (SHARED) =================
    val expenseRepository = remember { ExpenseRepository() }
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(expenseRepository)
    )

    // ================= NAV HOST =================
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ---------- AUTH ----------
        composable("splash") { SplashScreen(navController, authRepository) }
        composable("login") { LoginScreen(navController, authViewModel) }
        composable("register") { RegisterScreen(navController, authViewModel) }
        composable("otp_verification") { OTPVerificationScreen(navController, authViewModel) }
        composable("forgot_password") { ForgotPasswordScreen(navController, authViewModel) }
        composable("new_password") { NewPasswordScreen(navController, authViewModel) }
        composable("profile") { ProfileScreen(navController, authViewModel) }

        // ---------- HOME ----------
        composable("home") {
            MainAppScreen(
                navController = navController,
                authViewModel = authViewModel,
                studyGroupViewModel = studyGroupViewModel,
                noteViewModel = noteViewModel,
                expenseViewModel = expenseViewModel
            )
        }

        // ---------- EVENT ----------
        composable("add_event") {
            val vm: EventViewModel = viewModel(factory = eventViewModelFactory)
            AddEventScreen(vm) { navController.popBackStackSafe() }
        }

        composable("my_events") {
            val vm: EventViewModel = viewModel(factory = eventViewModelFactory)
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
            MyEventsScreen(
                viewModel = vm,
                currentUserId = userId,
                onNavigateToDetail = { navController.navigateSafe("event_detail/$it") },
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        composable(
            "event_detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) {
            val eventId = it.arguments?.getString("eventId") ?: ""
            val vm: EventViewModel = viewModel(factory = eventViewModelFactory)
            DetailEventScreen(eventId, vm) { navController.popBackStackSafe() }
        }

        // ---------- STUDY GROUP ----------
        composable("study_groups") { StudyGroupListScreen(navController, studyGroupViewModel) }
        composable("create_group") { CreateEditGroupScreen(navController, studyGroupViewModel) }

        composable(
            "edit_group/{groupId}",
            listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            CreateEditGroupScreen(
                navController,
                studyGroupViewModel,
                it.arguments!!.getLong("groupId")
            )
        }

        composable(
            "group_detail/{groupId}",
            listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupDetailScreen(
                navController,
                studyGroupViewModel,
                it.arguments!!.getLong("groupId")
            )
        }

        composable(
            "manage_invites/{groupId}",
            listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            InviteManagementScreen(
                navController,
                studyGroupViewModel,
                it.arguments!!.getLong("groupId")
            )
        }

        composable(
            "group_chat/{groupId}",
            listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupChatScreen(navController, it.arguments!!.getLong("groupId"))
        }

        composable("join_group") {
            JoinGroupScreen(navController, studyGroupViewModel)
        }

        // ---------- TUGAS ----------
        composable("task_schedule") {
            val repo = remember { TugasRepository() }
            val vm: TugasViewModel = viewModel(factory = TugasViewModelFactory(repo))
            TugasScreen(vm)
        }

        // ---------- NOTES ----------
        composable("note/add") {
            AddEditNoteScreen(
                note = null,
                onBack = { navController.popBackStackSafe() },
                onSave = { t, d, p, img ->
                    noteViewModel.addNote(t, d, p, img)
                }
            )
        }

        composable(
            "note/read/{noteId}",
            listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            val id = it.arguments!!.getLong("noteId")
            LaunchedEffect(id) { noteViewModel.loadNoteDetail(id) }

            val state by noteViewModel.noteDetailState.collectAsState()
            if (state is UiState.Success) {
                ReadNoteScreen(
                    note = (state as UiState.Success).data!!,
                    onBack = { navController.popBackStackSafe() },
                    onEditNote = { navController.navigateSafe("note/edit/$it") },
                    onPinToggle = { p -> noteViewModel.updatePinStatus(id, p) },
                    onDelete = { noteViewModel.deleteNote(it) }
                )
            }
        }

        composable(
            "note/edit/{noteId}",
            listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            val id = it.arguments!!.getLong("noteId")
            LaunchedEffect(id) { noteViewModel.loadNoteDetail(id) }

            val state by noteViewModel.noteDetailState.collectAsState()
            if (state is UiState.Success) {
                AddEditNoteScreen(
                    note = (state as UiState.Success).data,
                    onBack = { navController.popBackStackSafe() },
                    onSave = { t, d, p, img ->
                        noteViewModel.updateNote(id, t, d, p, img)
                    }
                )
            }
        }

        // ---------- EXPENSE ----------
        composable("add_expense") {
            AddExpenseScreen(expenseViewModel) {
                navController.popBackStackSafe()
            }
        }

        composable(
            "expense_detail/{expenseId}",
            listOf(navArgument("expenseId") { type = NavType.IntType })
        ) {
            ExpenseDetailScreen(
                it.arguments!!.getInt("expenseId"),
                expenseViewModel
            ) {
                navController.popBackStackSafe()
            }
        }
    }
}
