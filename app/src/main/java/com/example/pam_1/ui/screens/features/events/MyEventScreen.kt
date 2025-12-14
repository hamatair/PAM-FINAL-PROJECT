package com.example.pam_1.ui.screens.features.events

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Event
import com.example.pam_1.data.model.EventCategory
import com.example.pam_1.ui.common.CategoryChip
import com.example.pam_1.ui.common.EventCard
import com.example.pam_1.ui.theme.*
import com.example.pam_1.utils.ImageCompressor
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Terjemahkan label di enum
enum class MyEventStatus(val dbValue: String?, val label: String) {
    ALL(null, "Semua"),
    SCHEDULED("scheduled", "Dijadwalkan"),
    IN_PROGRESS("in_progress", "Berlangsung"),
    DONE("done", "Selesai")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsScreen(
    viewModel: EventViewModel,
    currentUserId: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val eventState by viewModel.eventListState.collectAsState()
    val categoryState by viewModel.categoryListState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf(MyEventStatus.ALL) }
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialog States
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Feedback Handler
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is UiState.Success -> {
                // Pesan Toast tetap dalam bahasa Inggris sesuai data
                Toast.makeText(context, state.data, Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
                showEditDialog = false
                showDeleteDialog = false
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    fun onRefresh() {
        isRefreshing = true
        viewModel.loadEvents(isRefresh = true)
        isRefreshing = false
    }

    val filteredEvents by remember(eventState, selectedStatus, currentUserId) {
        derivedStateOf {
            if (eventState is UiState.Success) {
                val allEvents = (eventState as UiState.Success).data
                val myEvents = allEvents.filter { it.userId == currentUserId }
                if (selectedStatus == MyEventStatus.ALL) myEvents
                else myEvents.filter { it.eventStatus?.equals(selectedStatus.dbValue, ignoreCase = true) == true }
            } else emptyList()
        }
    }

    Scaffold(
        containerColor = BackgroundBeige,
        topBar = {
            TopAppBar(
                // Mengganti judul TopAppBar
                title = { Text("Acara Saya", color = TextBlack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Mengganti contentDescription
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = PrimaryBrown)
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

                // Filter Chips menggunakan label dari enum yang sudah diterjemahkan
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    items(MyEventStatus.entries) { status ->
                        CategoryChip(
                            text = status.label, // Label yang sudah diterjemahkan
                            isSelected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                    }
                }

                // List Content
                when (val state = eventState) {
                    is UiState.Loading -> {
                        if (filteredEvents.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBrown) }
                    }
                    is UiState.Success, UiState.Idle -> {
                        if (filteredEvents.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) {
                                // Pesan jika tidak ada event
                                Text("Tidak ada event.", color = TextGray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(filteredEvents) { event ->
                                    EventItemWithActions(
                                        event = event,
                                        onClick = { event.eventId?.let { onNavigateToDetail(it) } },
                                        onEditClick = {
                                            selectedEvent = event
                                            showEditDialog = true
                                        },
                                        onDeleteClick = {
                                            selectedEvent = event
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = DangerRed) }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. DELETE DIALOG
    if (showDeleteDialog && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            // Mengganti judul
            title = { Text("Hapus Event") },
            // Mengganti teks konfirmasi
            text = { Text("Yakin ingin menghapus '${selectedEvent?.eventName}'?") },
            confirmButton = {
                Button(
                    onClick = { selectedEvent?.eventId?.let { viewModel.deleteEvent(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Hapus") } // Tombol Hapus
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } // Tombol Batal
            }
        )
    }

    // 2. EDIT DIALOG (UPDATED WITH PICKERS)
    if (showEditDialog && selectedEvent != null) {
        EditEventDialog(
            event = selectedEvent!!,
            categories = categoryState,
            onDismiss = { showEditDialog = false },
            onSave = { formData, imageBytes ->
                viewModel.updateEvent(
                    eventId = selectedEvent!!.eventId ?: "",
                    name = selectedEvent!!.eventName,
                    desc = formData.description,
                    location = formData.location,
                    date = formData.date,
                    startTime = formData.startTime,
                    endTime = formData.endTime,
                    selectedCategoryIds = formData.categoryIds,
                    imageBytes = imageBytes, // Sudah byte array
                    currentImageUrl = selectedEvent!!.eventImageUrl
                )
            },
            isLoading = actionState is UiState.Loading
        )
    }
}

// --- EDIT DIALOG COMPONENT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: Event,
    categories: List<EventCategory>,
    onDismiss: () -> Unit,
    onSave: (EditEventFormData, ByteArray?) -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Form States
    var desc by remember { mutableStateOf(event.eventDescription) }
    var location by remember { mutableStateOf(event.eventLocation) }

    // Date & Time States
    var date by remember { mutableStateOf(event.eventDate) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }

    // Picker Visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val selectedCategories = remember { mutableStateListOf<String>().apply { addAll(event.categoryIds) } }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // --- DATE PICKER LOGIC ---
    val datePickerState = rememberDatePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        date = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK", color = PrimaryBrown) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = TextGray) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // --- TIME PICKER LOGIC ---
    // (Asumsi TimePickerDialog sudah ada, hanya menampilkan teks)
    // ...

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Mengganti judul dialog
                Text("Ubah Event", style = MaterialTheme.typography.headlineSmall, color = PrimaryBrown, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Gambar
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else if (event.eventImageUrl != null) AsyncImage(model = event.eventImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.DateRange, null, tint = Color.Gray)

                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)) {
                            // Mengganti teks overlay
                            Text("Ketuk untuk ganti gambar", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    // 2. Nama (Locked)
                    OutlinedTextField(
                        value = event.eventName, onValueChange = {},
                        // Mengganti label
                        label = { Text("Nama (Terkunci)") },
                        modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextBlack, disabledBorderColor = Color.LightGray)
                    )

                    // 3. Deskripsi & Lokasi
                    // Mengganti label
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Lokasi") }, modifier = Modifier.fillMaxWidth())

                    // 4. DATE PICKER (READ ONLY CLICKABLE)
                    ClickableReadOnlyTextField(
                        value = date,
                        // Mengganti label
                        label = "Tanggal (YYYY-MM-DD)",
                        icon = Icons.Default.CalendarToday,
                        onClick = { showDatePicker = true }
                    )

                    // 5. TIME PICKERS (READ ONLY CLICKABLE)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            ClickableReadOnlyTextField(
                                value = startTime,
                                // Mengganti label
                                label = "Mulai",
                                icon = Icons.Default.Schedule,
                                onClick = { showStartTimePicker = true }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            ClickableReadOnlyTextField(
                                value = endTime,
                                // Mengganti label
                                label = "Selesai",
                                icon = Icons.Default.Schedule,
                                onClick = { showEndTimePicker = true }
                            )
                        }
                    }

                    // 6. Kategori
                    Text("Kategori", style = MaterialTheme.typography.labelLarge)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            val isSelected = selectedCategories.contains(category.categoryId)
                            FilterChip(
                                selected = isSelected,
                                onClick = { if (isSelected) selectedCategories.remove(category.categoryId) else selectedCategories.add(category.categoryId) },
                                label = { Text(category.categoryName) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBrown, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    if (errorMessage != null) Text(errorMessage!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }

                // Footer
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = TextGray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (desc.isBlank() || location.isBlank() || date.isBlank() || startTime.isBlank()) {
                                // Mengganti pesan error
                                errorMessage = "Mohon lengkapi semua data."
                            } else {
                                // PERBAIKAN: Kompress dulu sebelum kirim
                                scope.launch {
                                    val imageBytes = selectedImageUri?.let { uri ->
                                        withContext(Dispatchers.IO) {
                                            ImageCompressor.compressImage(context, uri)
                                        }
                                    }
                                    onSave(
                                        EditEventFormData(desc, location, date, startTime, endTime, selectedCategories),
                                        imageBytes // Kirim hasil kompresi
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrown),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        // Mengganti teks tombol
                        else Text("Simpan")
                    }
                }
            }
        }
    }
}

// --- REUSABLE HELPER COMPONENTS (COPIED FROM ADD EVENT) ---

@Composable
fun ClickableReadOnlyTextField(
    value: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {}, // Read only
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true, // KEY: Keyboard wont show
        trailingIcon = { Icon(imageVector = icon, contentDescription = null, tint = PrimaryBrown) },
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBrown,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = PrimaryBrown
        ),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            }
    )
}

@Composable
fun EventItemWithActions(
    event: Event,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {

        // Card utama event
        EventCard(
            event = event,
            onClick = onClick
        )

        // Tombol titik tiga (More Options)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {

            // Floating surface untuk ikon
            Surface(
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        // Mengganti contentDescription
                        contentDescription = "Opsi",
                        tint = TextBlack
                    )
                }
            }

            // Dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {

                DropdownMenuItem(
                    // Mengganti teks
                    text = { Text("Ubah") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBrown)
                    },
                    onClick = {
                        expanded = false
                        onEditClick()
                    }
                )

                DropdownMenuItem(
                    // Mengganti teks
                    text = { Text("Hapus", color = DangerRed) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                    },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}


data class EditEventFormData(
    val description: String, val location: String, val date: String,
    val startTime: String, val endTime: String, val categoryIds: List<String>
)