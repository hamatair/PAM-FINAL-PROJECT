package com.example.pam_1.navigations // Sesuaikan package

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

// Extension untuk mencegah crash saat double tap navigasi
fun NavController.navigateSafe(route: String, builder: (NavOptionsBuilder.() -> Unit)? = null) {
    // Hanya navigate jika state saat ini RESUMED (artinya layar aktif dan siap)
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        if (builder != null) {
            this.navigate(route, builder)
        } else {
            this.navigate(route)
        }
    }
}

// Extension untuk popBackStack yang aman
fun NavController.popBackStackSafe() {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.popBackStack()
    }
}