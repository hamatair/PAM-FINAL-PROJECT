package com.example.pam_1.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.pam_1.data.repository.TugasRepository
import com.example.pam_1.navigations.NavigationItem
import com.example.pam_1.navigations.navigateSafe
import com.example.pam_1.ui.common.AnimatedBottomNavigationBar
import com.example.pam_1.ui.screens.features.events.EventListScreen
import com.example.pam_1.ui.screens.features.finance.ExpenseHomeScreen
import com.example.pam_1.ui.screens.features.group_chat.StudyGroupListScreen
import com.example.pam_1.ui.screens.features.notes.NotesListScreen
import com.example.pam_1.ui.screens.features.tugas.TugasScreen
import com.example.pam_1.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.painterResource
import com.example.pam_1.R

// ==========================
// TOP APP BAR
// ==========================
@Composable
private fun AppToolbar(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // **BAGIAN KIRI: Icon Aplikasi dan Nama Aplikasi**
            Row(
                verticalAlignment = Alignment.CenterVertically // Pusatkan logo dan teks secara vertikal
            ) {
                // 1. Icon Aplikasi (Logo)
                // Kita gunakan painterResource dan merujuk ke drawable app_logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo), // <-- INI YANG DITAMBAHKAN
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(32.dp) // Ukuran icon, sesuaikan jika perlu
                )

                Spacer(modifier = Modifier.width(8.dp)) // Jarak antara logo dan teks

                // 2. Nama Aplikasi (Teks)
                Text(
                    text = "Unify",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // **BAGIAN KANAN: Icon Profil (Tidak Berubah)**
            IconButton(
                onClick = { navController.navigateSafe("profile") }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            }
        }
    }

// ==========================
// MAIN APP SCREEN (FINAL MERGE)
// ==========================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    studyGroupViewModel: StudyGroupViewModel,
    noteViewModel: NoteViewModel,
    expenseViewModel: ExpenseViewModel
) {
    // ---------- TAB SETUP ----------
    val tabs = listOf(
        NavigationItem.Tugas.route,
        NavigationItem.Keuangan.route,
        NavigationItem.Grup.route,
        NavigationItem.Catatan.route,
        NavigationItem.Event.route
    )

    val lastTab by authViewModel.lastActiveTab.collectAsState()
    val initialIndex = tabs.indexOf(lastTab).takeIf { it >= 0 } ?: 0

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { tabs.size }
    )

    var currentTab by remember {
        mutableStateOf(tabs[initialIndex])
    }

    val coroutineScope = rememberCoroutineScope()

    // Sync pager -> bottom nav
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                val tab = tabs[page]
                currentTab = tab
                authViewModel.setLastActiveTab(tab)
            }
    }

    // ---------- EVENT VM ----------
    val eventRepository = remember { EventRepository(SupabaseClient.client) }
    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventRepository)
    )

    Scaffold(
        topBar = { AppToolbar(navController) },
        bottomBar = {
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

                // ================= TUGAS =================
                NavigationItem.Tugas.route -> {
                    val repo = remember { TugasRepository() }
                    val tugasViewModel: TugasViewModel = viewModel(
                        factory = TugasViewModelFactory(repo)
                    )
                    TugasScreen(viewModel = tugasViewModel)
                }

                // ================= KEUANGAN =================
                NavigationItem.Keuangan.route -> {
                    ExpenseHomeScreen(
                        viewModel = expenseViewModel,
                        onAddExpenseClick = {
                            navController.navigateSafe("add_expense")
                        },
                        onExpenseClick = { id ->
                            navController.navigateSafe("expense_detail/$id")
                        }
                    )
                }

                // ================= GROUP =================
                NavigationItem.Grup.route -> {
                    StudyGroupListScreen(
                        navController = navController,
                        viewModel = studyGroupViewModel
                    )
                }

                // ================= CATATAN =================
                NavigationItem.Catatan.route -> {
                    NotesListScreen(
                        viewModel = noteViewModel,
                        onAddNote = {
                            navController.navigateSafe("note/add")
                        },
                        onNoteClick = { id ->
                            navController.navigateSafe("note/read/$id")
                        }
                    )
                }

                // ================= EVENT =================
                NavigationItem.Event.route -> {
                    EventListScreen(
                        viewModel = eventViewModel,
                        navController = navController,
                        onNavigateToAddEvent = {
                            navController.navigateSafe("add_event")
                        },
                        onNavigateToDetail = { id ->
                            navController.navigateSafe("event_detail/$id")
                        }
                    )
                }
            }
        }
    }
}
}
