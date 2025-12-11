package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.ui.screens.*
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.AuthViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Pass context ke AuthRepository
    val authRepository = remember { AuthRepository(context) }
    val userRepository = remember { UserRepository() }

    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            authRepository = authRepository,
            userRepository = userRepository
        )
    )

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController, authRepository)
        }
        composable("login") {
            LoginScreen(navController, viewModel)
        }
        composable("register") {
            RegisterScreen(navController, viewModel)
        }
        // Route baru untuk verifikasi OTP
        composable("otp_verification") {
            OTPVerificationScreen(navController, viewModel)
        }
        composable("home") {
            MainAppScreen(navController, viewModel)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController, viewModel)
        }
        composable("new_password") {
            NewPasswordScreen(navController, viewModel)
        }
        composable("profile") {
            ProfileScreen(navController, viewModel)
        }
    }
}