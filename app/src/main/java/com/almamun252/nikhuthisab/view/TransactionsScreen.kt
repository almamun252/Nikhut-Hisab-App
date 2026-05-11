package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.utils.PdfGenerator
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsState()

    // Screen Entry Animation State
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Filters State for Main List (ডিফল্ট মাস এখন 'This Month' করা হলো)
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf("This Month") }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // --- PDF Export Bottom Sheet State ---
    var showExportSheet by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var exportType by remember { mutableStateOf("All") } // All, Income, Expense, Lending, Borrowing
    var exportPeriod by remember { mutableStateOf("This Month") }
    var exportSpecificMonth by remember { mutableStateOf("") }
    var exportCategory by remember { mutableStateOf("All") }

    var exportStartDate by remember { mutableStateOf<Long?>(null) }
    var exportEndDate by remember { mutableStateOf<Long?>(null) }
    var showExportStartDatePicker by remember { mutableStateOf(false) }
    var showExportEndDatePicker by remember { mutableStateOf(false) }

    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Pagination State
    val itemsPerPage = 40
    var currentPage by remember { mutableStateOf(0) }

    // Bottom Sheet State for Details
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dropdown States
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // Dynamic Strings
    val typeOptions = mapOf(
        "All" to stringResource(R.string.filter_all_type),
        "Income" to stringResource(R.string.tab_income),
        "Expense" to stringResource(R.string.tab_expense),
        "Lending" to stringResource(R.string.tab_receivable),
        "Borrowing" to stringResource(R.string.tab_payable)
    )

    val monthOptions = mapOf(
        "All Time" to stringResource(R.string.filter_all_time),
        "This Month" to stringResource(R.string.filter_this_month),
        "Last Month" to stringResource(R.string.filter_last_month),
        "Custom" to stringResource(R.string.filter_custom)
    )

    // Main List Filter Logic
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
                val start = customStartDate ?: 0L
                val end = customEndDate?.let { it + 86400000L - 1L } ?: Long.MAX_VALUE
                tx.date in start..end
            }
            else -> true // All Time
        }

        matchesType && matchesSearch && matchesMonth
    }.sortedByDescending { it.date }

    LaunchedEffect(selectedType, selectedMonth, searchQuery) {
        currentPage = 0
    }

    val totalPages = maxOf(1, ceil(filteredTransactions.size.toDouble() / itemsPerPage).toInt())
    val startIndex = currentPage * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, filteredTransactions.size)
    val currentItems = if (filteredTransactions.isEmpty()) emptyList() else filteredTransactions.subList(startIndex, endIndex)

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)
    val lendingColor = Color(0xFF3B82F6) // Blue
    val borrowingColor = Color(0xFFF59E0B) // Amber/Yellow

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 150 },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Top Bar: Search Bar on Left, Icons on Right ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expanded Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.hint_search), color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icons Row (Category, Date, Download)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category/Type Dropdown Icon
                        Box {
                            val typeTint = if (selectedType == "All") MaterialTheme.colorScheme.primary else Color(0xFFF59E0B)
                            SequentialWigglingIcon(
                                icon = Icons.Rounded.Category,
                                tint = typeTint,
                                iconIndex = 0, // প্রথম বাটন
                                onClick = { typeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = typeDropdownExpanded,
                                onDismissRequest = { typeDropdownExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                typeOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                fontWeight = if (selectedType == key) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selectedType == key) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                                            )
                                        },
                                        onClick = {
                                            selectedType = key
                                            typeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date/Time Dropdown Icon
                        Box {
                            val dateTint = if (selectedMonth == "This Month" || selectedMonth == "All Time") MaterialTheme.colorScheme.primary else Color(0xFFF59E0B)
                            SequentialWigglingIcon(
                                icon = Icons.Rounded.Schedule,
                                tint = dateTint,
                                iconIndex = 1, // দ্বিতীয় বাটন
                                onClick = { monthDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                monthOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                fontWeight = if (selectedMonth == key) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selectedMonth == key) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                                            )
                                        },
                                        onClick = {
                                            selectedMonth = key
                                            monthDropdownExpanded = false
                                            if (key == "Custom") {
                                                showCustomDateDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Download Icon
                        SequentialWigglingIcon(
                            icon = Icons.Rounded.Download,
                            tint = Color.White,
                            iconIndex = 2, // তৃতীয় বাটন
                            isGradientBg = true, // কালারফুল ব্যাকগ্রাউন্ড
                            onClick = { showExportSheet = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Dialogs for Custom Date ---
                if (showCustomDateDialog) {
                    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                    val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.hint_select_start_date)
                    val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.hint_select_end_date)

                    AlertDialog(
                        onDismissRequest = {
                            showCustomDateDialog = false
                            if (selectedMonth == "Custom" && customStartDate == null && customEndDate == null) selectedMonth = "This Month"
                        },
                        title = { Text(stringResource(R.string.title_select_date), fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(startStr, fontSize = 16.sp)
                                }
                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(endStr, fontSize = 16.sp)
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showCustomDateDialog = false; selectedMonth = "Custom" }) { Text(stringResource(R.string.btn_confirm), fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showCustomDateDialog = false; customStartDate = null; customEndDate = null; selectedMonth = "This Month" }) { Text(stringResource(R.string.btn_reset), color = Color.Red) } }
                    )
                }

                if (showStartDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = { TextButton(onClick = { customStartDate = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) } }
                    ) { DatePicker(state = datePickerState, title = { Text(" " + stringResource(R.string.title_start_date), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
                }

                if (showEndDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = { TextButton(onClick = { customEndDate = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) } }
                    ) { DatePicker(state = datePickerState, title = { Text(" " + stringResource(R.string.title_end_date), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
                }

                if (currentItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.msg_no_transactions), color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(currentItems.size, key = { currentItems[it].id }) { index ->
                            AllTransactionItemCard(
                                transaction = currentItems[index],
                                modifier = Modifier.animateItemPlacement(tween(300)),
                                onClick = { selectedTransaction = currentItems[index] }
                            )
                        }
                    }
                }

                // Pagination Row
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_previous))
                        }
                        Text(text = stringResource(R.string.label_page_info, currentPage + 1, totalPages), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1) {
                            Text(stringResource(R.string.btn_next))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }
        }

        // Details Bottom Sheet
        if (selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedTransaction = null },
                sheetState = detailsSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                AllTransactionDetailsSheet(
                    transaction = selectedTransaction!!,
                    onEdit = {
                        val id = selectedTransaction!!.id
                        val route = when (selectedTransaction!!.type) {
                            "Income" -> "add_income?transactionId=$id"
                            "Expense" -> "add_expense?transactionId=$id"
                            "Lending" -> "add_debt_credit?type=Lending&transactionId=$id"
                            "Borrowing" -> "add_debt_credit?type=Borrowing&transactionId=$id"
                            else -> "add_income?transactionId=$id"
                        }
                        selectedTransaction = null
                        navController.navigate(route)
                    },
                    onDelete = {
                        viewModel.deleteTransaction(selectedTransaction!!)
                        selectedTransaction = null
                    }
                )
            }
        }

        // --- PDF Export Bottom Sheet ---
        if (showExportSheet) {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val recentMonths = remember {
                (0..11).map {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, -it)
                    monthFormat.format(c.time) to c
                }
            }
            if (exportSpecificMonth.isEmpty()) exportSpecificMonth = recentMonths.first().first

            val availableCategories = remember(exportType, allTransactions) {
                allTransactions
                    .filter { exportType == "All" || it.type == exportType }
                    .map { it.category }
                    .distinct()
                    .sorted()
            }

            ModalBottomSheet(
                onDismissRequest = { if (!isGeneratingPdf) showExportSheet = false },
                sheetState = exportSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.msg_generating_report), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text(stringResource(R.string.title_download_report), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(24.dp))

                        // 1. Transaction Type
                        Text(stringResource(R.string.step_transaction_type), modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { PdfOptionButton(text = stringResource(R.string.filter_all), isSelected = exportType == "All", color = MaterialTheme.colorScheme.primary) { exportType = "All" } }
                            item { PdfOptionButton(text = stringResource(R.string.tab_income), isSelected = exportType == "Income", color = incomeColor) { exportType = "Income" } }
                            item { PdfOptionButton(text = stringResource(R.string.tab_expense), isSelected = exportType == "Expense", color = expenseColor) { exportType = "Expense" } }
                            item { PdfOptionButton(text = stringResource(R.string.tab_receivable), isSelected = exportType == "Lending", color = lendingColor) { exportType = "Lending" } }
                            item { PdfOptionButton(text = stringResource(R.string.tab_payable), isSelected = exportType == "Borrowing", color = borrowingColor) { exportType = "Borrowing" } }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Time Period
                        Text(stringResource(R.string.step_time_period), modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { PdfOptionButton(stringResource(R.string.filter_this_month), exportPeriod == "This Month", MaterialTheme.colorScheme.primary) { exportPeriod = "This Month" } }
                            item { PdfOptionButton(stringResource(R.string.filter_specific_month), exportPeriod == "Specific Month", MaterialTheme.colorScheme.primary) { exportPeriod = "Specific Month" } }
                            item { PdfOptionButton(stringResource(R.string.filter_custom_date), exportPeriod == "Custom", MaterialTheme.colorScheme.primary) { exportPeriod = "Custom" } }
                            item { PdfOptionButton(stringResource(R.string.filter_all_time_report), exportPeriod == "All Time", MaterialTheme.colorScheme.primary) { exportPeriod = "All Time" } }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sub-options for Time Period
                        if (exportPeriod == "Specific Month") {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = exportSpecificMonth, onValueChange = {}, readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    recentMonths.forEach { (monthStr, _) ->
                                        DropdownMenuItem(text = { Text(monthStr) }, onClick = { exportSpecificMonth = monthStr; expanded = false })
                                    }
                                }
                            }
                        } else if (exportPeriod == "Custom") {
                            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showExportStartDatePicker = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text(exportStartDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.title_start_date), fontSize = 13.sp)
                                }
                                OutlinedButton(onClick = { showExportEndDatePicker = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text(exportEndDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.title_end_date), fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Category Filter
                        Text(stringResource(R.string.step_category_optional), modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        var catExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                            OutlinedTextField(
                                value = if (exportCategory == "All") stringResource(R.string.filter_all_categories) else exportCategory,
                                onValueChange = {}, readOnly = true,
                                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                            )
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_all_categories), fontWeight = FontWeight.Bold) }, onClick = { exportCategory = "All"; catExpanded = false })
                                availableCategories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = { exportCategory = cat; catExpanded = false })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                isGeneratingPdf = true
                                coroutineScope.launch {
                                    // Apply Export Filters
                                    val pdfTransactions = allTransactions.filter { tx ->
                                        val typeMatch = exportType == "All" || tx.type == exportType
                                        val catMatch = exportCategory == "All" || tx.category == exportCategory

                                        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                                        val txMonth = cal.get(Calendar.MONTH)
                                        val txYear = cal.get(Calendar.YEAR)
                                        val currentCal = Calendar.getInstance()

                                        val timeMatch = when (exportPeriod) {
                                            "This Month" -> txMonth == currentCal.get(Calendar.MONTH) && txYear == currentCal.get(Calendar.YEAR)
                                            "Specific Month" -> {
                                                val targetCal = recentMonths.find { it.first == exportSpecificMonth }?.second
                                                targetCal != null && txMonth == targetCal.get(Calendar.MONTH) && txYear == targetCal.get(Calendar.YEAR)
                                            }
                                            "Custom" -> {
                                                val s = exportStartDate ?: 0L
                                                val e = exportEndDate?.let { it + 86400000L - 1L } ?: Long.MAX_VALUE
                                                tx.date in s..e
                                            }
                                            else -> true // All Time
                                        }
                                        typeMatch && catMatch && timeMatch
                                    }.sortedBy { it.date } // Chronological for PDF

                                    val reportIncStr = context.getString(R.string.report_income)
                                    val reportExpStr = context.getString(R.string.report_expense)
                                    val reportLenStr = context.getString(R.string.report_lending)
                                    val reportBorStr = context.getString(R.string.report_borrowing)
                                    val reportAllStr = context.getString(R.string.report_all_transactions)

                                    val title = when (exportType) {
                                        "Income" -> reportIncStr
                                        "Expense" -> reportExpStr
                                        "Lending" -> reportLenStr
                                        "Borrowing" -> reportBorStr
                                        else -> reportAllStr
                                    } + if (exportCategory != "All") " ($exportCategory)" else ""

                                    val dateStr = when (exportPeriod) {
                                        "This Month" -> context.getString(R.string.filter_this_month)
                                        "Specific Month" -> exportSpecificMonth
                                        "Custom" -> {
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            val s = exportStartDate?.let { sdf.format(Date(it)) } ?: ""
                                            val e = exportEndDate?.let { sdf.format(Date(it)) } ?: context.getString(R.string.today)
                                            if (s.isNotEmpty()) "$s ${context.getString(R.string.to)} $e" else context.getString(R.string.report_custom_range)
                                        }
                                        else -> context.getString(R.string.report_all_time)
                                    }

                                    val timeStamp = SimpleDateFormat("dd-MMM-yyyy_hh-mm-a", Locale.US).format(Date())
                                    val customFileName = "${context.getString(R.string.report_file_prefix)}-$timeStamp.pdf"

                                    PdfGenerator.generatePdf(
                                        context = context,
                                        transactions = pdfTransactions,
                                        reportTitle = title,
                                        dateRange = dateStr,
                                        fileName = customFileName
                                    )
                                    isGeneratingPdf = false
                                    showExportSheet = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_generate_download_pdf), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Export Custom Date Pickers
        if (showExportStartDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportStartDate ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showExportStartDatePicker = false },
                confirmButton = { TextButton(onClick = { exportStartDate = datePickerState.selectedDateMillis; showExportStartDatePicker = false }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showExportStartDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) } }
            ) { DatePicker(state = datePickerState, title = { Text(" " + stringResource(R.string.title_start_date), modifier = Modifier.padding(16.dp)) }) }
        }
        if (showExportEndDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportEndDate ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showExportEndDatePicker = false },
                confirmButton = { TextButton(onClick = { exportEndDate = datePickerState.selectedDateMillis; showExportEndDatePicker = false }) { Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showExportEndDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) } }
            ) { DatePicker(state = datePickerState, title = { Text(" " + stringResource(R.string.title_end_date), modifier = Modifier.padding(16.dp)) }) }
        }
    }
}

