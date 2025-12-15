package com.example.pam_1.ui.screens.features.group_chat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pam_1.utils.InviteCodeGenerator
import com.example.pam_1.viewmodel.StudyGroupUIState
import com.example.pam_1.viewmodel.StudyGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(navController: NavController, viewModel: StudyGroupViewModel) {
    var inviteCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        when (uiState) {
            is StudyGroupUIState.Success -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                // Clear input
                inviteCode = ""
                viewModel.resetState()
            }
            is StudyGroupUIState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Gabung Grup") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Kembali")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    "Gabung Grup Belajar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Text(
                    "Masukkan kode undangan untuk bergabung ke grup belajar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        inviteCode = it.uppercase()
                        codeError = false
                    },
                    label = { Text("Kode Undangan") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = codeError,
                    singleLine = true,
                    placeholder = { Text("cth., ABC123") },
                    supportingText = {
                        if (codeError)
                                Text(
                                        text = "Silakan masukkan kode undangan yang valid",
                                        color = MaterialTheme.colorScheme.error
                                )
                    }
            )

            Spacer(Modifier.height(24.dp))

            if (uiState is StudyGroupUIState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                        onClick = {
                            if (inviteCode.isBlank() ||
                                            !InviteCodeGenerator.isValidFormat(inviteCode)
                            ) {
                                codeError = true
                                Toast.makeText(
                                                context,
                                                "Silakan masukkan kode undangan yang valid",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                                return@Button
                            }

                            viewModel.joinByInviteCode(inviteCode)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Gabung Grup") }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                        onClick = {
                            // TODO: Implement QR scanner
                            Toast.makeText(
                                            context,
                                            "Pemindai QR akan segera hadir",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            // navController.navigate("scan_qr")
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Pindai Kode QR")
                }
            }
        }
    }
}
