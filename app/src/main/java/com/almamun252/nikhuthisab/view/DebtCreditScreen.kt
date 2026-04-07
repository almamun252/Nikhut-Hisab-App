package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
fun DebtCreditScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Tab State: 0 = Receivable (পাবো/পাওনা), 1 = Payable (দিবো/দেনা)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("সব সময়") }

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

    val lendings = baseFilteredList.filter { it.type == "Lending" }.sortedByDescending { it.date }
    val borrowings = baseFilteredList.filter { it.type == "Borrowing" }.sortedByDescending { it.date }

    val totalReceivable = lendings.sumOf { it.amount - it.settledAmount }.toFloat()
    val totalPayable = borrowings.sumOf { it.amount - it.settledAmount }.toFloat()

    val currentList = if (selectedTabIndex == 0) lendings else borrowings

    // Modern Premium Colors
    val themeColor = if (selectedTabIndex == 0) Color(0xFF10B981) else Color(0xFFF43F5E) // Emerald (পাওনা) vs Rose (দেনা)
    val lightThemeColor = if (selectedTabIndex == 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val bgColor = Color(0xFFF8FAFC) // Very Light Slate Background

    // Bottom Sheet States
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Payment Dialog State
    var showPaymentDialog by remember { mutableStateOf<Transaction?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }

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
                            val type = if (selectedTabIndex == 0) "Lending" else "Borrowing"
                            navController.navigate("add_debt_credit?type=$type&transactionId=-1")
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
                    Text(if (selectedTabIndex == 0) "নতুন পাওনা" else "নতুন দেনা", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF334155))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "দেনা-পাওনা খাতা",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimalist Tab Switcher with Glowing Effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp, // Glowing effect
                                shape = RoundedCornerShape(20.dp),
                                spotColor = themeColor.copy(alpha = 0.5f),
                                ambientColor = themeColor.copy(alpha = 0.3f)
                            )
                    ) {
                        CustomMinimalTab(
                            selectedIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            receivableAmount = totalReceivable,
                            payableAmount = totalPayable,
                            themeColor = themeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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

                    // Modern Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (selectedTabIndex == 0) "পাওনা খুঁজুন..." else "দেনা খুঁজুন...", color = Color.Gray, fontSize = 14.sp) },
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
                                            if (selectedTabIndex == 0) Icons.Rounded.Handshake else Icons.Rounded.Payments,
                                            contentDescription = null,
                                            tint = themeColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = if (selectedTabIndex == 0) "কাউকে এখনো টাকা দেননি!" else "কারো কাছ থেকে টাকা নেননি!",
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "নতুন হিসাব যোগ করতে নিচের বাটনে ক্লিক করুন",
                                        fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center
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
                                    ModernDebtCreditCard(
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
    }

    // --- Modern Bottom Sheet Details ---
    if (selectedTransaction != null) {
        val tx = selectedTransaction!!
        val isSettled = tx.amount - tx.settledAmount <= 0
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
                // Header Avatar
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(themeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tx.title.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = themeColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(tx.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                if (isSettled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF10B981).copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("সম্পূর্ণ পরিশোধিত ✅", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(24.dp))

                val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))
                val dueSdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(icon = Icons.Rounded.AccountBalanceWallet, label = "মোট পরিমাণ", value = "৳${tx.amount.toInt()}", color = themeColor)
                    DetailRow(icon = Icons.Rounded.CheckCircle, label = "পরিশোধ হয়েছে", value = "৳${tx.settledAmount.toInt()}", color = Color(0xFF10B981))
                    DetailRow(icon = Icons.Rounded.Pending, label = "বাকি আছে", value = "৳${(tx.amount - tx.settledAmount).toInt()}", color = if (isSettled) Color.Gray else themeColor)
                    DetailRow(icon = Icons.Rounded.CalendarToday, label = "প্রদানের তারিখ ও সময়", value = sdf.format(Date(tx.date)), color = Color.Gray)
                    if (tx.dueDate != null) {
                        val isOverdue = tx.dueDate < System.currentTimeMillis() && !isSettled
                        DetailRow(icon = Icons.Rounded.EventBusy, label = "ডেডলাইন", value = dueSdf.format(Date(tx.dueDate)), color = if (isOverdue) Color.Red else Color.Gray)
                    }
                    if (!tx.note.isNullOrBlank()) {
                        DetailRow(icon = Icons.Rounded.Subject, label = "নোট", value = tx.note, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isSettled) {
                    Button(
                        onClick = {
                            selectedTransaction = null
                            paymentAmountInput = ""
                            showPaymentDialog = tx
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedTabIndex == 0) "টাকা পেয়েছি (Add Payment)" else "টাকা দিয়েছি (Add Payment)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = {
                            val id = tx.id
                            selectedTransaction = null
                            try {
                                navController.navigate("add_debt_credit?type=${tx.type}&transactionId=$id")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Routing Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
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
                        onClick = { showDeleteDialog = true },
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

    // --- Partial Payment Dialog ---
    if (showPaymentDialog != null) {
        val tx = showPaymentDialog!!
        val remaining = tx.amount - tx.settledAmount

        AlertDialog(
            onDismissRequest = { showPaymentDialog = null },
            containerColor = Color.White,
            title = { Text("পরিশোধ আপডেট করুন", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
            text = {
                Column {
                    Text("বাকি আছে: ৳${remaining.toInt()}", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("কত টাকা?") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = themeColor, modifier = Modifier.padding(start = 12.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = paymentAmountInput.toDoubleOrNull()
                        if (input != null && input > 0 && input <= remaining) {
                            val updatedTx = tx.copy(settledAmount = tx.settledAmount + input)
                            viewModel.deleteTransaction(tx)
                            viewModel.insertTransaction(updatedTx)
                            Toast.makeText(context, "সফলভাবে যোগ হয়েছে!", Toast.LENGTH_SHORT).show()
                            showPaymentDialog = null
                        } else {
                            Toast.makeText(context, "সঠিক পরিমাণ দিন!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = null }) {
                    Text("বাতিল", color = Color(0xFF64748B))
                }
            }
        )
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

    // --- Start/End Date Pickers ---
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
        ) { DatePicker(state = datePickerState) }
    }

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
        ) { DatePicker(state = datePickerState) }
    }
}

// --- Date Filter Dropdown Component ---
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
fun CustomMinimalTab(selectedIndex: Int, onTabSelected: (Int) -> Unit, receivableAmount: Float, payableAmount: Float, themeColor: Color) {
    val tabTitles = listOf("পাবো (পাওনা)", "দিবো (দেনা)")
    val tabColors = listOf(Color(0xFF10B981), Color(0xFFF43F5E))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            val amount = if (index == 0) receivableAmount else payableAmount

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) themeColor.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) tabColors[index] else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "৳${amount.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isSelected) tabColors[index] else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// --- Modern Compact Card Design with Glowing Shadow ---
@Composable
fun ModernDebtCreditCard(transaction: Transaction, themeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val remaining = transaction.amount - transaction.settledAmount
    val progress = if (transaction.amount > 0) (transaction.settledAmount / transaction.amount).toFloat() else 0f
    val isSettled = remaining <= 0

    val sdf = SimpleDateFormat("dd MMM, yy", Locale("bn", "BD"))
    val isOverdue = transaction.dueDate != null && transaction.dueDate < System.currentTimeMillis() && !isSettled

    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1200), label = "progress")

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
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) { // Compact padding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(themeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(transaction.title.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(transaction.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(if (isSettled) "পরিশোধিত ✅" else "বাকি: ৳${remaining.toInt()}", fontSize = 11.sp, color = if (isSettled) Color(0xFF10B981) else Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("৳${transaction.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = themeColor)
                        if (transaction.dueDate != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = if (isOverdue) Color(0xFFF43F5E) else Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sdf.format(Date(transaction.dueDate)),
                                    fontSize = 10.sp,
                                    color = if (isOverdue) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (!isSettled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
                            color = themeColor,
                            trackColor = Color(0xFFF1F5F9),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${(animProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
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