// ----------------------------------------------------------------------
// ৩টি বাটনে সিকোয়েনশিয়াল অ্যানিমেশনের জন্য কাস্টম কম্পোজেবল
// ----------------------------------------------------------------------
@Composable
fun SequentialWigglingIcon(
    icon: ImageVector,
    tint: Color,
    iconIndex: Int, // 0 = Category, 1 = Date, 2 = Download
    isGradientBg: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sequential_wiggle_$iconIndex")

    // ৪ সেকেন্ডের একটি সাইকেল, যেখানে প্রতি বাটন তার ইনডেক্স অনুযায়ী আলাদা সময়ে নড়বে
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                val start = iconIndex * 500 // প্রথমটা ০ms, দ্বিতীয়টা ৫০০ms, তৃতীয়টা ১০০০ms এ শুরু হবে

                0f at 0
                if (start > 0) { 0f at start }

                -15f at start + 100
                15f at start + 250
                -15f at start + 400
                0f at start + 500

                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_anim_$iconIndex"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                val start = iconIndex * 500

                1f at 0
                if (start > 0) { 1f at start }

                1.15f at start + 150
                1.15f at start + 350
                1f at start + 500

                1f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_anim_$iconIndex"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .rotate(rotation)
            .scale(scale)
            .then(
                if (isGradientBg) {
                    Modifier
                        .shadow(6.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
                            ),
                            shape = CircleShape
                        )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.1f))
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGradientBg) Color.White else tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Reusable Option Button for PDF Export Dialog
@Composable
fun PdfOptionButton(text: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) color else Color.Gray
        ),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color.LightGray),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
    }
}

