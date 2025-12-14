package com.example.pam_1.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Tugas
import com.example.pam_1.ui.theme.DangerRed
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.*

// --- VARIABEL GLOBAL ---
// Dipindahkan kesini agar bisa diakses oleh PrioritySelector
val priorityOptions = listOf("Rendah", "Sedang", "Tinggi")

// --- KOMPONEN KALENDER HORIZONTAL ---
// selectedDate: UI format (dd MMMM yyyy)
@Composable
fun HorizontalCalendar(selectedDate: String, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    // Generate 7 hari mulai hari ini
    val days = (0..6).map {
        val date = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        date
    }

    val localeID = Locale.forLanguageTag("id-ID")
    val fullFormat = SimpleDateFormat("dd MMMM yyyy", localeID)
    val dayNameFormat = SimpleDateFormat("EEE", localeID)
    val dayNumberFormat = SimpleDateFormat("dd", localeID)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        items(days) { date ->
            val dateString = fullFormat.format(date)
            val isSelected = dateString == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) PrimaryBrown else Color.White)
                    .clickable { onDateSelected(dateString) }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayNameFormat.format(date),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayNumberFormat.format(date),
                    color = if (isSelected) Color.White else TextBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// --- TAB FILTER ---
@Composable
fun FilterTabs(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    // Bahasa Indonesia: sesuai ViewModel yang aku refactor
    val filters = listOf("Belum Selesai", "Selesai")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) TextBlack else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// --- KARTU TIMELINE TUGAS ---
@Composable
fun TimelineTaskCard(
    tugas: Tugas,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStatusToggle: () -> Unit
) {
    val (tagColor, tagText) = when (tugas.priority) {
        "Tinggi" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "Sedang" -> Color(0xFFFFFDE7) to Color(0xFFFBC02D)
        else -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = tugas.time,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFFE0E0E0)))
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.weight(1f).padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = tagColor, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = tugas.priority,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = tagText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, if(tugas.isCompleted) PrimaryBrown else Color.LightGray, CircleShape)
                            .background(if(tugas.isCompleted) PrimaryBrown else Color.Transparent)
                            .clickable { onStatusToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tugas.isCompleted) {
                            Icon(Icons.Default.Check, "Selesai", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tugas.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if(tugas.isCompleted) Color.Gray else TextBlack,
                    textDecoration = if(tugas.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = tugas.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )

                if (tugas.imageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = tugas.imageUri,
                        contentDescription = "Gambar Tugas",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit Tugas", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, "Hapus Tugas", tint = DangerRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// --- PEMILIH PRIORITAS ---
@Composable
fun PrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        priorityOptions.forEach { priority ->
            val isSelected = priority == selectedPriority
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PrimaryBrown else Color.LightGray.copy(alpha = 0.3f))
                    .clickable { onPrioritySelected(priority) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = priority,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// --- TOMBOL PEMILIH TANGGAL ---
@Composable
fun DatePickerButton(dateText: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (dateText.isEmpty()) "Pilih Tanggal" else dateText, color = TextBlack)
            Icon(Icons.Outlined.CalendarToday, "Pilih Tanggal", tint = PrimaryBrown)
        }
    }
}

// --- TOMBOL PEMILIH JAM ---
@Composable
fun TimePickerButton(timeText: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (timeText.isEmpty()) "Pilih Jam" else timeText, color = TextBlack)
            Icon(Icons.Outlined.Schedule, "Pilih Jam", tint = PrimaryBrown)
        }
    }
}

// --- FIELD INPUT KAMPUS ---
@Composable
fun CampusInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBrown,
                unfocusedBorderColor = Color.LightGray
            )
        )
        if (readOnly && onClick != null) {
            Box(Modifier.matchParentSize().clickable(onClick = onClick))
        }
    }
}
