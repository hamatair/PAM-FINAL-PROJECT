package com.example.pam_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.EventRepository
import com.example.pam_1.data.repository.NoteRepository
import com.example.pam_1.navigations.NavigationItem
// PENTING: Import extension function navigateSafe yang sudah dibuat
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.ui.common.AnimatedBottomNavigationBar
import com.example.pam_1.ui.screens.features.events.EventListScreen
import com.example.pam_1.ui.screens.features.notes.NotesListScreen
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.EventViewModelFactory
import com.example.pam_1.viewmodel.NoteViewModel
import com.example.pam_1.viewmodel.NoteViewModelFactory

@Composable
fun MainAppScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    noteViewModel: NoteViewModel
) {
    // --- State Management untuk Tab Aktif ---
    val initialTab = viewModel.lastActiveTab.collectAsState().value
    var currentTab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(currentTab) {
        viewModel.setLastActiveTab(currentTab)

        if (currentTab == NavigationItem.Catatan.route) {
            noteViewModel.loadNotes()
        }
    }

    // Inisialisasi ViewModel untuk Event di dalam MainApp
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventRepository)
    )
//    val noteRepository = remember { NoteRepository(SupabaseClient.client) }
//    val noteViewModel: NoteViewModel = viewModel(
//        factory = NoteViewModelFactory(noteRepository)
//    )


    Scaffold(
        bottomBar = {
            AnimatedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { selected ->
                    currentTab = selected
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                NavigationItem.Tugas.route -> DummyScreen("Halaman Tugas")
                NavigationItem.Keuangan.route -> DummyScreen("Halaman Keuangan")
                NavigationItem.Grup.route -> DummyScreen("Halaman Grup")
                NavigationItem.Catatan.route -> {
                    NotesListScreen(
                        viewModel = noteViewModel, // Gunakan parameter yang dikirim
                        onAddNote = {
                            navController.navigateSafe("note/add")
                        },
                        onNoteClick = { noteId ->
                            navController.navigateSafe("note/read/$noteId")
                        }
                    )
                }

                // --- TAB EVENT ---
                NavigationItem.Event.route -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                        EventListScreen(
                            viewModel = eventViewModel,
                            navController = navController,

                            // UBAH DISINI: Gunakan navigateSafe untuk mencegah double click
                            onNavigateToAddEvent = {
                                navController.navigateSafe("add_event")
                            },

                            // UBAH DISINI: Gunakan navigateSafe
                            onNavigateToDetail = { eventId ->
                                navController.navigateSafe("event_detail/$eventId")
                            },
                        )
                    }
                }
                else -> DummyScreen("Halaman Tugas")
            }
        }
    }
}

// Komponen Dummy (Tetap sama)
@Composable
fun DummyScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}