// ----------------------------------------------------------------------
// Transaction List Item (Idea 2 Design: Left Color Bar, Pill Badge)
// Adjusted for a slimmer, more compact look
// ----------------------------------------------------------------------
@Composable
private fun AllTransactionItemCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (themeColor, amountPrefix) = when (transaction.type) {
        "Income" -> Pair(Color(0xFF10B981), "+") // Emerald
        "Expense" -> Pair(Color(0xFFF43F5E), "-") // Rose
        "Borrowing" -> Pair(Color(0xFFF59E0B), "+") // Amber
        "Lending" -> Pair(Color(0xFF3B82F6), "-") // Blue
        else -> Pair(Color.Gray, "")
    }

    val dateString = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale.getDefault()).format(Date(transaction.date))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)) // Reduced from 16dp
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Softer shadow
        shape = RoundedCornerShape(12.dp) // Reduced from 16dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ২. ক্যাটাগরি আইকন
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ৩. কন্টেন্ট এরিয়া (Reduced Padding)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp), // Even slimmer padding (12, 10 to 10, 8)
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // টাইটেল এবং ক্যাটাগরি
                    val titleText = transaction.title.trim()
                    val categoryText = transaction.category.trim()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (titleText.isNotBlank() && titleText != "-") titleText else categoryText,
                            fontSize = 14.sp, // Reduced from 16.sp
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
                                fontSize = 12.sp, // Reduced from 13.sp
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // পিল ব্যাজ (Separated Icon and Text like the screenshot)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ক্যালেন্ডার আইকন বক্স
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // ডেট এবং টাইম বক্স
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateString,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp)) // Reduced gap

                // ৪. ডানদিকের টাকার অংক
                Text(
                    text = "$amountPrefix ৳${transaction.amount.toInt()}",
                    fontSize = 15.sp, // Reduced from 18.sp
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor
                )
            }
        }
    }
}

