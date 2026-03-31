package com.almamun252.nikhuthisab.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val allTransactions by viewModel.allTransactions.collectAsState()

    // Filters State
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") } // All, Income, Expense
    var selectedMonth by remember { mutableStateOf("All Time") } // All Time, This Month, Last Month, Custom

    // Custom Date Range State - Separate start and end dates
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Pagination State
    val itemsPerPage = 40
    var currentPage by remember { mutableStateOf(0) }

    // Bottom Sheet State
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter Logic
    val filteredTransactions = allTransactions.filter { tx ->
        val matchesType = selectedType == "All" || tx.type == selectedType
        val matchesSearch = searchQuery.isEmpty() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)

        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val txMonth = cal.get(Calendar.MONTH)
        val txYear = cal.get(Calendar.YEAR)

        val currentCal = Calendar.getInstance()
        val currMonth = currentCal.get(Calendar.MONTH)
        val currYear = currentCal.get(Calendar.YEAR)

        val matchesMonth = when (selectedMonth) {
            "This Month" -> txMonth == currMonth && txYear == currYear
            "Last Month" -> {
                val lastMonth = if (currMonth == 0) 11 else currMonth - 1
                val lastMonthYear = if (currMonth == 0) currYear - 1 else currYear
                txMonth == lastMonth && txYear == lastMonthYear
            }
            "Custom" -> {
                // কাস্টম ডেট রেঞ্জ এর লজিক
                val start = customStartDate ?: 0L
                val end = customEndDate?.let { it + 86400000L - 1L } ?: Long.MAX_VALUE
                tx.date in start..end
            }
            else -> true // All Time
        }

        matchesType && matchesSearch && matchesMonth
    }.sortedByDescending { it.date }

    // Pagination Reset on Filter Change
    LaunchedEffect(selectedType, selectedMonth, searchQuery) {
        currentPage = 0
    }

    // Pagination Calculations
    val totalPages = maxOf(1, ceil(filteredTransactions.size.toDouble() / itemsPerPage).toInt())
    val startIndex = currentPage * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, filteredTransactions.size)
    val currentItems = if (filteredTransactions.isEmpty()) emptyList() else filteredTransactions.subList(startIndex, endIndex)

    // Colors
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("সব লেনদেন", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("লেনদেন খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips (Scrollable)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Filters
                item { FilterChipWidget("সব", selectedType == "All") { selectedType = "All" } }
                item { FilterChipWidget("আয়", selectedType == "Income", incomeColor) { selectedType = "Income" } }
                item { FilterChipWidget("ব্যয়", selectedType == "Expense", expenseColor) { selectedType = "Expense" } }

                // Divider
                item { VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)) }

                // Month Filters
                item { FilterChipWidget("সব সময়", selectedMonth == "All Time") { selectedMonth = "All Time" } }
                item { FilterChipWidget("এই মাস", selectedMonth == "This Month") { selectedMonth = "This Month" } }
                item { FilterChipWidget("গত মাস", selectedMonth == "Last Month") { selectedMonth = "Last Month" } }
                item {
                    FilterChipWidget("কাস্টম 📅", selectedMonth == "Custom") {
                        showCustomDateDialog = true
                    }
                }
            }

            // --- Custom Date Range Main Dialog ---
            if (showCustomDateDialog) {
                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
                val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

                AlertDialog(
                    onDismissRequest = {
                        showCustomDateDialog = false
                        if (selectedMonth == "Custom" && customStartDate == null && customEndDate == null) {
                            selectedMonth = "All Time" // কোনো ডেট না দিলে All Time এ ফিরে যাবে
                        }
                    },
                    title = { Text("তারিখ নির্বাচন করুন", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Start Date Button
                            OutlinedButton(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (customStartDate != null) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(startStr, fontSize = 16.sp)
                            }

                            // End Date Button
                            OutlinedButton(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (customEndDate != null) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(endStr, fontSize = 16.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showCustomDateDialog = false
                            selectedMonth = "Custom"
                        }) {
                            Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCustomDateDialog = false
                            customStartDate = null
                            customEndDate = null
                            selectedMonth = "All Time"
                        }) {
                            Text("রিসেট", color = Color.Red)
                        }
                    }
                )
            }

            // --- Start Date Picker ---
            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            customStartDate = datePickerState.selectedDateMillis
                            showStartDatePicker = false
                        }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল") }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = { Text(" শুরুর তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // --- End Date Picker ---
            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            customEndDate = datePickerState.selectedDateMillis
                            showEndDatePicker = false
                        }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল") }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = { Text(" শেষের তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Transactions
            if (currentItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("কোনো লেনদেন পাওয়া যায়নি!", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentItems) { tx ->
                        AllTransactionItemCard(
                            transaction = tx,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor,
                            onClick = { selectedTransaction = tx }
                        )
                    }
                }
            }

            // Pagination Controls
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী")
                    }

                    Text(
                        text = "পেজ ${currentPage + 1} / $totalPages",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Text("পরবর্তী")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }

        // Bottom Sheet for Details
        if (selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedTransaction = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                AllTransactionDetailsSheet(
                    transaction = selectedTransaction!!,
                    incomeColor = incomeColor,
                    expenseColor = expenseColor,
                    onEdit = {
                        val id = selectedTransaction!!.id
                        val typeRoute = if (selectedTransaction!!.type == "Income") "add_income" else "add_expense"
                        selectedTransaction = null
                        navController.navigate("$typeRoute?transactionId=$id")
                    },
                    onDelete = {
                        viewModel.deleteTransaction(selectedTransaction!!)
                        selectedTransaction = null
                    }
                )
            }
        }
    }
}

