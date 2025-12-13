package com.example.pam_1.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.testing.toDp
import com.example.pam_1.navigations.NavigationItem
import com.example.pam_1.ui.theme.DarkerBrown
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.ui.theme.TextGray
import com.example.pam_1.ui.theme.White

/**
 * Bottom bar sekarang TIDAK memanggil navController langsung.
 * currentTab: route string (mis. "tugas")
 * onTabSelected: callback yang memberi tahu MainAppScreen untuk ganti tab
 */
@Composable
fun AnimatedBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        NavigationItem.Tugas,
        NavigationItem.Keuangan,
        NavigationItem.Grup,
        NavigationItem.Catatan,
        NavigationItem.Event
    )

    val selectedIndex = items.indexOfFirst { it.route == currentTab }

    // Animasi posisi indikator
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 80f
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(DarkerBrown)
    ) {
        // ===== INDICATOR (rectangle, sliding) =====
        Row(modifier = Modifier.matchParentSize()) {
            items.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.matchParentSize()) {

            val density = this@BoxWithConstraints
            val totalWidthPx = constraints.maxWidth.toFloat()
            val itemWidthPx = totalWidthPx / items.size

            // konversi px → dp
            val itemWidthDp = with(density) { itemWidthPx.toDp() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(itemWidthDp)   // sudah dalam dp → PAS 1 tab
                    .graphicsLayer {
                        translationX = indicatorOffset * itemWidthPx
                    }
                    .background(PrimaryBrown)
            )
        }

        // ===== ICONS =====
        Row(modifier = Modifier.matchParentSize()) {
            items.forEach { item ->
                val isSelected = currentTab == item.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = if (isSelected) White else TextGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            color = if (isSelected) White else TextGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
