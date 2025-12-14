package com.example.pam_1.ui.common

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.Tugas
import com.example.pam_1.ui.theme.DangerRed
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.TextBlack
import java.text.SimpleDateFormat
import java.util.*

// --- KOMPONEN KALENDER (Tetap) ---
@Composable
fun HorizontalCalendar(selectedDate: String, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val days = (0..6).map {
        val date = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        date
    }
    val localeID = Locale.forLanguageTag("id-ID")
    val fullFormat = SimpleDateFormat("dd MMMM yyyy", localeID)
    val dayNameFormat = SimpleDateFormat("EEE", localeID)
    val dayNumberFormat = SimpleDateFormat("dd", localeID)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 16.dp)) {
        items(days) { date ->
            val dateString = fullFormat.format(date)
            val isSelected = dateString == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) PrimaryBrown else Color.White)
                    .clickable { onDateSelected(dateString) }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dayNameFormat.format(date), color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(dayNumberFormat.format(date), color = if (isSelected) Color.White else TextBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --- TAB FILTER (Tetap) ---
@Composable
fun FilterTabs(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("To Do", "Completed").forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color.White else Color.Transparent).clickable { onFilterSelected(filter) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(filter, color = if (isSelected) TextBlack else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// --- TIMELINE CARD (Updated: Pake Jam Asli) ---
@Composable
fun TimelineTaskCard(
    tugas: Tugas,
    // Hapus parameter 'time' manual, kita ambil dari tugas.time
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStatusToggle: () -> Unit
) {
    val (tagColor, tagText) = when (tugas.priority) {
        "High" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "Medium" -> Color(0xFFFFFDE7) to Color(0xFFFBC02D)
        else -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Kolom Kiri: JAM ASLI
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
            Text(text = tugas.time, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium) // Ambil dari model
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFFE0E0E0)))
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Kartu
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = tagColor, shape = RoundedCornerShape(6.dp)) {
                        Text(tugas.priority, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = tagText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).border(2.dp, if(tugas.isCompleted) PrimaryBrown else Color.LightGray, CircleShape).background(if(tugas.isCompleted) PrimaryBrown else Color.Transparent).clickable { onStatusToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tugas.isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(tugas.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(tugas.isCompleted) Color.Gray else TextBlack, textDecoration = if(tugas.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                Text(tugas.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)

                if (tugas.imageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(model = tugas.imageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, null, tint = DangerRed, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

// Komponen Pendukung
@Composable
fun PrioritySelector(selected: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("High", "Medium", "Low").forEach { priority ->
            val isSelected = selected == priority
            val color = if(priority=="High") Color(0xFFFFCDD2) else if(priority=="Medium") Color(0xFFFFF9C4) else Color(0xFFC8E6C9)
            val textColor = if(priority=="High") Color(0xFFD32F2F) else if(priority=="Medium") Color(0xFFFBC02D) else Color(0xFF388E3C)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (isSelected) color else Color.Transparent).border(1.dp, if (isSelected) textColor else Color.LightGray, RoundedCornerShape(50)).clickable { onSelected(priority) }.padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text(priority, color = if (isSelected) textColor else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DatePickerButton(dateText: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (dateText.isEmpty()) "Pilih Tanggal" else dateText, color = TextBlack)
            Icon(Icons.Outlined.CalendarToday, null, tint = PrimaryBrown)
        }
    }
}

// TAMBAHAN: Tombol Time Picker
@Composable
fun TimePickerButton(timeText: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (timeText.isEmpty()) "Pilih Jam" else timeText, color = TextBlack)
            Icon(Icons.Outlined.Schedule, null, tint = PrimaryBrown)
        }
    }
}

@Composable
fun CampusInputField(value: String, onValueChange: (String) -> Unit, label: String, readOnly: Boolean = false, trailingIcon: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), readOnly = readOnly, trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBrown, unfocusedBorderColor = Color.LightGray)
        )
        if (readOnly && onClick != null) Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}