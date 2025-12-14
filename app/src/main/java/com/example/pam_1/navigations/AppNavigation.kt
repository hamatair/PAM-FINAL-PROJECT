package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pam_1.data.SupabaseClient
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
    // EXPENSE SETUP (SHARED INSTANCE)
    // ===============================
    // ✅ PENTING: Buat SATU instance di NavHost level
    // Instance ini akan di-share ke SEMUA expense screens
    val expenseRepository = remember { ExpenseRepository() }
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(expenseRepository)
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
                expenseViewModel = expenseViewModel // ✅ Pass shared instance
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

        // ---------- EXPENSE ----------
        composable("add_expense") {
            // ✅ Gunakan shared instance (BUKAN buat baru!)
            com.example.pam_1.ui.screens.features.finance.AddExpenseScreen(
                viewModel = expenseViewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        composable(
            route = "expense_detail/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: 0
            
            // ✅ Gunakan shared instance (BUKAN buat baru!)
            com.example.pam_1.ui.screens.features.finance.ExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = expenseViewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
    }
}