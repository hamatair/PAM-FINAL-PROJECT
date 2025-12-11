package com.example.pam_1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.pam_1.navigations.NavigationItem
import com.example.pam_1.ui.common.AnimatedBottomNavigationBar
import com.example.pam_1.viewmodel.AuthViewModel
import com.example.pam_1.ui.theme.PrimaryBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbar(navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = "PAM App",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryBrown
            )
        },
        actions = {
            IconButton(onClick = { navController.navigate("profile") }) {
                Icon(Icons.Filled.Person, contentDescription = "profile", tint = PrimaryBrown)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun MainAppScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    // state lokal untuk tab saat ini (string route seperti "tugas")
    var currentTab by remember { mutableStateOf(NavigationItem.Tugas.route) }

    // Jika kamu ingin default tab berubah berdasarkan route root (mis. saat navigate ke home),
    // kamu bisa membaca navBackStackEntry di sini dan set currentTab sekali.
    // Tapi sederhana: default = tugas (seperti sebelumnya).

    Scaffold(
        topBar = { AppToolbar(navController) },
        bottomBar = {
            AnimatedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { selected ->
                    currentTab = selected
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                NavigationItem.Tugas.route -> DummyScreen("Halaman Tugas")
                NavigationItem.Keuangan.route -> DummyScreen("Halaman Keuangan")
                NavigationItem.Grup.route -> DummyScreen("Halaman Grup")
                NavigationItem.Catatan.route -> DummyScreen("Halaman Catatan")
                NavigationItem.Event.route -> DummyScreen("Halaman Event")
                else -> DummyScreen("Halaman Tugas")
            }
        }
    }
}




// Komponen Sementara untuk test navigasi
@Composable
fun DummyScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Pastikan background terang (Beige/White)
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}