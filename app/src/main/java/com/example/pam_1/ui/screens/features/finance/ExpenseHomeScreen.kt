package com.example.pam_1.ui.screens.features.finance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pam_1.data.model.Expense
import com.example.pam_1.ui.theme.PrimaryBrown
import com.example.pam_1.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit = {}
) {
    val expenses by viewModel.expenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val totalExpense = expenses.sumOf { it.amount }

    // ✅ PENTING: Trigger initial fetch saat screen pertama kali muncul
    // LaunchedEffect dengan key Unit = hanya run sekali saat composition
    LaunchedEffect(Unit) {
        // Fetch data setelah auth session ready
        viewModel.fetchExpenses()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = PrimaryBrown
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            /* ================= HEADER TOTAL ================= */
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Pengeluaran",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Rp $totalExpense",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBrown
                    )
                }
            }

            /* ================= PIE CHART (FUNCTIONAL) ================= */
            item {
                // Calculate category totals
                val categoryTotals = expenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                
                if (categoryTotals.isNotEmpty()) {
                    ExpensePieChart(
                        categoryData = categoryTotals,
                        total = totalExpense
                    )
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data expense",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            /* ================= TITLE LIST ================= */
            item {
                Text(
                    text = "Your Expense",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            /* ================= LIST EXPENSE ================= */
            items(
                items = expenses,
                key = { expense -> expense.id ?: 0 } // ✅ PENTING: key untuk mencegah bug recomposition
            ) { expense ->
                ExpenseItem(
                    expense = expense,
                    onClick = { expenseId ->
                        onExpenseClick(expenseId)
                    },
                    onDelete = { expenseId ->
                        viewModel.deleteExpense(expenseId)
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    onClick: (Int) -> Unit = {},
    onDelete: (Int) -> Unit = {}
) {
    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Expense?") },
            text = {
                Column {
                    Text("Yakin ingin menghapus:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Rp ${expense.amount}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        expense.id?.let { onDelete(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Hapus", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = { 
                    expense.id?.let { onClick(it) }
                },
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCategoryColor(expense.category)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = expense.category.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = expense.expenseDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(
                text = "Rp ${expense.amount}",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ================= PIE CHART COMPONENT ================= */

@Composable
fun ExpensePieChart(
    categoryData: Map<String, Long>,
    total: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut Chart (Stroke-based)
        Box(
            modifier = Modifier
                .size(200.dp),  // Ukuran lebih kecil
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasSize = size.minDimension
                val strokeWidth = 40f  // Ketebalan stroke donut
                val radius = (canvasSize - strokeWidth) / 2f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                
                var startAngle = -90f // Start from top
                
                categoryData.forEach { (category, amount) ->
                    val sweepAngle = (amount.toFloat() / total.toFloat()) * 360f
                    val color = getCategoryColor(category)
                    
                    // Draw ARC with STROKE (not fill)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,  // ← Penting: false untuk donut
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)  // ← Stroke instead of fill
                    )
                    
                    startAngle += sweepAngle
                }
                
                // TIDAK ADA white circle - center transparan
            }
            
            // Center text (already centered by Box alignment)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Rp $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // ✅ Legend removed - kategori info ada di history list
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Akademik" -> Color(0xFF4CAF50)      // Green
        "Organisasi" -> Color(0xFF2196F3)    // Blue
        "Keuangan" -> Color(0xFFFF9800)      // Orange
        "Proyek" -> Color(0xFF9C27B0)        // Purple
        "Pribadi" -> Color(0xFFE91E63)       // Pink
        "Sosial" -> Color(0xFF00BCD4)        // Cyan
        "Kesehatan" -> Color(0xFFF44336)     // Red
        "Karier" -> Color(0xFF795548)        // Brown
        else -> Color.Gray
    }
}
