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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.data.repository.EventRepository
import com.example.pam_1.data.repository.TugasRepository
import com.example.pam_1.navigations.NavigationItem
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.ui.common.AnimatedBottomNavigationBar
import com.example.pam_1.ui.screens.features.events.EventListScreen
import com.example.pam_1.ui.screens.features.group_chat.StudyGroupListScreen
import com.example.pam_1.ui.screens.features.notes.NotesListScreen
import com.example.pam_1.ui.screens.features.tugas.TugasScreen
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.EventViewModelFactory
import com.example.pam_1.viewmodel.NoteViewModel
import com.example.pam_1.viewmodel.StudyGroupViewModel
import com.example.pam_1.viewmodel.TugasViewModel
import com.example.pam_1.viewmodel.TugasViewModelFactory

// accompanist pager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.ExperimentalFoundationApi

// coroutines / flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppScreen(
    navController: NavController,
    viewModel: AuthViewModel,          // tetap ada jika dipakai
    noteViewModel: NoteViewModel,
    authViewModel: AuthViewModel,      // kamu nampaknya punya dua param auth, biarkan agar kompatibel
    studyGroupViewModel: StudyGroupViewModel
) {
    // --- restore tab terakhir ---
    val initialTab by authViewModel.lastActiveTab.collectAsState()
    val tabs = listOf(
        NavigationItem.Tugas.route,
        NavigationItem.Keuangan.route,
        NavigationItem.Grup.route,
        NavigationItem.Catatan.route,
        NavigationItem.Event.route
    )

    val initialIndex = tabs.indexOf(initialTab).takeIf { it >= 0 } ?: 0

    // Pager state (accompanist)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { tabs.size }
    )

    // currentTab di-sync dari pagerState
    var currentTab by remember { mutableStateOf(tabs.getOrElse(initialIndex) { tabs.first() }) }

    val coroutineScope = rememberCoroutineScope()

    // Sync: saat pager berpindah (swipe), update currentTab & simpan ke authViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                val newTab = tabs.getOrElse(page) { tabs.first() }
                currentTab = newTab
                authViewModel.setLastActiveTab(newTab)
            }
    }

    // --- Event ViewModel (shared di MainApp) ---
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventRepository)
    )

    Scaffold(
        topBar = { AppToolbar(navController) },
        bottomBar = {
            // Ketika user tap bottom bar -> scroll pager ke page yang sesuai
            AnimatedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { selected ->
                    val index = tabs.indexOf(selected).takeIf { it >= 0 } ?: 0
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->

        // CONTENT: Pager yang bisa di-swipe (accompanist)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(
                    top = 64.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) { page ->
        when (tabs[page]) {
                // ================= TUGAS TAB =================
                NavigationItem.Tugas.route -> {
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

                NavigationItem.Keuangan.route -> {
                    DummyScreen("Halaman Keuangan")
                }

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

                NavigationItem.Catatan.route -> {
                    NotesListScreen(
                        viewModel = noteViewModel, // Menggunakan Shared ViewModel
                        onAddNote = {
                            navController.navigateSafe("note/add")
                        },
                        onNoteClick = { noteId ->
                            navController.navigateSafe("note/read/$noteId")
                        }
                    )
                }

                else -> DummyScreen("Halaman Tugas")
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
