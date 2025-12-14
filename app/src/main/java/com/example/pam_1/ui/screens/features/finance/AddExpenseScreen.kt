package com.example.pam_1.ui.screens.features.finance

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Handle Success state for navigation (similar to EventViewModel pattern)
    LaunchedEffect(uiState) {
        when (uiState) {
            is com.example.pam_1.viewmodel.ExpenseUiState.Success -> {
                viewModel.resetUiState()
                onNavigateBack()
            }
            else -> {}
        }
    }

    /* ================= STATE ================= */

    val categories = listOf("Akademik", "Organisasi", "Keuangan", "Proyek", "Pribadi", "Sosial", "Kesehatan", "Karier")
    var selectedCategory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    /* ================= UI ================= */

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Pengeluaran") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBrown,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /* ================= TITLE INPUT ================= */
            item {
                Column {
                    Text("Nama Pengeluaran", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Beli buku") }
                    )
                }
            }

            /* ================= AMOUNT INPUT ================= */
            item {
                Column {
                    Text("Jumlah (Rp)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: 50000") }
                    )
                }
            }

            /* ================= CATEGORY DROPDOWN ================= */
            item {
                Column {
                    Text("Kategori", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            placeholder = { Text("Pilih kategori") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            /* ================= IMAGE PICKER ================= */
            item {
                Column {
                    Text("Gambar (Opsional)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Display selected image or placeholder
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.LightGray, RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Pick Image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("Tap untuk pilih gambar", color = Color.Gray)
                            }
                        }
                    }
                }
            }

            /* ================= SAVE BUTTON ================= */
            item {
                Button(
                    onClick = {
                        // Validate inputs
                        if (title.isNotBlank() && amount.isNotBlank() && selectedCategory.isNotBlank()) {
                            val amountLong = amount.toLongOrNull() ?: 0L
                            viewModel.addExpense(
                                context = context,
                                title = title,
                                amount = amountLong,
                                category = selectedCategory,
                                imageUri = selectedImageUri
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBrown
                    ),
                    enabled = uiState !is com.example.pam_1.viewmodel.ExpenseUiState.Loading
                ) {
                    if (uiState is com.example.pam_1.viewmodel.ExpenseUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Simpan", color = Color.White)
                    }
                }
            }

            /* ================= ERROR MESSAGE ================= */
            if (uiState is com.example.pam_1.viewmodel.ExpenseUiState.Error) {
                item {
                    Text(
                        text = (uiState as com.example.pam_1.viewmodel.ExpenseUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