// Helper Composable for Filter Chips
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipWidget(
    label: String,
    selected: Boolean,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.2f),
            selectedLabelColor = selectedColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) selectedColor else Color.LightGray
        )
    )
}

// Transaction List Item
@Composable
private fun AllTransactionItemCard(
    transaction: Transaction,
    incomeColor: Color,
    expenseColor: Color,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == "Income"
    val themeColor = if (isIncome) incomeColor else expenseColor
    val lightThemeColor = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val amountPrefix = if (isIncome) "+" else "-"

    val dateString = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(lightThemeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.category.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${transaction.category} • $dateString",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Amount
            Text(
                text = "$amountPrefix ৳${transaction.amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
        }
    }
}

// Bottom Sheet Content
@Composable
private fun AllTransactionDetailsSheet(
    transaction: Transaction,
    incomeColor: Color,
    expenseColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "Income"
    val themeColor = if (isIncome) incomeColor else expenseColor
    val lightThemeColor = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val amountPrefix = if (isIncome) "+" else "-"

    val dateString = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD")).format(Date(transaction.date))
    val timeString = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(transaction.date))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(lightThemeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.category.take(1).uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = themeColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Amount
        Text(
            text = transaction.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$amountPrefix ৳${transaction.amount.toInt()}",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themeColor
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))

        // Details list
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AllTxDetailRow(icon = Icons.Rounded.Category, label = "ক্যাটাগরি", value = transaction.category, iconTint = themeColor)
            AllTxDetailRow(icon = Icons.Rounded.CalendarToday, label = "তারিখ", value = dateString, iconTint = themeColor)
            AllTxDetailRow(icon = Icons.Rounded.Schedule, label = "সময়", value = timeString, iconTint = themeColor)

            // Note
            if (!transaction.note.isNullOrBlank()) {
                AllTxDetailRow(icon = Icons.Rounded.Info, label = "নোট", value = transaction.note, iconTint = themeColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions: Edit & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                Spacer(modifier = Modifier.width(8.dp))
                Text("এডিট করুন", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("মুছে ফেলুন", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AllTxDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}