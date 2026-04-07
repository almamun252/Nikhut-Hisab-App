package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
fun IncomeExpenseScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Tab State: 0 = Income (আয়), 1 = Expense (ব্যয়)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("চলতি মাস") }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // --- Filtering Logic ---
    val baseFilteredList = allTransactions.filter { tx ->
        val searchMatch = searchQuery.isEmpty() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)

        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val txMonth = cal.get(Calendar.MONTH)
        val txYear = cal.get(Calendar.YEAR)

        val currentCal = Calendar.getInstance()
        val currMonth = currentCal.get(Calendar.MONTH)
        val currYear = currentCal.get(Calendar.YEAR)

        val dateMatch = when (selectedDateFilter) {
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
        searchMatch && dateMatch
    }

    val incomes = baseFilteredList.filter { it.type == "Income" }.sortedByDescending { it.date }
    val expenses = baseFilteredList.filter { it.type == "Expense" }.sortedByDescending { it.date }

    val totalIncome = incomes.sumOf { it.amount }.toFloat()
    val totalExpense = expenses.sumOf { it.amount }.toFloat()

    val currentList = if (selectedTabIndex == 0) incomes else expenses
    val currentTotal = if (selectedTabIndex == 0) totalIncome else totalExpense

    // Modern Premium Colors
    val themeColor = if (selectedTabIndex == 0) Color(0xFF10B981) else Color(0xFFF43F5E) // Emerald vs Rose
    val lightThemeColor = if (selectedTabIndex == 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val darkThemeColor = if (selectedTabIndex == 0) Color(0xFF047857) else Color(0xFFBE123C)
    val bgColor = Color(0xFFF8FAFC)

    // Bottom Sheet States
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = bgColor,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(tween(600, delayMillis = 300)) { 150 } + fadeIn(tween(600)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        try {
                            val route = if (selectedTabIndex == 0) "add_income" else "add_expense"
                            navController.navigate(route)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Routing Error", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(bottom = 90.dp),
                    containerColor = themeColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedTabIndex == 0) "নতুন আয়" else "নতুন ব্যয়", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Clean Header with Tabs ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF334155))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "আয় ও ব্যয় খাতা",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Minimalist Tab Switcher
                CustomIncomeExpenseTab(
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    incomeAmount = totalIncome,
                    expenseAmount = totalExpense
                )
            }

            // --- Filters & List Section ---
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Filter Dropdown
                    SharedDateRangeFilter(
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Summary Card with Glowing Shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp, // Stronger elevation for glow
                                shape = RoundedCornerShape(20.dp),
                                spotColor = themeColor.copy(alpha = 0.7f), // Theme color glow
                                ambientColor = themeColor.copy(alpha = 0.4f)
                            )
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = lightThemeColor),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (selectedTabIndex == 0) "মোট আয়" else "মোট ব্যয়",
                                    color = darkThemeColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${currentTotal.toInt()}",
                                    color = darkThemeColor,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Modern Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (selectedTabIndex == 0) "আয় খুঁজুন..." else "ব্যয় খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = themeColor) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), // Compact height
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // List View
                    AnimatedContent(
                        targetState = currentList.isEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "list_anim"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (selectedTabIndex == 0) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = themeColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = if (selectedTabIndex == 0) "কোনো আয়ের হিসাব নেই!" else "কোনো ব্যয়ের হিসাব নেই!",
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp) // কার্ডগুলো আরও কাছাকাছি
                            ) {
                                items(currentList, key = { it.id }) { tx ->
                                    ModernIncomeExpenseCard(
                                        transaction = tx,
                                        themeColor = themeColor,
                                        lightThemeColor = lightThemeColor,
                                        modifier = Modifier.animateItemPlacement(tween(300)),
                                        onClick = { selectedTransaction = tx }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Custom Date Range Dialog ---
        if (showCustomDateDialog) {
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
            val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
            val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

            AlertDialog(
                onDismissRequest = { showCustomDateDialog = false },
                containerColor = Color.White,
                title = { Text("তারিখ নির্বাচন করুন", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (customStartDate != null) themeColor else Color.Gray)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(startStr, fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (customEndDate != null) themeColor else Color.Gray)
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
                dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল", color = Color.Gray) } }
            ) { DatePicker(state = datePickerState, title = { Text(" শুরুর তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
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
                dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল", color = Color.Gray) } }
            ) { DatePicker(state = datePickerState, title = { Text(" শেষের তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
        }

        // --- Modern Bottom Sheet Details with Delete Confirmation ---
        if (selectedTransaction != null) {
            val tx = selectedTransaction!!
            var showDeleteDialog by remember { mutableStateOf(false) }

            ModalBottomSheet(
                onDismissRequest = { selectedTransaction = null },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(themeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tx.category.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = themeColor)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(tx.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${if (tx.type == "Income") "+" else "-"} ৳${tx.amount.toInt()}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(24.dp))

                    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IncomeExpenseDetailRow(icon = Icons.Rounded.Category, label = "ক্যাটাগরি", value = tx.category, color = themeColor)
                        IncomeExpenseDetailRow(icon = Icons.Rounded.CalendarToday, label = "তারিখ ও সময়", value = sdf.format(Date(tx.date)), color = Color.Gray)

                        val noteStr = try { tx.javaClass.getMethod("getNote").invoke(tx) as? String } catch (e: Exception) { null }
                        if (!noteStr.isNullOrBlank()) {
                            IncomeExpenseDetailRow(icon = Icons.Rounded.Subject, label = "নোট", value = noteStr, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = {
                                val id = tx.id
                                selectedTransaction = null
                                val route = if (tx.type == "Income") "add_income?transactionId=$id" else "add_expense?transactionId=$id"
                                navController.navigate(route)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এডিট", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true }, // Show Confirmation Dialog
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ডিলিট", fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }
                    }
                }
            }

            // --- Delete Confirmation Dialog ---
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = Color.White,
                    title = { Text("হিসাব মুছে ফেলবেন?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = { Text("আপনি কি নিশ্চিত যে এই হিসাবটি মুছে ফেলতে চান? মুছে ফেললে এটি আর ফিরে পাওয়া যাবে না।") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteTransaction(tx)
                                showDeleteDialog = false
                                selectedTransaction = null
                                Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedDateRangeFilter(selectedOption: String, themeColor: Color, onOptionSelected: (String) -> Unit) {
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
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp), // Compact height
            modifier = Modifier.menuAnchor().fillMaxWidth().height(52.dp),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = selectionOption,
                            fontWeight = if (selectedOption == selectionOption) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedOption == selectionOption) themeColor else Color(0xFF1E293B)
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

// --- Minimalist Tab Switcher ---
@Composable
fun CustomIncomeExpenseTab(selectedIndex: Int, onTabSelected: (Int) -> Unit, incomeAmount: Float, expenseAmount: Float) {
    val tabTitles = listOf("আয় (Income)", "ব্যয় (Expense)")
    val tabColors = listOf(Color(0xFF10B981), Color(0xFFF43F5E))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(60.dp) // Compact Height
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(16.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            val amount = if (index == 0) incomeAmount else expenseAmount

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color.LightGray) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) tabColors[index] else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "৳${amount.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isSelected) tabColors[index] else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// --- Modern Compact Card Design with Glowing Shadow ---
@Composable
fun ModernIncomeExpenseCard(transaction: Transaction, themeColor: Color, lightThemeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val prefix = if (transaction.type == "Income") "+" else "-"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, // Glowing effect
                shape = RoundedCornerShape(16.dp), // Compact shape
                spotColor = themeColor.copy(alpha = 0.6f), // Dynamic glow color
                ambientColor = themeColor.copy(alpha = 0.3f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp), // Compact padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(lightThemeColor), // Compact Icon
                    contentAlignment = Alignment.Center
                ) {
                    Text(transaction.category.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${transaction.category} • ${sdf.format(Date(transaction.date))}", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
                Text("$prefix ৳${transaction.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = themeColor)
            }
        }
    }
}

@Composable
fun IncomeExpenseDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}