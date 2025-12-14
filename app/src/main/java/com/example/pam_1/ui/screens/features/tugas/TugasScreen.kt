package com.example.pam_1.ui.screens.features.tugas

import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Tugas
import com.example.pam_1.data.repository.TugasRepository
import com.example.pam_1.ui.theme.BackgroundBeige
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.TextBlack
import com.example.pam_1.viewmodel.TugasViewModel
import com.example.pam_1.viewmodel.TugasViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.pam_1.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasScreen(
    viewModel: TugasViewModel = viewModel(
        factory = TugasViewModelFactory(TugasRepository())
    )
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // State Form
    var idEditing by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var deadlineInput by remember { mutableStateOf(viewModel.selectedDateUi.value) }
    var timeInput by remember { mutableStateOf("09:00") }
    var priorityInput by remember { mutableStateOf("Sedang") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    val datePickerState = rememberDatePickerState()
    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            timeInput = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    fun resetForm() {
        idEditing = null
        titleInput = ""
        descInput = ""
        deadlineInput = viewModel.selectedDateUi.value
        timeInput = "09:00"
        priorityInput = "Sedang"
        selectedImageUri = null
    }

    fun openEditForm(tugas: Tugas) {
        idEditing = tugas.id
        titleInput = tugas.title
        descInput = tugas.description
        deadlineInput = viewModel.formatDbToUi(tugas.deadline)
        timeInput = tugas.time
        priorityInput = tugas.priority
        selectedImageUri = tugas.imageUri?.let { Uri.parse(it) }
        showBottomSheet = true
    }

    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    showBottomSheet = true
                },
                containerColor = PrimaryBrown,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            }
        },
        containerColor = BackgroundBeige
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {

            HorizontalCalendar(
                selectedDate = viewModel.selectedDateUi.value,
                onDateSelected = { uiDate ->
                    val dbDate = viewModel.convertUiToDb(uiDate)
                    viewModel.selectedDateDb.value = dbDate
                    viewModel.selectedDateUi.value = uiDate
                    deadlineInput = uiDate
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Jadwal Hari Ini",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilterTabs(
                selectedFilter = viewModel.filterType.value,
                onFilterSelected = { viewModel.filterType.value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val tasks = viewModel.filteredTugasList

                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada tugas pada tanggal ini", color = Color.Gray)
                        }
                    }
                } else {
                    items(tasks) { tugas ->
                        TimelineTaskCard(
                            tugas = tugas,
                            onDelete = { viewModel.removeTugas(tugas) },
                            onEdit = { openEditForm(tugas) },
                            onStatusToggle = { viewModel.toggleStatus(tugas) }
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        if (idEditing == null) "Tugas Baru" else "Edit Tugas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CampusInputField(titleInput, { titleInput = it }, "Judul")
                    Spacer(modifier = Modifier.height(12.dp))
                    CampusInputField(descInput, { descInput = it }, "Deskripsi")

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Prioritas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    PrioritySelector(
                        selectedPriority = priorityInput,
                        onPrioritySelected = { priorityInput = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            DatePickerButton(deadlineInput) { showDatePicker = true }
                        }
                        Box(Modifier.weight(1f)) {
                            TimePickerButton(timeInput) { timePickerDialog.show() }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "Ganti Foto" else "Tambah Foto")
                    }

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .height(100.dp)
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (titleInput.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Judul tidak boleh kosong",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (idEditing == null) {
                                viewModel.addTugas(
                                    context = context,
                                    title = titleInput,
                                    desc = descInput,
                                    deadlineUi = deadlineInput,
                                    time = timeInput,
                                    priority = priorityInput,
                                    imageUri = selectedImageUri
                                )
                            } else {
                                viewModel.updateTugas(
                                    idEditing!!,
                                    titleInput,
                                    descInput,
                                    deadlineInput,
                                    timeInput,
                                    priorityInput,
                                    selectedImageUri
                                )
                            }

                            showBottomSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrown),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (idEditing == null) "Simpan Tugas" else "Perbarui Tugas")
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val localeID = Locale("id", "ID")
                            deadlineInput =
                                SimpleDateFormat("dd MMMM yyyy", localeID).format(Date(it))
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
