package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.AuthRepository
import com.example.pam_1.data.repository.EventRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.ui.screens.*
import com.example.pam_1.ui.screens.features.auth.*
import com.example.pam_1.ui.screens.features.events.*
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.AuthViewModelFactory
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.EventViewModelFactory
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Repository Setup
    val authRepository = remember { AuthRepository(context) }
    val userRepository = remember { UserRepository() }

    // Auth ViewModel Setup
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            authRepository = authRepository,
            userRepository = userRepository
        )
    )

    // Factory Event (Di-share ke screen yang butuh EventViewModel)
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModelFactory = remember { EventViewModelFactory(eventRepository) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // --- AUTH SECTION ---
        // (Section ini jarang menyebabkan crash double tap, tapi aman menggunakan navigate biasa)
        composable("splash") { SplashScreen(navController, authRepository) }
        composable("login") { LoginScreen(navController, authViewModel) }
        composable("register") { RegisterScreen(navController, authViewModel) }
        composable("otp_verification") { OTPVerificationScreen(navController, authViewModel) }
        composable("forgot_password") { ForgotPasswordScreen(navController, authViewModel) }
        composable("new_password") { NewPasswordScreen(navController, authViewModel) }
        composable("profile") { ProfileScreen(navController, authViewModel) }

        // --- MAIN FEATURES ---

        // 1. HOME (MainAppScreen dengan Bottom Nav)
        composable("home") {
            // MainAppScreen menerima navController, pastikan di dalam MainAppScreen
            // Anda juga menggunakan .navigateSafe() jika memanggil navigasi
            MainAppScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        // 2. ADD EVENT
        composable("add_event") {
            val eventViewModel: EventViewModel = viewModel(factory = eventViewModelFactory)
            AddEventScreen(
                viewModel = eventViewModel,
                // FIX: Gunakan popBackStackSafe
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        // 3. MY EVENTS (Event Saya)
        composable("my_events") {
            val eventViewModel: EventViewModel = viewModel(factory = eventViewModelFactory)
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

            MyEventsScreen(
                viewModel = eventViewModel,
                currentUserId = currentUserId,
                // FIX: Gunakan navigateSafe
                onNavigateToDetail = { eventId ->
                    navController.navigateSafe("event_detail/$eventId")
                },
                // FIX: Gunakan popBackStackSafe
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }

        // 4. EVENT DETAIL
        composable(
            route = "event_detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventViewModel = viewModel(factory = eventViewModelFactory)

            DetailEventScreen(
                eventId = eventId,
                viewModel = eventViewModel,
                // FIX: Gunakan popBackStackSafe
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
    }
}