package com.example.pam_1.navigations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Tugas : NavigationItem("tugas", "Tugas", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment)
    object Keuangan : NavigationItem("keuangan", "Keuangan", Icons.Default.MonetizationOn, Icons.Outlined.MonetizationOn)
    object Grup : NavigationItem("grup", "Grup", Icons.Default.Group, Icons.Outlined.Group)
    object Catatan : NavigationItem("catatan", "Catatan",
        Icons.AutoMirrored.Filled.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2
    )
    object Event : NavigationItem("event", "Event", Icons.Default.DateRange, Icons.Outlined.DateRange)
}