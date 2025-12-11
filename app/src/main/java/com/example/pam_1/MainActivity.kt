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

        // SupabaseClient object sudah membuat client (sesuai file SupabaseClient.kt kamu).
        // Tidak perlu memodifikasi SupabaseClient di sini untuk v3.2.6.

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
    val permissionsToRequest = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.CAMERA
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    fun allPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val hasAskedPermissions = prefs.getBoolean(KEY_PERMISSIONS_ASKED, false)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, true).apply()
    }

    LaunchedEffect(Unit) {
        if (!hasAskedPermissions) {
            if (!allPermissionsGranted(permissionsToRequest)) {
                launcher.launch(permissionsToRequest)
            } else {
                prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, true).apply()
            }
        }
    }
}
