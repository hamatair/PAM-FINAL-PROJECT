package com.example.pam_1.navigations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.pam_1.data.repository.NoteRepository
import com.example.pam_1.data.repository.UserRepository
import com.example.pam_1.ui.screens.*
import com.example.pam_1.ui.screens.features.auth.*
import com.example.pam_1.ui.screens.features.events.*
import com.example.pam_1.ui.screens.features.notes.AddEditNoteScreen
import com.example.pam_1.ui.screens.features.notes.NotesListScreen
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

    val noteRepository = remember { NoteRepository(SupabaseClient.client) }
    val noteViewModelFactory = remember { NoteViewModelFactory(noteRepository) }


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

        // ================= NOTES =================

//        composable("notes") {
//            val noteViewModel: NoteViewModel =
//                viewModel(factory = noteViewModelFactory)
//
//            LaunchedEffect(Unit) {
//                noteViewModel.loadNotes()
//            }
//
//            NotesListScreen(
//                viewModel = noteViewModel,
//                onAddNote = {
//                    navController.navigateSafe("note/add")
//                },
//                onNoteClick = { id ->
//                    navController.navigateSafe("note/read/$id")
//                }
//            )
//        }

// ---------- READ NOTE ----------
        composable(
            route = "note/read/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->

            val noteId = backStackEntry.arguments!!.getLong("noteId")

            val noteViewModel: NoteViewModel =
                viewModel(factory = noteViewModelFactory)

            val noteState = noteViewModel.noteDetailState.collectAsState()

            LaunchedEffect(noteId) {
                noteViewModel.loadNoteDetail(noteId) // ✅ BENAR
            }

            if (noteState.value is UiState.Success) {
                val note = (noteState.value as UiState.Success).data

                if (note != null) {
                    ReadNoteScreen(
                        note = note, // ✅ KIRIM OBJECT NOTE
                        onBack = {
                            navController.popBackStackSafe()
                        },
                        onEditNote = { id ->
                            navController.navigateSafe("note/edit/$id")
                        },
                        onPinToggle = { pinned ->
                            noteViewModel.updateNote(
                                noteId = note.id!!,
                                title = note.title,
                                description = note.description,
                                isPinned = pinned,
                                imageBytes = null,
                                currentImageUrl = note.imageUrl
                            )
                        },
                        onDelete = { id ->
                            noteViewModel.deleteNote(id)
                            navController.popBackStackSafe()
                        }
                    )
                }
            }
        }


        // ---------- ADD NOTE ----------
        composable("note/add") {
            val noteViewModel: NoteViewModel =
                viewModel(factory = noteViewModelFactory)

            AddEditNoteScreen(
                note = null,
                onBack = {
                    navController.popBackStackSafe()
                },
                onSave = { title, description, isPinned ->
                    noteViewModel.addNote(
                        title = title,
                        description = description,
                        isPinned = isPinned,
                        imageBytes = null
                    )
                    navController.popBackStackSafe()
                },
                onAddImage = { }
            )
        }


// ---------- EDIT NOTE ----------
        // ---------- EDIT NOTE ----------
        composable(
            route = "note/edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->

            val noteId = backStackEntry.arguments!!.getLong("noteId")
            val noteViewModel: NoteViewModel =
                viewModel(factory = noteViewModelFactory)

            val noteState = noteViewModel.noteDetailState.collectAsState()

            LaunchedEffect(noteId) {
                noteViewModel.loadNoteDetail(noteId) // ✅ FIX
            }

            if (noteState.value is UiState.Success) {
                val note = (noteState.value as UiState.Success).data ?: return@composable

                AddEditNoteScreen(
                    note = note,
                    onBack = {
                        noteViewModel.clearNoteDetail()
                        navController.popBackStackSafe()
                    },
                    onSave = { title, description, isPinned ->
                        noteViewModel.updateNote(
                            noteId = noteId,
                            title = title,
                            description = description,
                            isPinned = isPinned,
                            imageBytes = null,
                            currentImageUrl = note.imageUrl // ✅ WAJIB
                        )
                        navController.popBackStackSafe()
                    },
                    onAddImage = { }
                )
            }
        }






    }
}