package com.example.pam_1.ui.screens.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil.compose.AsyncImage
import com.example.pam_1.data.model.DEFAULT_AVATAR
import com.example.pam_1.data.model.Event
import com.example.pam_1.ui.common.CategoryChip
import com.example.pam_1.ui.common.formatCreatedAt
import com.example.pam_1.ui.common.formatTimeWIB
import com.example.pam_1.ui.theme.*
import com.example.pam_1.viewmodel.EventViewModel
import com.example.pam_1.viewmodel.UiState

@Composable
fun DetailEventScreen(
    eventId: String,
    viewModel: EventViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(eventId) {
        viewModel.loadEventDetail(eventId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearEventDetail() }
    }

    val detailState by viewModel.eventDetailState.collectAsState()

    Scaffold(
        containerColor = BackgroundBeige,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = detailState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBrown)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = DangerRed)
                    }
                }
                is UiState.Success -> {
                    val event = state.data
                    if (event != null) {
                        EventDetailContent(event = event, onBack = onNavigateBack)
                    } else {
                        Text("Data kosong", modifier = Modifier.align(Alignment.Center))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun EventDetailContent(
    event: Event,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // --- 1. HERO IMAGE HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AsyncImage(
                model = event.eventImageUrl ?: "https://via.placeholder.com/600x400",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                            startY = 0f,
                            endY = 300f
                        )
                    )
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 40.dp, start = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- 2. KONTEN DETAIL ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = BackgroundBeige
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // -- Profile Pembuat --
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    AsyncImage(
                        model = event.creator?.photo_profile ?: DEFAULT_AVATAR,
                        contentDescription = "Pembuat",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = event.creator?.username ?: "Tidak Diketahui",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Diposting ${formatCreatedAt(event.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // -- Judul --
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(16.dp))
                // -- Kategori --
                if (event.categoryPivots?.isNotEmpty() == true) {

                    // GANTI DARI Row MENJADI FlowRow
                    @OptIn(ExperimentalLayoutApi::class) // Perlu anotasi untuk FlowRow
                    FlowRow(
                        // Sama seperti horizontalArrangement pada Row
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        // Kita juga bisa menambahkan jarak vertikal antar baris
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        event.categoryPivots.forEach { pivot ->
                            pivot.categoryDetail?.let { cat ->
                                CategoryChip(
                                    text = cat.categoryName,
                                    isSelected = true,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // -- Informasi --
                Text("Informasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                DetailInfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Tanggal",
                    value = event.eventDate
                )
                DetailInfoRow(
                    icon = Icons.Default.Schedule,
                    label = "Waktu",
                    value = "${formatTimeWIB(event.startTime)} - ${formatTimeWIB(event.endTime)} WIB"
                )
                DetailInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Lokasi",
                    value = event.eventLocation
                )

                Spacer(modifier = Modifier.height(24.dp))

                // -- Deskripsi --
                Text("Deskripsi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // --- PERBAIKAN CRASH DI SINI ---
                // Kita gunakan fontSize * 1.5, bukan lineHeight * 1.5
                // Karena lineHeight bawaan tema seringkali 'Unspecified' -> Crash jika dikali
                val bodyStyle = MaterialTheme.typography.bodyMedium
                Text(
                    text = event.eventDescription,
                    style = bodyStyle,
                    color = TextBlack,
                    // FIX: Gunakan fontSize sebagai basis perkalian, karena fontSize pasti ada nilainya
                    lineHeight = bodyStyle.fontSize * 1.5
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBrown,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextGray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextBlack)
        }
    }
}