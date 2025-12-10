package com.example.pam_1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.pam_1.navigations.AppNavigation
import com.example.pam_1.ui.theme.Pam_1Theme

// Nama key untuk SharedPreferences
private const val PREFS_NAME = "PamAppPrefs"
private const val KEY_PERMISSIONS_ASKED = "permissions_asked"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pam_1Theme {
                // Panggil fungsi request permission
                RequestPermissionsOnStart(context = this)

                AppNavigation()
            }
        }
    }
}

@Composable
fun RequestPermissionsOnStart(context: Context) {
    // 1. Tentukan permission apa saja yang dibutuhkan berdasarkan Versi Android
    // Logika ini disederhanakan, pastikan sesuai dengan kebutuhan Anda.
    val permissionsToRequest = when {
        // Android 13 (TIRAMISU) ke atas
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.CAMERA
        )
        // Android 10 (Q) sampai Android 12 (S)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        // Android 9 (P) ke bawah
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    // Fungsi helper untuk memeriksa apakah izin sudah diberikan
    fun allPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Cek apakah izin sudah pernah diminta sebelumnya (dari SharedPreferences)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val hasAskedPermissions = prefs.getBoolean(KEY_PERMISSIONS_ASKED, false)

    // 2. Siapkan Launcher untuk meminta izin
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Setelah permintaan, kita menandai bahwa proses permintaan sudah selesai
        prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, true).apply()
        // Handle hasil permintaan izin di sini (misalnya menampilkan Toast jika ditolak)
    }

    // 3. Eksekusi permintaan izin HANYA jika:
    // a. Belum pernah diminta (hasAskedPermissions == false)
    // b. DAN belum semua izin diberikan.
    LaunchedEffect(Unit) {
        if (!hasAskedPermissions) {
            if (!allPermissionsGranted(permissionsToRequest)) {
                // Jika belum pernah diminta dan belum semua diberikan, baru kita panggil launcher
                launcher.launch(permissionsToRequest)
            } else {
                // Jika belum pernah diminta, tapi entah bagaimana izin sudah ada (kasus edge),
                // tetap tandai bahwa kita sudah "mengurus" permission.
                prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, true).apply()
            }
        }
        // Jika hasAskedPermissions == true, maka kita tidak melakukan apa-apa.
    }
}