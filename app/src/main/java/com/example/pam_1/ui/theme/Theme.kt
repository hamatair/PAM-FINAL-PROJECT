package com.example.pam_1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Mapping warna Light Mode sesuai gambar
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBrown,
    onPrimary = White, // Teks putih di atas background coklat
    secondary = LightBrown,
    onSecondary = DarkerBrown,
    tertiary = DarkerBrown,

    background = BackgroundBeige,
    onBackground = TextBlack,

    surface = White, // Kartu biasanya putih
    onSurface = TextBlack,

    error = DangerRed,
    onError = White
)

// Mapping Dark Mode (Opsional: disesuaikan agar tetap terbaca)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBrown,
    onPrimary = White, // Teks putih di atas background coklat
    secondary = LightBrown,
    onSecondary = DarkerBrown,
    tertiary = DarkerBrown,

    background = BackgroundBeige,
    onBackground = TextBlack,

    surface = White, // Kartu biasanya putih
    onSurface = TextBlack,

    error = DangerRed,
    onError = White
)
@Composable
fun Pam_1Theme(
    // Paksa SELALU light mode
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
