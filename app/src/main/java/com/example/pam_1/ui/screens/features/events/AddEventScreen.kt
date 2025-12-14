package com.example.pam_1.ui.screens.features.events

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.pam_1.data.SupabaseClient
import com.example.pam_1.ui.common.CategoryChip
import com.example.pam_1.ui.theme.*
import com.example.pam_1.utils.ImageCompressor
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.UiState
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    viewModel: EventViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryState by viewModel.categoryListState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // User Session
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }

    // --- Form Data States ---
    var eventName by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") } // YYYY-MM-DD
    var startTime by remember { mutableStateOf("") } // HH:mm
    var endTime by remember { mutableStateOf("") }   // HH:mm
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // --- Picker Visibility States ---
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // --- Error States ---
    var nameError by remember { mutableStateOf<String?>(null) }
    var descError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }

    // --- Image State ---
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri // Simpan URI saja dulu
    }
    val scope = rememberCoroutineScope()

    // Effect: Handle Success/Error
    LaunchedEffect(actionState) {
        when (actionState) {
            is UiState.Success -> {
                // Pesan sukses
                Toast.makeText(context, "Event berhasil dibuat!", Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
                onNavigateBack()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((actionState as UiState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    // --- LOGIC: DATE PICKER ---
    val datePickerState = rememberDatePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Konversi Millis ke format YYYY-MM-DD
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        eventDate = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK", color = PrimaryBrown) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = TextGray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- LOGIC: TIME PICKER (Reusable Function) ---
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endTime = String.format("%02d:%02d", hour, minute)
                showEndTimePicker = false
            }
        )
    }


    // --- VALIDATION LOGIC ---
    fun validateAndSubmit() {
        nameError = null; descError = null; locationError = null
        dateError = null; startTimeError = null; endTimeError = null
        var isValid = true

        if (eventName.isBlank()) { nameError = "Nama wajib diisi"; isValid = false }
        if (eventDesc.isBlank()) { descError = "Deskripsi wajib diisi"; isValid = false }
        if (eventLocation.isBlank()) { locationError = "Lokasi wajib diisi"; isValid = false }
        if (eventDate.isBlank()) { dateError = "Tanggal wajib diisi"; isValid = false }
        if (startTime.isBlank()) { startTimeError = "Jam mulai wajib"; isValid = false }
        if (endTime.isBlank()) { endTimeError = "Jam selesai wajib"; isValid = false }

        var parsedDate: LocalDate? = null
        var parsedStartTime: LocalTime? = null

        if (eventDate.isNotBlank()) {
            parsedDate = LocalDate.parse(eventDate)
            if (parsedDate.isBefore(LocalDate.now())) {
                dateError = "Tanggal tidak boleh lampau"; isValid = false
            }
        }

        if (startTime.isNotBlank()) {
            parsedStartTime = LocalTime.parse(startTime)
            if (parsedDate != null && parsedDate == LocalDate.now() && parsedStartTime.isBefore(LocalTime.now())) {
                startTimeError = "Waktu sudah lewat"; isValid = false
            }
        }

        if (endTime.isNotBlank() && parsedStartTime != null) {
            val parsedEndTime = LocalTime.parse(endTime)
            if (!parsedEndTime.isAfter(parsedStartTime)) {
                endTimeError = "Harus setelah jam mulai"; isValid = false
            }
        }

        if (isValid && currentUser != null) {
            // PERBAIKAN: Kompress gambar di background thread
            scope.launch {
                val imageBytes = selectedImageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        ImageCompressor.compressImage(context, uri)
                    }
                }

                viewModel.addEvent(
                    userId = currentUser.id,
                    name = eventName,
                    desc = eventDesc,
                    location = eventLocation,
                    date = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    selectedCategoryIds = selectedCategoryIds,
                    imageBytes = imageBytes // Kirim byte array yang sudah dikompres
                )
            }
        } else if (currentUser == null) {
            // Pesan jika sesi habis
            Toast.makeText(context, "Sesi habis, login ulang", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundBeige,
        topBar = {
            CenterAlignedTopAppBar(
                // Mengganti judul
                title = { Text("Tambah Event", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundBeige
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // 1. Image Picker
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .dashedBorder(PrimaryBrown, 12.dp)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = LightBrown, modifier = Modifier.size(48.dp))
                            // Mengganti teks
                            Text("Unggah Gambar", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    }
                }
            }

            // 2. Input Biasa
            // Mengganti label
            item { CustomTextField(value = eventName, onValueChange = { eventName = it }, label = "Nama Event", errorMsg = nameError) }
            item { CustomTextField(value = eventDesc, onValueChange = { eventDesc = it }, label = "Deskripsi", singleLine = false, minLines = 3, errorMsg = descError) }
            item { CustomTextField(value = eventLocation, onValueChange = { eventLocation = it }, label = "Lokasi", errorMsg = locationError) }

            // 3. Date Picker Field (Read Only)
            item {
                ClickableReadOnlyTextField(
                    value = eventDate,
                    // Mengganti label
                    label = "Tanggal Event",
                    icon = Icons.Default.CalendarToday,
                    onClick = { showDatePicker = true },
                    errorMsg = dateError
                )
            }

            // 4. Time Picker Fields (Read Only)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ClickableReadOnlyTextField(
                            value = startTime,
                            // Mengganti label
                            label = "Mulai",
                            icon = Icons.Default.Schedule,
                            onClick = { showStartTimePicker = true },
                            errorMsg = startTimeError
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ClickableReadOnlyTextField(
                            value = endTime,
                            // Mengganti label
                            label = "Selesai",
                            icon = Icons.Default.Schedule,
                            onClick = { showEndTimePicker = true },
                            errorMsg = endTimeError
                        )
                    }
                }
            }

            // 5. Category
            item {
                // Mengganti judul
                Text("Kategori", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(categoryState) { category ->
                        val isSelected = selectedCategoryIds.contains(category.categoryId)
                        CategoryChip(
                            text = category.categoryName,
                            isSelected = isSelected,
                            onClick = {
                                selectedCategoryIds = if (isSelected) selectedCategoryIds - category.categoryId
                                else selectedCategoryIds + category.categoryId
                            }
                        )
                    }
                }
            }

            // 6. Submit
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        validateAndSubmit() // Panggil fungsi validasi
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrown),
                    shape = RoundedCornerShape(2.dp),
                    enabled = actionState !is UiState.Loading
                ) {
                    if (actionState is UiState.Loading) CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    // Mengganti teks tombol
                    else Text("Simpan Event", style = MaterialTheme.typography.labelLarge, color = White)
                }
            }
        }
    }
}

