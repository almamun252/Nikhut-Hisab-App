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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
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

    // --- Dynamic Filter Strings ---
    val filterAllTime = stringResource(R.string.filter_all_time)
    val filterThisMonth = stringResource(R.string.filter_this_month)
    val filterLastMonth = stringResource(R.string.filter_last_month)
    val filterLast6Months = stringResource(R.string.filter_last_6_months)
    val filterCustomRange = stringResource(R.string.filter_custom_range)

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(filterThisMonth) }

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
            filterAllTime -> true
            filterThisMonth -> txMonth == currMonth && txYear == currYear
            filterLastMonth -> {
                val lastMonth = if (currMonth == 0) 11 else currMonth - 1
                val lastMonthYear = if (currMonth == 0) currYear - 1 else currYear
                txMonth == lastMonth && txYear == lastMonthYear
            }
            filterLast6Months -> {
                val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
                tx.date >= sixMonthsAgo
            }
            filterCustomRange -> {
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
                            // Silently ignore or show error
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
                    Text(if (selectedTabIndex == 0) stringResource(R.string.btn_new_income) else stringResource(R.string.btn_new_expense), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Clean Header ---
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
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = stringResource(R.string.desc_back), tint = Color(0xFF334155))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.title_income_expense),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // --- Filters & List Section ---
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1: Modern Search Bar & Date Filter Side-by-Side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.hint_search), color = Color.Gray, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = themeColor) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        // Date Filter Dropdown
                        SharedDateRangeFilter(
                            selectedOption = selectedDateFilter,
                            themeColor = themeColor,
                            modifier = Modifier.width(135.dp),
                            onOptionSelected = { option ->
                                if (option == filterCustomRange) {
                                    showCustomDateDialog = true
                                } else {
                                    selectedDateFilter = option
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 2: Total Summary Card with built-in Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = themeColor.copy(alpha = 0.7f),
                                ambientColor = themeColor.copy(alpha = 0.4f)
                            )
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = lightThemeColor),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = if (selectedTabIndex == 0) stringResource(R.string.label_total_income) else stringResource(R.string.label_total_expense),
                                        color = darkThemeColor.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳ ${currentTotal.toInt()}",
                                        color = darkThemeColor,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Compact Toggle
                                IncomeExpenseCompactToggle(
                                    selectedIndex = selectedTabIndex,
                                    onTabSelected = { selectedTabIndex = it },
                                    themeColor = themeColor
                                )
                            }
                        }
                    }

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
                                        text = if (selectedTabIndex == 0) stringResource(R.string.msg_no_income) else stringResource(R.string.msg_no_expense),
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(currentList, key = { it.id }) { tx ->
                                    IncomeExpenseItemCard(
                                        transaction = tx,
                                        themeColor = themeColor,
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
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.hint_select_start_date)
            val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.hint_select_end_date)

            AlertDialog(
                onDismissRequest = { showCustomDateDialog = false },
                containerColor = Color.White,
                title = { Text(stringResource(R.string.title_select_date), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
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
                        selectedDateFilter = context.getString(R.string.filter_custom_range)
                    }) {
                        Text(stringResource(R.string.btn_confirm), fontWeight = FontWeight.Bold, color = themeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCustomDateDialog = false
                        customStartDate = null
                        customEndDate = null
                    }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.Gray)
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
                    }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold, color = themeColor) }
                },
                dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) } }
            ) { DatePicker(state = datePickerState, title = { Text(stringResource(R.string.title_start_date), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
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
                    }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold, color = themeColor) }
                },
                dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) } }
            ) { DatePicker(state = datePickerState, title = { Text(stringResource(R.string.title_end_date), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
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

                    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale.getDefault())

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IncomeExpenseDetailRow(icon = Icons.Rounded.Category, label = stringResource(R.string.label_category), value = tx.category, color = themeColor)
                        IncomeExpenseDetailRow(icon = Icons.Rounded.CalendarToday, label = stringResource(R.string.label_date_time), value = sdf.format(Date(tx.date)), color = Color.Gray)

                        val noteStr = try { tx.javaClass.getMethod("getNote").invoke(tx) as? String } catch (e: Exception) { null }
                        if (!noteStr.isNullOrBlank()) {
                            IncomeExpenseDetailRow(icon = Icons.Rounded.Subject, label = stringResource(R.string.label_note), value = noteStr, color = Color.Gray)
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
                            Text(stringResource(R.string.btn_edit), fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
                            Text(stringResource(R.string.btn_delete), fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }
                    }
                }
            }

            // --- Delete Confirmation Dialog ---
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = Color.White,
                    title = { Text(stringResource(R.string.title_delete_transaction), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = { Text(stringResource(R.string.msg_confirm_delete_transaction)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteTransaction(tx)
                                showDeleteDialog = false
                                selectedTransaction = null
                                Toast.makeText(context, context.getString(R.string.msg_deleted_successfully), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.btn_yes_delete), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

// --- Compact Toggle for Inside Card ---
@Composable
fun IncomeExpenseCompactToggle(selectedIndex: Int, onTabSelected: (Int) -> Unit, themeColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.5f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val titles = listOf(stringResource(R.string.tab_income), stringResource(R.string.tab_expense))
        titles.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) themeColor else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- Filter Component Updates ---
@Composable
private fun SharedDateRangeFilter(selectedOption: String, themeColor: Color, modifier: Modifier = Modifier, onOptionSelected: (String) -> Unit) {
    val options = listOf(
        stringResource(R.string.filter_all_time),
        stringResource(R.string.filter_this_month),
        stringResource(R.string.filter_last_month),
        stringResource(R.string.filter_last_6_months),
        stringResource(R.string.filter_custom_range)
    )

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = themeColor,
                containerColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedOption,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
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

// --- Modern List Item Card (Home Screen Style) ---
@Composable
fun IncomeExpenseItemCard(
    transaction: Transaction,
    themeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dateString = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val amountPrefix = if (transaction.type == "Income") "+" else "-"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Using fallback icon since getCategoryIcon is top-level in another file, or handled by context
                Icon(
                    imageVector = Icons.Rounded.Category, // Fallback icon
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val titleText = transaction.title.trim()
                    val categoryText = transaction.category.trim()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (titleText.isNotBlank() && titleText != "-") titleText else categoryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (titleText.isNotBlank() && titleText != "-" && titleText != categoryText) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCBD5E1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = categoryText,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = dateString,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$amountPrefix ৳${transaction.amount.toInt()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor
                )
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