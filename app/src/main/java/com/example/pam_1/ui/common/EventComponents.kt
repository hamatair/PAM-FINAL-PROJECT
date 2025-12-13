package com.example.pam_1.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pam_1.data.model.DEFAULT_AVATAR
import com.example.pam_1.data.model.Event
import com.example.pam_1.ui.theme.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Helper: Format Waktu Event (Jam)
fun formatTimeWIB(timeString: String): String {
    if (timeString.isBlank()) return ""
    return try {
        val time = LocalTime.parse(timeString)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        time.format(formatter)
    } catch (e: Exception) {
        timeString.substringBeforeLast(':')
    }
}

// Helper: Format Tanggal Pembuatan (Created At)
// Mengubah timestamp ISO ke format "dd MMM yyyy, HH:mm"
fun formatCreatedAt(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(timestamp)
        val zoneId = ZoneId.systemDefault() // Atau ZoneId.of("Asia/Jakarta")
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(zoneId)
        formatter.format(instant)
    } catch (e: Exception) {
        timestamp // Fallback jika gagal parse
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryBrown else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, LightBrown)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) White else TextGray
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    val formattedStartTime = formatTimeWIB(event.startTime)
    val formattedEndTime = formatTimeWIB(event.endTime)

    val scheduleText = if (formattedStartTime.isNotEmpty() && formattedEndTime.isNotEmpty()) {
        "$formattedStartTime - $formattedEndTime WIB"
    } else if (formattedStartTime.isNotEmpty()) {
        "$formattedStartTime - Selesai WIB"
    } else {
        "Waktu tidak tersedia"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // --- HEADER: USER PROFILE INFO ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBrown)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Foto Profil
                AsyncImage(
                    model = event.creator?.photo_profile ?: DEFAULT_AVATAR,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Username & Waktu Dibuat
                Column {
                    Text(
                        text = event.creator?.username ?: "Unknown User",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextBlack
                    )
                    Text(
                        text = formatCreatedAt(event.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }
            // --- GAMBAR EVENT ---
            AsyncImage(
                model = event.eventImageUrl ?: "https://via.placeholder.com/400x200",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Sedikit diperbesar agar proporsional
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // JUDUL
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // DESKRIPSI
                Text(
                    text = event.eventDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FOOTER: DETAIL & BUTTON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EventInfoRow(icon = Icons.Default.DateRange, text = event.eventDate)
                        EventInfoRow(icon = Icons.Default.Schedule, text = scheduleText)
                        EventInfoRow(icon = Icons.Default.LocationOn, text = event.eventLocation)
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrown),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        modifier = Modifier.align(Alignment.Bottom)
                    ) {
                        Text(
                            text = "Lihat Detail",
                            style = MaterialTheme.typography.labelMedium,
                            color = White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = TextBlack, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextBlack)
    }
}