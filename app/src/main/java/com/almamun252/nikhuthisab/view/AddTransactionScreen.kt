package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    isIncome: Boolean = false,
    transactionId: Int? = null,
    viewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()

    // Screen Entry Animation State
    var isVisible by remember { mutableStateOf(false) }

    // Trigger animation when screen opens
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val existingTransaction = transactions.find { it.id == transactionId }

    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var tempDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }

    // নতুন স্টেট: ধারে খরচের জন্য
    var isBorrowedExpense by remember { mutableStateOf(false) }
    var lenderName by remember { mutableStateOf("") }

    // ধারে খরচের পরিশোধের তারিখের (Due Date) জন্য স্টেট
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var tempDueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }

    val type = if (isIncome) "Income" else "Expense"

    // আধুনিক থিম কালার
    val themeColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val lightThemeColor = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    val screenTitle = if (transactionId != null) {
        if (isIncome) "আয় আপডেট করুন" else "ব্যয় আপডেট করুন"
    } else {
        if (isIncome) "নতুন আয়" else "নতুন ব্যয়"
    }

    LaunchedEffect(existingTransaction) {
        existingTransaction?.let {
            amount = if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
            title = if (it.title == "-") "" else it.title
            category = if (it.category == "অন্যান্য") "" else it.category
            selectedDateMillis = it.date
            note = it.note ?: ""
            // যদি এটি সাধারণ ব্যয় হয় এবং ভবিষ্যতে এডিট করতে আসে তখন আর ধারে খরচ দেখানোর দরকার নেই
        }
    }

    val dynamicSuggestions = transactions
        .filter { it.type == type && it.title.isNotBlank() && it.title != "-" }
        .groupingBy { it.title }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(5)
        .map { it.first }

    val defaultChips = if (isIncome) listOf("বেতন", "উপহার", "বোনাস", "ফ্রিল্যান্সিং")
    else listOf("রিকশা", "বাস", "চা", "খাবার", "ইন্টারনেট")
    val suggestionChips = dynamicSuggestions.ifEmpty { defaultChips }

    val defaultCategories = if (isIncome) {
        listOf("বেতন", "ফ্রিল্যান্সিং", "উপহার", "ব্যবসা")
    } else {
        listOf("খাবার", "যাতায়াত", "বাসা ভাড়া", "শপিং", "বিল", "চিকিৎসা", "শিক্ষা")
    }

    val dbCategories = transactions
        .filter { it.type == type && it.category.isNotBlank() }
        .map { it.category }

    val allCategories = (defaultCategories + dbCategories)
        .distinct()
        .filter { it != "অন্যান্য" } + "অন্যান্য"

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 150 },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Custom Top Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Title
                    Text(text = screenTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)

                    // Delete Button or Empty Space
                    if (transactionId != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE))
                                .clickable {
                                    existingTransaction?.let {
                                        viewModel.deleteTransaction(it)
                                        Toast.makeText(context, "হিসাবটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                // --- Amount Input Field ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("টাকার পরিমাণ *", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(lightThemeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("৳", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = themeColor)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = themeColor),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedLabelColor = themeColor
                    ),
                    singleLine = true
                )

                // --- Form Fields ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // --- নতুন ফিচার: নগদ নাকি ধারে খরচ? (শুধুমাত্র ব্যয়ের ক্ষেত্রে এবং নতুন এন্ট্রির সময় দেখাবে) ---
                    if (!isIncome && transactionId == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("খরচের ধরন নির্বাচন করুন:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // নগদ অপশন
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (!isBorrowedExpense) themeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { isBorrowedExpense = false }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("নগদ খরচ", color = if (!isBorrowedExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                                // ধারে অপশন
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isBorrowedExpense) themeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { isBorrowedExpense = true }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ধারে খরচ", color = if (isBorrowedExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // ধারে খরচ হলে ধার প্রদানকারীর নাম ও ডেডলাইন নেওয়ার ফিল্ড
                        AnimatedVisibility(visible = isBorrowedExpense) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = lenderName,
                                    onValueChange = { lenderName = it },
                                    label = { Text("কার কাছ থেকে ধার করা হলো? *") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = themeColor) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeColor,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                        focusedLabelColor = themeColor
                                    ),
                                    singleLine = true
                                )

                                // Due Date (Deadline)
                                val dueSdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = dueDateMillis?.let { dueSdf.format(Date(it)) } ?: "কবে পরিশোধ করবেন? (ঐচ্ছিক)",
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("পরিশোধের ডেডলাইন") },
                                        leadingIcon = { Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = if (dueDateMillis != null) themeColor else Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = themeColor,
                                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                            focusedLabelColor = themeColor
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDueDatePicker = true }
                                    )
                                    if (dueDateMillis != null) {
                                        IconButton(
                                            onClick = { dueDateMillis = null },
                                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Title Field
                    Column {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("খরচের নাম (ঐচ্ছিক)") },
                            leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedLabelColor = themeColor
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Suggestion Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestionChips) { chip ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (title == chip) themeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { title = chip }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chip,
                                        color = if (title == chip) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = if (title == chip) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Category Field
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("ক্যাটাগরি") },
                            leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null, tint = themeColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedLabelColor = themeColor
                            )
                        )

                        if (allCategories.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                allCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(text = cat, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date & Time Field
                    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))
                    val dateTimeString = sdf.format(Date(selectedDateMillis))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateTimeString,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("তারিখ ও সময়") },
                            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedLabelColor = themeColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        // Clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showDatePicker = true }
                        )
                    }

                    // Note Field
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("বিস্তারিত নোট (ঐচ্ছিক)") },
                        leadingIcon = { Icon(Icons.Rounded.Subject, contentDescription = null, tint = themeColor) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedLabelColor = themeColor
                        )
                    )
                }

                // --- Save / Update Button ---
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()

                        // ভ্যালিডেশন
                        if (amountValue == null || amountValue <= 0) {
                            Toast.makeText(context, "সঠিক টাকার পরিমাণ দিন!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // যদি ধারে খরচ হয়, তবে ধার দাতার নাম চেক করা
                        if (!isIncome && transactionId == null && isBorrowedExpense && lenderName.trim().isEmpty()) {
                            Toast.makeText(context, "দয়া করে ধার প্রদানকারীর নাম লিখুন!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val finalTitle = title.trim()

                        // মূল ট্রানজেকশন (ব্যয় বা আয়)
                        val transaction = Transaction(
                            id = existingTransaction?.id ?: 0,
                            title = finalTitle.ifEmpty { "-" },
                            amount = amountValue,
                            type = type,
                            category = category.ifEmpty { "অন্যান্য" },
                            date = selectedDateMillis,
                            note = note.trim().ifEmpty { null }
                        )

                        if (transactionId != null && existingTransaction != null) {
                            viewModel.deleteTransaction(existingTransaction)
                            viewModel.insertTransaction(transaction)
                            Toast.makeText(context, "হিসাব সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            // ১. মূল খরচ বা আয় সেভ করা
                            viewModel.insertTransaction(transaction)

                            // ২. যদি ধারে খরচ হয়, তাহলে দ্বিতীয় একটি 'ঋণ' এন্ট্রি সেভ করা (ডাবল এন্ট্রি লজিক)
                            if (!isIncome && isBorrowedExpense) {
                                val borrowTitleContext = finalTitle.ifEmpty { category.ifEmpty { "অন্যান্য" } }
                                val borrowingTx = Transaction(
                                    id = 0,
                                    title = lenderName.trim(), // ধার দাতার নাম
                                    amount = amountValue,
                                    type = "Borrowing", // ঋণ হিসেবে সেভ হবে
                                    category = "ধার/লোন",
                                    date = selectedDateMillis,
                                    note = "ধারে করা খরচ: $borrowTitleContext",
                                    dueDate = dueDateMillis // ডেডলাইন যুক্ত করা হলো
                                )
                                viewModel.insertTransaction(borrowingTx)
                            }

                            Toast.makeText(context, "সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }

                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = if (transactionId != null) "আপডেট করুন" else "সেভ করুন",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempDateMillis = it
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("পরবর্তী (সময়)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("বাতিল", color = Color.Red)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Time Picker Dialog ---
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = tempDateMillis
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)

                    selectedDateMillis = calendar.timeInMillis
                    showTimePicker = false
                }) {
                    Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("বাতিল", color = Color.Red)
                }
            },
            title = { Text("সময় নির্বাচন করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // --- Due Date Picker (ডেডলাইনের তারিখ) ---
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { tempDueDateMillis = it }
                    showDueDatePicker = false
                    showDueTimePicker = true // ডেডলাইনের সময় সিলেক্ট করার জন্য
                }) { Text("পরবর্তী (সময়)", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("বাতিল", color = Color.Red) } }
        ) { DatePicker(state = datePickerState, title = { Text(" ডেডলাইন নির্বাচন করুন", modifier = Modifier.padding(16.dp)) }) }
    }

    // --- Due Time Picker (ডেডলাইনের সময়) ---
    if (showDueTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDateMillis ?: System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showDueTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = tempDueDateMillis
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    dueDateMillis = calendar.timeInMillis
                    showDueTimePicker = false
                }) {
                    Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueTimePicker = false }) { Text("বাতিল", color = Color.Red) }
            },
            title = { Text("ডেডলাইনের সময়", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}