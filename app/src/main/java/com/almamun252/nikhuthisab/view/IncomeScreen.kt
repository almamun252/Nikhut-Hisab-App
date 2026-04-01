package com.almamun252.nikhuthisab.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IncomeScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    // Screen Entry Animation State
    var isVisible by remember { mutableStateOf(false) }

    // Trigger animation when screen opens
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Date Filter State
    var selectedDateFilter by remember { mutableStateOf("চলতি মাস") }

    // Custom Date Range State
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // ডেটাবেস থেকে রিয়েল-টাইম ডেটা আনা
    val allTransactions by viewModel.allTransactions.collectAsState()

    // শুধু আয়ের ডেটা ফিল্টার করা (Search + Date Range)
    val incomes = allTransactions.filter { tx ->
        val matchesTypeAndSearch = tx.type == "Income" &&
                (searchQuery.isEmpty() || tx.title.contains(searchQuery, ignoreCase = true) || tx.category.contains(searchQuery, ignoreCase = true))

        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val txMonth = cal.get(Calendar.MONTH)
        val txYear = cal.get(Calendar.YEAR)

        val currentCal = Calendar.getInstance()
        val currMonth = currentCal.get(Calendar.MONTH)
        val currYear = currentCal.get(Calendar.YEAR)

        val matchesDate = when (selectedDateFilter) {
            "সব সময়" -> true
            "চলতি মাস" -> txMonth == currMonth && txYear == currYear
            "গত মাস" -> {
                val lastMonth = if (currMonth == 0) 11 else currMonth - 1
                val lastMonthYear = if (currMonth == 0) currYear - 1 else currYear
                txMonth == lastMonth && txYear == lastMonthYear
            }
            "গত ৬ মাস" -> {
                val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
                tx.date >= sixMonthsAgo
            }
            "কাস্টম রেঞ্জ" -> {
                val start = customStartDate ?: 0L
                val end = customEndDate?.let { it + 86400000L - 1L } ?: Long.MAX_VALUE
                tx.date in start..end
            }
            else -> true
        }

        matchesTypeAndSearch && matchesDate
    }.sortedByDescending { it.date }

    // মোট আয় হিসাব করা
    val totalIncome = incomes.sumOf { it.amount.toDouble() }

    // থিম কালার (সবুজ - আয়ের জন্য)
    val themeColor = Color(0xFF4CAF50)
    val lightThemeColor = Color(0xFFE8F5E9)
    val darkThemeColor = Color(0xFF2E7D32)

    // Bottom Sheet এর জন্য স্টেট
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("আয় ট্র্যাকিং", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 300)) + slideInVertically(tween(600, delayMillis = 300)) { 100 }
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_income") },
                    containerColor = themeColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নতুন আয়", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 150 },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ডেট রেঞ্জ ড্রপডাউন ফিল্টার (আয়ের থিম কালার দিয়ে)
                IncomeDateRangeFilter(
                    selectedOption = selectedDateFilter,
                    themeColor = themeColor,
                    onOptionSelected = { option ->
                        if (option == "কাস্টম রেঞ্জ") {
                            showCustomDateDialog = true
                        } else {
                            selectedDateFilter = option
                        }
                    }
                )

                // --- Custom Date Range Main Dialog ---
                if (showCustomDateDialog) {
                    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                    val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
                    val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

                    AlertDialog(
                        onDismissRequest = { showCustomDateDialog = false },
                        title = { Text("তারিখ নির্বাচন করুন", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Start Date Button
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (customStartDate != null) themeColor else Color.Gray
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
                                        contentColor = if (customEndDate != null) themeColor else Color.Gray
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
                                selectedDateFilter = "কাস্টম রেঞ্জ"
                            }) {
                                Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold, color = themeColor)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showCustomDateDialog = false
                                customStartDate = null
                                customEndDate = null
                            }) {
                                Text("বাতিল", color = Color.Gray)
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
                            }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold, color = themeColor) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল", color = Color.Gray) }
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
                            }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold, color = themeColor) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল", color = Color.Gray) }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = { Text(" শেষের তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total Income Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = lightThemeColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "মোট আয়",
                            color = darkThemeColor.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳ ${totalIncome.toInt()}",
                            color = darkThemeColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Modern Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("আয় খুঁজুন (নাম বা ক্যাটাগরি)...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = themeColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.List, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সব আয়ের তালিকা", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Incomes
                if (incomes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("কোনো হিসাব পাওয়া যায়নি!", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // FAB এর জন্য নিচে জায়গা রাখা
                    ) {
                        items(incomes, key = { it.id }) { income ->
                            IncomeItemCard(
                                income = income,
                                themeColor = themeColor,
                                lightThemeColor = lightThemeColor,
                                modifier = Modifier.animateItemPlacement(tween(300)), // Smooth filtering animation
                                onClick = { selectedTransaction = income }
                            )
                        }
                    }
                }
            }
        }

        // Transaction Details Bottom Sheet
        if (selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedTransaction = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                IncomeTransactionDetailsSheet(
                    transaction = selectedTransaction!!,
                    themeColor = themeColor,
                    lightThemeColor = lightThemeColor,
                    onEdit = {
                        val id = selectedTransaction!!.id
                        selectedTransaction = null
                        // একদম ঠিক করা রুট: add_income?transactionId=id
                        navController.navigate("add_income?transactionId=$id")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeDateRangeFilter(selectedOption: String, themeColor: Color, onOptionSelected: (String) -> Unit) {
    val options = listOf("সব সময়", "চলতি মাস", "গত মাস", "গত ৬ মাস", "কাস্টম রেঞ্জ")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = { },
            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = themeColor) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = selectionOption,
                            fontWeight = if (selectedOption == selectionOption) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedOption == selectionOption) themeColor else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IncomeItemCard(income: Transaction, themeColor: Color, lightThemeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val dateString = sdf.format(Date(income.date))

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Icon / Initial Letter
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(lightThemeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = income.category.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = income.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${income.category} • $dateString",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Amount
            Text(
                text = "+ ৳${income.amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
        }
    }
}

// নাম ইউনিক করা হয়েছে
@Composable
fun IncomeTransactionDetailsSheet(
    transaction: Transaction,
    themeColor: Color,
    lightThemeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD")).format(Date(transaction.date))
    val timeString = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(transaction.date))

    // ডিলিট কনফার্মেশন ডায়লগের জন্য স্টেট
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
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
            text = "+ ৳${transaction.amount.toInt()}",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themeColor
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))

        // Details list
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IncomeDetailRow(icon = Icons.Rounded.Category, label = "ক্যাটাগরি", value = transaction.category, iconTint = themeColor)
            IncomeDetailRow(icon = Icons.Rounded.CalendarToday, label = "তারিখ", value = dateString, iconTint = themeColor)
            IncomeDetailRow(icon = Icons.Rounded.Schedule, label = "সময়", value = timeString, iconTint = themeColor)

            // নোট ফিল্ড
            val note = try { transaction.javaClass.getMethod("getNote").invoke(transaction) as? String } catch (e: Exception) { null }
            if (!note.isNullOrEmpty()) {
                IncomeDetailRow(icon = Icons.Rounded.Info, label = "নোট", value = note, iconTint = themeColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions: Edit and Delete Buttons
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
                // এখানে সরাসরি onDelete() কল না করে আগে ডায়লগ শো করা হচ্ছে
                onClick = { showDeleteDialog = true },
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

    // --- Delete Warning Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("হিসাব মুছে ফেলবেন?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("আপনি কি নিশ্চিত যে এই আয়ের হিসাবটি মুছে ফেলতে চান? মুছে ফেললে এটি আর ফিরে পাওয়া যাবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete() // এখানে নিশ্চিত করার পর ডিলিট হবে
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, মুছে ফেলুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("বাতিল", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// নাম ইউনিক করা হয়েছে
@Composable
fun IncomeDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconTint: Color) {
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