// Bottom Sheet Content
@Composable
private fun AllTransactionDetailsSheet(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // দেনা-পাওনার কালার লজিক
    val (themeColor, lightThemeColor, amountPrefix) = when (transaction.type) {
        "Income" -> Triple(Color(0xFF4CAF50), Color(0xFFE8F5E9), "+")
        "Expense" -> Triple(Color(0xFFF44336), Color(0xFFFFEBEE), "-")
        "Borrowing" -> Triple(Color(0xFFF59E0B), Color(0xFFFEF3C7), "+") // হলুদ
        "Lending" -> Triple(Color(0xFF3B82F6), Color(0xFFEFF6FF), "-") // নীল
        else -> Triple(Color.Gray, Color(0xFFF1F5F9), "")
    }

    val dateString = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(transaction.date))
    val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date))

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
            Icon(
                imageVector = getCategoryIcon(transaction.category),
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(36.dp)
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
            AllTxDetailRow(icon = Icons.Rounded.Category, label = stringResource(R.string.label_category), value = transaction.category, iconTint = themeColor)
            AllTxDetailRow(icon = Icons.Rounded.CalendarToday, label = stringResource(R.string.label_date_only), value = dateString, iconTint = themeColor)
            AllTxDetailRow(icon = Icons.Rounded.Schedule, label = stringResource(R.string.label_time_only), value = timeString, iconTint = themeColor)

            // Note
            val note = try { transaction.javaClass.getMethod("getNote").invoke(transaction) as? String } catch (e: Exception) { null }
            if (!note.isNullOrBlank()) {
                AllTxDetailRow(icon = Icons.Rounded.Info, label = stringResource(R.string.label_note), value = note, iconTint = themeColor)
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
                Text(stringResource(R.string.btn_edit), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_delete), fontWeight = FontWeight.Bold, color = Color.White)
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

// ----------------------------------------------------------------------
// ক্যাটাগরি অনুযায়ী অটোমেটিক আইকন নির্ধারণের ফাংশন
// ----------------------------------------------------------------------
@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    // English and Bangla categories checking for robust backward compatibility
    return when (category) {
        "খাবার", "Food" -> Icons.Rounded.Restaurant
        "যাতায়াত", "Transport" -> Icons.Rounded.DirectionsCar
        "বাসা ভাড়া", "Rent" -> Icons.Rounded.Home
        "শপিং", "Shopping" -> Icons.Rounded.ShoppingCart
        "বিল", "Bills" -> Icons.Rounded.Receipt
        "চিকিৎসা", "Health" -> Icons.Rounded.LocalHospital
        "শিক্ষা", "Education" -> Icons.Rounded.School
        "বেতন", "Salary" -> Icons.Rounded.AccountBalanceWallet
        "ফ্রিল্যান্সিং", "Freelance", "Freelancing" -> Icons.Rounded.Computer
        "উপহার", "Gift" -> Icons.Rounded.CardGiftcard
        "ব্যবসা", "Business" -> Icons.Rounded.Store
        "ধার দিয়েছি", "পাওনা", "Lent", "Receivable" -> Icons.Rounded.Handshake
        "ধার নিয়েছি", "দেনা", "Borrowed", "Payable", "ধার/লোন", "Loan/Borrow" -> Icons.Rounded.MoneyOff
        else -> Icons.Rounded.Category
    }
}