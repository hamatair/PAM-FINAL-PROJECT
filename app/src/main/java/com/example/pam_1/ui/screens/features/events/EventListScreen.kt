package com.example.pam_1.ui.screens.features.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.ui.common.CategoryChip
import com.example.pam_1.ui.common.EventCard
import com.example.pam_1.ui.theme.*
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.UiState
// PENTING: Import Extension Functions
import com.example.pam_1.navigations.navigateSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    viewModel: EventViewModel,
    navController: NavController,
    onNavigateToAddEvent: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val eventState by viewModel.eventListState.collectAsState()
    val categoryState by viewModel.categoryListState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    // State Refresh & Scroll
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    fun onRefresh() {
        isRefreshing = true
        viewModel.loadEvents(isRefresh = true)
        isRefreshing = false
    }

    // Logic Infinite Scroll (Load More)
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 1
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !viewModel.isLoadingMore && !viewModel.isLastPage) {
            viewModel.loadEvents(isRefresh = false)
        }
    }

    Scaffold(
        containerColor = BackgroundBeige,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddEvent,
                containerColor = PrimaryBrown,
                contentColor = White
            ) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            // PERUBAHAN UTAMA: Gunakan satu LazyColumn untuk SELURUH layar
            // Ini menjamin layar selalu bisa discroll (untuk refresh) walau isinya kosong
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp) // Padding atas bawah
            ) {

                // --- ITEM 1: HEADER ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Event Aktif",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- ITEM 2: SEARCH BAR ---
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari Event", color = TextGray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryBrown) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBrown,
                            unfocusedBorderColor = InactiveGray,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White
                        ),
                        singleLine = true
                    )
                }

                // --- ITEM 3: CATEGORY CHIPS ---
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            CategoryChip("Semua", selectedCategoryId == null) { selectedCategoryId = null }
                        }
                        items(categoryState) { category ->
                            CategoryChip(category.categoryName, selectedCategoryId == category.categoryId) {
                                selectedCategoryId = category.categoryId
                            }
                        }
                    }
                }

                // --- ITEM 4: CONTENT LIST (LOGIC) ---
                when (val state = eventState) {
                    is UiState.Loading -> {
                        // Loading awal (hanya jika list benar-benar kosong)
                        if (!viewModel.isLastPage && listState.firstVisibleItemIndex == 0) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(300.dp), // Beri tinggi agar terlihat
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PrimaryBrown)
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Text(text = state.message, color = DangerRed)
                            }
                        }
                    }
                    is UiState.Success -> {
                        val activeEvents = viewModel.getActiveEvents()
                        val filteredEvents = activeEvents.filter { event ->
                            val matchSearch = event.eventName.contains(searchQuery, ignoreCase = true)
                            val matchCategory = selectedCategoryId == null || event.categoryIds.contains(selectedCategoryId)
                            matchSearch && matchCategory
                        }

                        if (filteredEvents.isEmpty()) {
                            // KUNCI PERBAIKAN:
                            // Empty State dimasukkan sebagai 'item' di LazyColumn
                            // Menggunakan fillParentMaxHeight(0.5f) agar menempati sisa layar
                            // Sehingga layar tetap memiliki struktur scrollable
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxHeight(0.7f) // Isi 70% sisa layar
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tidak ada event aktif saat ini", color = TextGray)
                                }
                            }
                        } else {
                            // Render List Event
                            items(filteredEvents) { event ->
                                EventCard(event = event) {
                                    event.eventId?.let { onNavigateToDetail(it) }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Loading Indicator (Infinite Scroll)
                        if (viewModel.isLoadingMore && !viewModel.isLastPage) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBrown)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}