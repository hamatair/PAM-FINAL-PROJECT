package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.GroupInviteRepository
import com.example.pam_1.data.repository.GroupMemberRepository
import com.example.pam_1.data.repository.StudyGroupRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.ui.screens.*
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.AuthViewModelFactory
import com.example.pam_1.viewmodel.StudyGroupViewModel
import com.example.pam_1.viewmodel.StudyGroupViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Pass context ke AuthRepository
    val authRepository = remember { AuthRepository(context) }
    val userRepository = remember { UserRepository() }

    val viewModel: AuthViewModel =
            viewModel(
                    factory =
                            AuthViewModelFactory(
                                    authRepository = authRepository,
                                    userRepository = userRepository
                            )
            )

    // Study Group repositories and ViewModel
    val groupRepository = remember { StudyGroupRepository() }
    val memberRepository = remember { GroupMemberRepository() }
    val inviteRepository = remember { GroupInviteRepository() }

    val studyGroupViewModel: StudyGroupViewModel =
            viewModel(
                    factory =
                            StudyGroupViewModelFactory(
                                    groupRepository = groupRepository,
                                    memberRepository = memberRepository,
                                    inviteRepository = inviteRepository
                            )
            )

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController, authRepository) }
        composable("login") { LoginScreen(navController, viewModel) }
        composable("register") { RegisterScreen(navController, viewModel) }
        // Route baru untuk verifikasi OTP
        composable("otp_verification") { OTPVerificationScreen(navController, viewModel) }
        composable("home") { MainAppScreen(navController, viewModel) }
        composable("forgot_password") { ForgotPasswordScreen(navController, viewModel) }
        composable("new_password") { NewPasswordScreen(navController, viewModel) }
        composable("profile") { ProfileScreen(navController, viewModel) }
        // Study Group Routes
        composable("study_groups") { StudyGroupListScreen(navController, studyGroupViewModel) }
        composable("create_group") { CreateEditGroupScreen(navController, studyGroupViewModel) }
        composable("edit_group/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            groupId?.let { CreateEditGroupScreen(navController, studyGroupViewModel, it) }
        }
        composable("group_detail/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            groupId?.let { GroupDetailScreen(navController, studyGroupViewModel, it) }
        }
        composable("manage_invites/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            groupId?.let { InviteManagementScreen(navController, studyGroupViewModel, it) }
        }
        composable("join_group") { JoinGroupScreen(navController, studyGroupViewModel) }
    }
}
