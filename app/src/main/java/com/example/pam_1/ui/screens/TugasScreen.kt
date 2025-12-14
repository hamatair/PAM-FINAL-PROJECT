package com.example.pam_1.ui.screens

import android.app.TimePickerDialog
import android.net.Uri
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
import com.example.pam_1.ui.theme.BackgroundBeige
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.TextBlack
import com.example.pam_1.viewmodel.TugasViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.pam_1.ui.common.HorizontalCalendar
import com.example.pam_1.ui.common.FilterTabs
import com.example.pam_1.ui.common.TimelineTaskCard
import com.example.pam_1.ui.common.CampusInputField
import com.example.pam_1.ui.common.PrioritySelector
import com.example.pam_1.ui.common.DatePickerButton
import com.example.pam_1.ui.common.TimePickerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasScreen(viewModel: TugasViewModel = viewModel()) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Form Input States
    var idEditing by remember { mutableStateOf<Long?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var deadlineInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("09:00") } // Default jam
    var priorityInput by remember { mutableStateOf("Medium") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    val datePickerState = rememberDatePickerState()

    // Setup Time Picker Dialog
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            timeInput = String.format("%02d:%02d", hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true // 24 jam format
    )

    fun resetForm() {
        idEditing = null; titleInput = ""; descInput = "";
        deadlineInput = viewModel.selectedDate.value
        timeInput = "09:00"
        priorityInput = "Medium"; selectedImageUri = null
    }

    fun openEditForm(tugas: Tugas) {
        idEditing = tugas.id; titleInput = tugas.title; descInput = tugas.description
        deadlineInput = tugas.deadline; timeInput = tugas.time // Load jam
        priorityInput = tugas.priority
        selectedImageUri = tugas.imageUri?.let { Uri.parse(it) }
        showBottomSheet = true
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Schedule", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundBeige)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { resetForm(); showBottomSheet = true }, containerColor = PrimaryBrown, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, null) }
        },
        containerColor = BackgroundBeige
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)) {
            // 1. KALENDER ATAS
            HorizontalCalendar(selectedDate = viewModel.selectedDate.value, onDateSelected = { viewModel.selectedDate.value = it })

            Text("Schedule Today", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextBlack)
            Spacer(modifier = Modifier.height(16.dp))

            // 2. TAB FILTER
            FilterTabs(selectedFilter = viewModel.filterType.value, onFilterSelected = { viewModel.filterType.value = it })

            // 3. LIST TUGAS
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                val tasks = viewModel.filteredTugasList
                if (tasks.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) { Text("No tasks for this date", color = Color.Gray) } }
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

        // --- POP-UP INPUT ---
        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, containerColor = Color.White) {
                Column(modifier = Modifier.padding(20.dp).padding(bottom = 20.dp)) {
                    Text(if (idEditing == null) "New Task" else "Edit Task", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    CampusInputField(titleInput, { titleInput = it }, "Title")
                    Spacer(modifier = Modifier.height(12.dp))
                    CampusInputField(descInput, { descInput = it }, "Description")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Priority", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    PrioritySelector(priorityInput, { priorityInput = it })

                    Spacer(modifier = Modifier.height(12.dp))

                    // Baris Tanggal & Jam Berdampingan
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { DatePickerButton(deadlineInput, { showDatePicker = true }) }
                        Box(Modifier.weight(1f)) { TimePickerButton(timeInput, { timePickerDialog.show() }) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Image, null); Spacer(modifier = Modifier.width(8.dp))
                        Text(if(selectedImageUri!=null) "Change Photo" else "Add Photo")
                    }
                    if(selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription=null, modifier = Modifier.height(80.dp).padding(top=8.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (idEditing == null) viewModel.addTugas(titleInput, descInput, deadlineInput, timeInput, priorityInput, selectedImageUri)
                            else viewModel.updateTugas(idEditing!!, titleInput, descInput, deadlineInput, timeInput, priorityInput, selectedImageUri)
                            showBottomSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrown), shape = RoundedCornerShape(12.dp)
                    ) { Text("Save Task") }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { deadlineInput = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            }) { DatePicker(state = datePickerState) }
        }
    }
}
