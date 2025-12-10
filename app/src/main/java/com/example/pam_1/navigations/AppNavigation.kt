package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.ui.screens.LoginScreen
import com.example.pam_1.ui.screens.ProfileScreen // Menggunakan ProfileScreen yang sudah digabung
import com.example.pam_1.ui.screens.RegisterScreen
import com.example.pam_1.ui.screens.SplashScreen
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.AuthViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Setup ViewModel dengan kedua Repository
    val authRepository = remember { AuthRepository() }
    val userRepository = remember { UserRepository() }
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository, userRepository)
    )

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController, authRepository)
        }

        composable("login") {
            LoginScreen(navController, viewModel)
        }

        composable("register") {
            RegisterScreen(navController, viewModel)
        }

        // Route "home" sekarang langsung ke ProfileScreen
        composable("home") {
            ProfileScreen(navController, viewModel)
        }

        // Route "profile" juga ke ProfileScreen
        composable("profile") {
            ProfileScreen(navController, viewModel)
        }

        // *** Route "edit_profile" DIHAPUS karena sudah digabung ke ProfileScreen ***
        // Jika kode EditProfileScreen dihilangkan, baris ini tidak perlu ada:
        /*
        composable("edit_profile") {
            // EditProfileScreen(navController, viewModel) // Dihapus
        }
        */
    }
}