package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

    // Filters State for Main List
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf("All Time") }

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

    val typeOptions = mapOf(
        "All" to "সব ধরন",
        "Income" to "আয়",
        "Expense" to "ব্যয়",
        "Lending" to "পাওনা",
        "Borrowing" to "দেনা"
    )
    val monthOptions = mapOf(
        "All Time" to "সব সময়",
        "This Month" to "এই মাস",
        "Last Month" to "গত মাস",
        "Custom" to "কাস্টম 📅"
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

                // Search Bar and Download Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("লেনদেন খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600, delayMillis = 300))
                    ) {
                        WigglingDownloadIconButton(
                            onClick = { showExportSheet = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown Filters Row (Category & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = typeOptions[selectedType] ?: "সব ধরন",
                            onValueChange = { },
                            leadingIcon = {
                                Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            typeOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (selectedType == key) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedType == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

                    // Month/Time Dropdown
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = monthOptions[selectedMonth] ?: "সব সময়",
                            onValueChange = { },
                            leadingIcon = {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            monthOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (selectedMonth == key) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedMonth == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                }

                if (showCustomDateDialog) {
                    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                    val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
                    val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

                    AlertDialog(
                        onDismissRequest = {
                            showCustomDateDialog = false
                            if (selectedMonth == "Custom" && customStartDate == null && customEndDate == null) selectedMonth = "All Time"
                        },
                        title = { Text("তারিখ নির্বাচন করুন", fontWeight = FontWeight.Bold) },
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
                        confirmButton = { TextButton(onClick = { showCustomDateDialog = false; selectedMonth = "Custom" }) { Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showCustomDateDialog = false; customStartDate = null; customEndDate = null; selectedMonth = "All Time" }) { Text("রিসেট", color = Color.Red) } }
                    )
                }

                if (showStartDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = { TextButton(onClick = { customStartDate = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল") } }
                    ) { DatePicker(state = datePickerState, title = { Text(" শুরুর তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
                }

                if (showEndDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = { TextButton(onClick = { customEndDate = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) } },
                        dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল") } }
                    ) { DatePicker(state = datePickerState, title = { Text(" শেষের তারিখ", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }) }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        verticalArrangement = Arrangement.spacedBy(8.dp), // কার্ডগুলোর মাঝের দূরত্ব কমানো হয়েছে (12dp থেকে 8dp)
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(currentItems, key = { it.id }) { tx ->
                            AllTransactionItemCard(
                                transaction = tx,
                                modifier = Modifier.animateItemPlacement(tween(300)),
                                onClick = { selectedTransaction = tx }
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
                            Text("পূর্ববর্তী")
                        }
                        Text(text = "পেজ ${currentPage + 1} / $totalPages", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1) {
                            Text("পরবর্তী")
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
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("bn", "BD"))
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
                        Text("রিপোর্ট তৈরি হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text("রিপোর্ট ডাউনলোড", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(24.dp))

                        // 1. Transaction Type
                        Text("১. লেনদেনের ধরন", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { PdfOptionButton(text = "সব", isSelected = exportType == "All", color = MaterialTheme.colorScheme.primary) { exportType = "All" } }
                            item { PdfOptionButton(text = "আয়", isSelected = exportType == "Income", color = incomeColor) { exportType = "Income" } }
                            item { PdfOptionButton(text = "ব্যয়", isSelected = exportType == "Expense", color = expenseColor) { exportType = "Expense" } }
                            item { PdfOptionButton(text = "পাওনা", isSelected = exportType == "Lending", color = lendingColor) { exportType = "Lending" } }
                            item { PdfOptionButton(text = "দেনা", isSelected = exportType == "Borrowing", color = borrowingColor) { exportType = "Borrowing" } }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Time Period
                        Text("২. সময়কাল", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { PdfOptionButton("চলতি মাস", exportPeriod == "This Month", MaterialTheme.colorScheme.primary) { exportPeriod = "This Month" } }
                            item { PdfOptionButton("নির্দিষ্ট মাস", exportPeriod == "Specific Month", MaterialTheme.colorScheme.primary) { exportPeriod = "Specific Month" } }
                            item { PdfOptionButton("কাস্টম ডেট", exportPeriod == "Custom", MaterialTheme.colorScheme.primary) { exportPeriod = "Custom" } }
                            item { PdfOptionButton("শুরু থেকে আজ", exportPeriod == "All Time", MaterialTheme.colorScheme.primary) { exportPeriod = "All Time" } }
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
                            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showExportStartDatePicker = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text(exportStartDate?.let { sdf.format(Date(it)) } ?: "শুরু", fontSize = 13.sp)
                                }
                                OutlinedButton(onClick = { showExportEndDatePicker = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                    Text(exportEndDate?.let { sdf.format(Date(it)) } ?: "শেষ", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Category Filter
                        Text("৩. ক্যাটাগরি (ঐচ্ছিক)", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        var catExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                            OutlinedTextField(
                                value = if (exportCategory == "All") "সব ক্যাটাগরি" else exportCategory,
                                onValueChange = {}, readOnly = true,
                                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                            )
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                DropdownMenuItem(text = { Text("সব ক্যাটাগরি", fontWeight = FontWeight.Bold) }, onClick = { exportCategory = "All"; catExpanded = false })
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

                                    val title = when (exportType) {
                                        "Income" -> "আয় রিপোর্ট"
                                        "Expense" -> "ব্যয় রিপোর্ট"
                                        "Lending" -> "পাওনা রিপোর্ট"
                                        "Borrowing" -> "দেনা রিপোর্ট"
                                        else -> "সকল লেনদেন রিপোর্ট"
                                    } + if (exportCategory != "All") " ($exportCategory)" else ""

                                    val dateStr = when (exportPeriod) {
                                        "This Month" -> "চলতি মাস"
                                        "Specific Month" -> exportSpecificMonth
                                        "Custom" -> {
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("bn", "BD"))
                                            val s = exportStartDate?.let { sdf.format(Date(it)) } ?: ""
                                            val e = exportEndDate?.let { sdf.format(Date(it)) } ?: "আজ"
                                            if (s.isNotEmpty()) "$s হতে $e" else "কাস্টম রেঞ্জ"
                                        }
                                        else -> "শুরু থেকে আজ পর্যন্ত"
                                    }

                                    val timeStamp = SimpleDateFormat("dd-MMM-yyyy_hh-mm-a", Locale("bn", "BD")).format(Date())
                                    val customFileName = "হিসাব-$timeStamp.pdf"

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
                            Text("PDF তৈরি ও ডাউনলোড করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                confirmButton = { TextButton(onClick = { exportStartDate = datePickerState.selectedDateMillis; showExportStartDatePicker = false }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showExportStartDatePicker = false }) { Text("বাতিল") } }
            ) { DatePicker(state = datePickerState, title = { Text(" শুরুর তারিখ", modifier = Modifier.padding(16.dp)) }) }
        }
        if (showExportEndDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = exportEndDate ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showExportEndDatePicker = false },
                confirmButton = { TextButton(onClick = { exportEndDate = datePickerState.selectedDateMillis; showExportEndDatePicker = false }) { Text("ঠিক আছে", fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { showExportEndDatePicker = false }) { Text("বাতিল") } }
            ) { DatePicker(state = datePickerState, title = { Text(" শেষের তারিখ", modifier = Modifier.padding(16.dp)) }) }
        }
    }
}

// ----------------------------------------------------------------------
// নড়াচড়া করা (Wiggling) আকর্ষণীয় ডাউনলোড বাটন কম্পোজেবল
// ----------------------------------------------------------------------
@Composable
fun WigglingDownloadIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "wiggle_transition")

    // রোটেশন অ্যানিমেশন
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                0f at 0
                0f at 2800        // ৮০% সময় স্থির থাকবে
                -15f at 2950      // তারপর নড়বে
                15f at 3100
                -15f at 3250
                0f at 3500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_anim"
    )

    // স্কেল অ্যানিমেশন (নড়ার সময় একটু বড় হবে)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                1f at 0
                1f at 2800
                1.15f at 2950
                1.15f at 3100
                1.15f at 3250
                1f at 3500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_anim"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .rotate(rotation)
            .scale(scale)
            .shadow(6.dp, CircleShape) // আকর্ষণীয় শ্যাডো
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1)) // ব্লু-ইন্ডিগো গ্রেডিয়েন্ট
                ),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = "ডাউনলোড রিপোর্ট",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
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

// Transaction List Item
@Composable
private fun AllTransactionItemCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // দেনা-পাওনার কালার লজিক
    val (themeColor, lightThemeColor, amountPrefix) = when (transaction.type) {
        "Income" -> Triple(Color(0xFF4CAF50), Color(0xFFE8F5E9), "+")
        "Expense" -> Triple(Color(0xFFF44336), Color(0xFFFFEBEE), "-")
        "Borrowing" -> Triple(Color(0xFFF59E0B), Color(0xFFFEF3C7), "+") // হলুদ (Amber) এবং (+) সাইন
        "Lending" -> Triple(Color(0xFF3B82F6), Color(0xFFEFF6FF), "-") // নীল (Blue) এবং (-) সাইন
        else -> Triple(Color.Gray, Color(0xFFF1F5F9), "")
    }

    val dateString = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Date(transaction.date))

    // Card এর বদলে Box ব্যবহার করা হয়েছে গ্লো ইফেক্ট দেওয়ার জন্য
    Box(
        modifier = modifier
            .fillMaxWidth()
            // ট্রানজ্যাকশনের টাইপ অনুযায়ী ডায়নামিক গ্লোয়িং শ্যাডো
            .drawBehind {
                val shadowColor = themeColor.copy(alpha = 0.2f).toArgb() // গ্লো এর অপাসিটি কমানো হয়েছে যাতে হালকা লাগে

                val paint = Paint().asFrameworkPaint().apply {
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        12f, // ব্লার রেডিয়াস
                        0f,
                        6f, // নিচের দিকে হালকা শ্যাডো
                        shadowColor
                    )
                }

                drawContext.canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    12.dp.toPx(), // রাউন্ডেড কর্নার 16 থেকে 12 করা হয়েছে
                    12.dp.toPx(),
                    paint
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp), // আগের 16dp প্যাডিং কমিয়ে কার্ড স্লিম করা হয়েছে
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // আইকনের সাইজ 52dp থেকে কমিয়ে 40dp করা হয়েছে
                    .clip(CircleShape)
                    .background(lightThemeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.category.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp, // ফন্ট সাইজ 20sp থেকে কমানো হয়েছে
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp, // 16sp থেকে কমানো হয়েছে
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp)) // মাঝের গ্যাপ কমানো হয়েছে
                Text(
                    text = "${transaction.category} • $dateString",
                    fontSize = 11.sp, // 13sp থেকে কমানো হয়েছে
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "$amountPrefix ৳${transaction.amount.toInt()}",
                fontSize = 15.sp, // 18sp থেকে কমিয়ে 15sp করা হয়েছে
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
            val note = try { transaction.javaClass.getMethod("getNote").invoke(transaction) as? String } catch (e: Exception) { null }
            if (!note.isNullOrBlank()) {
                AllTxDetailRow(icon = Icons.Rounded.Info, label = "নোট", value = note, iconTint = themeColor)
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