// --- REUSABLE COMPONENTS ---

// 1. TextField yang tidak bisa diketik, tapi bisa diklik (untuk trigger Picker)
@Composable
fun ClickableReadOnlyTextField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    errorMsg: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {}, // Read only, tidak berubah via typing
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true, // KUNCI UTAMA: Keyboard tidak muncul
            trailingIcon = {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryBrown)
            },
            shape = RoundedCornerShape(8.dp),
            isError = errorMsg != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBrown,
                unfocusedBorderColor = InactiveGray,
                focusedLabelColor = PrimaryBrown,
                errorBorderColor = DangerRed
            ),
            // Menggunakan InteractionSource untuk mendeteksi klik
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) {
                                onClick() // Trigger dialog saat ditekan
                            }
                        }
                    }
                }
        )
        if (errorMsg != null) {
            Text(text = errorMsg, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// 2. Custom Time Picker Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timeState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) {
                Text("OK", color = PrimaryBrown)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextGray)
            }
        },
        text = {
            // Widget Jam Bawaan Material 3
            TimePicker(state = timeState)
        }
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    errorMsg: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            isError = errorMsg != null,
            singleLine = singleLine,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBrown,
                unfocusedBorderColor = InactiveGray,
                focusedLabelColor = PrimaryBrown,
                errorBorderColor = DangerRed
            )
        )
        if (errorMsg != null) {
            Text(text = errorMsg, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun Modifier.dashedBorder(color: Color, cornerRadius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
    drawRoundRect(color = color, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()))
}