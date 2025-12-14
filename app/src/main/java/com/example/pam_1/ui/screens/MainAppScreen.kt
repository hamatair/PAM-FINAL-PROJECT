package com.example.pam_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.EventRepository
import com.example.pam_1.data.repository.TugasRepository // IMPORT INI
import com.example.pam_1.navigations.NavigationItem
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.ui.common.AnimatedBottomNavigationBar
import com.example.pam_1.ui.screens.features.events.EventListScreen
import com.example.pam_1.ui.screens.features.group_chat.StudyGroupListScreen
import com.example.pam_1.ui.screens.features.tugas.TugasScreen
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.EventViewModelFactory
import com.example.pam_1.viewmodel.StudyGroupViewModel
import com.example.pam_1.viewmodel.TugasViewModel
import com.example.pam_1.viewmodel.TugasViewModelFactory // IMPORT INI

// ==========================
// TOP APP BAR
// ==========================
@Composable
private fun AppToolbar(navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "PAM App",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(
                onClick = { navController.navigateSafe("profile") },
                modifier = Modifier.size(24.dp)  // Ukuran lebih kecil
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================
// MAIN APP SCREEN (MERGED)
// ==========================
@Composable
fun MainAppScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    studyGroupViewModel: StudyGroupViewModel
) {
    // --- restore tab terakhir ---
    val initialTab = authViewModel.lastActiveTab.collectAsState().value
    var currentTab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(currentTab) {
        authViewModel.setLastActiveTab(currentTab)
    }

    // --- Event ViewModel (shared di MainApp) ---
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventRepository)
    )

    Scaffold(
        topBar = { AppToolbar(navController) },
        bottomBar = {
            AnimatedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { selected -> currentTab = selected }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(top = 64.dp ,bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                // ================= TUGAS TAB =================
                NavigationItem.Tugas.route -> {
                    // PERBAIKAN DISINI: Gunakan Factory untuk TugasViewModel
                    val tugasRepository = remember { TugasRepository() }
                    val tugasViewModel: TugasViewModel = viewModel(
                        factory = TugasViewModelFactory(tugasRepository)
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        TugasScreen(viewModel = tugasViewModel)
                    }
                }

                NavigationItem.Keuangan.route ->
                    DummyScreen("Halaman Keuangan")

                // ================= EVENT TAB =================
                NavigationItem.Event.route -> {
                    EventListScreen(
                        viewModel = eventViewModel,
                        navController = navController,
                        onNavigateToAddEvent = {
                            navController.navigateSafe("add_event")
                        },
                        onNavigateToDetail = { eventId ->
                            navController.navigateSafe("event_detail/$eventId")
                        }
                    )
                }

                // ================= GROUP TAB =================
                NavigationItem.Grup.route -> {
                    StudyGroupListScreen(
                        navController = navController,
                        viewModel = studyGroupViewModel
                    )
                }

                NavigationItem.Catatan.route ->
                    DummyScreen("Halaman Catatan")

                else ->
                    DummyScreen("Halaman Tugas")
            }
        }
    }
}

// ==========================
// DUMMY SCREEN
// ==========================
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
            style = MaterialTheme.typography.headlineMedium
        )
    }
}