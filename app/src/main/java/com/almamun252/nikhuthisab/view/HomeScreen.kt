package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val dbBalance by viewModel.currentBalance.collectAsState()

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    var selectedDateFilter by remember { mutableStateOf("চলতি মাস") }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val filteredTransactions = allTransactions.filter { tx: Transaction ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val txMonth = cal.get(Calendar.MONTH)
        val txYear = cal.get(Calendar.YEAR)

        val currentCal = Calendar.getInstance()
        val currMonth = currentCal.get(Calendar.MONTH)
        val currYear = currentCal.get(Calendar.YEAR)

        when (selectedDateFilter) {
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
    }

    // ফিল্টার করা নির্দিষ্ট সময়ের আয়-ব্যয়
    val totalIncome = filteredTransactions.filter { it.type == "Income" }.map { it.amount }.sum().toFloat()
    val totalExpense = filteredTransactions.filter { it.type == "Expense" }.map { it.amount }.sum().toFloat()

    val globalBorrowing = allTransactions.filter { it.type == "Borrowing" }.map { it.amount }.sum().toFloat()
    val globalLending = allTransactions.filter { it.type == "Lending" }.map { it.amount }.sum().toFloat()

    val globalBorrowingSettled = allTransactions.filter { it.type == "Borrowing" }.map { it.settledAmount }.sum().toFloat()
    val globalLendingSettled = allTransactions.filter { it.type == "Lending" }.map { it.settledAmount }.sum().toFloat()

    // বর্তমান ক্যাশ ব্যালেন্স (ডাটাবেস ব্যালেন্স + ঋণ থেকে পাওয়া ক্যাশ - ধার দেওয়া ক্যাশ)
    val balance = dbBalance.toFloat() + (globalBorrowing + globalLendingSettled) - (globalLending + globalBorrowingSettled)

    // রিয়েল-টাইম পাওনা এবং দেনা
    val realReceivable = globalLending - globalLendingSettled
    val realPayable = globalBorrowing - globalBorrowingSettled

    // রিয়েল-টাইম ডাইনামিক এলার্ট তৈরি
    val realAlerts = remember(allTransactions) {
        val alerts = mutableListOf<AlertItem>()
        val currentMillis = System.currentTimeMillis()
        val todayCal = Calendar.getInstance()

        allTransactions.forEach { tx: Transaction ->
            if (tx.type == "Lending" || tx.type == "Borrowing") {
                val amountDue = (tx.amount - tx.settledAmount).toFloat()
                if (amountDue > 0) {
                    val dueDate = tx.dueDate
                    if (dueDate != null) {
                        val dueCal = Calendar.getInstance().apply { timeInMillis = dueDate }
                        val isToday = todayCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                                todayCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

                        val diffMillis = dueCal.timeInMillis - currentMillis
                        val name = if (tx.title.isNotEmpty() && tx.title != "-") tx.title else "অজ্ঞাত"

                        if (diffMillis < 0 && !isToday) {
                            val msg = if (tx.type == "Lending") "সতর্কতা: $name এর ৳${amountDue.toInt()} দেওয়ার সময় পার হয়ে গেছে!"
                            else "সতর্কতা: $name কে ৳${amountDue.toInt()} পরিশোধ করার সময় পার হয়ে গেছে!"
                            alerts.add(AlertItem(msg, AlertType.OVERDUE))
                        } else if (isToday) {
                            val msg = if (tx.type == "Lending") "আজ $name এর ৳${amountDue.toInt()} দেওয়ার কথা"
                            else "আজ $name কে ৳${amountDue.toInt()} দিতে হবে"
                            alerts.add(AlertItem(msg, AlertType.TODAY))
                        } else if (diffMillis in 0..(86400000L * 3)) { // আগামী ৩ দিন
                            val sdf = SimpleDateFormat("dd MMM", Locale("bn", "BD"))
                            val dateStr = sdf.format(Date(dueDate))
                            val msg = if (tx.type == "Lending") "আগামী $dateStr তারিখে $name ৳${amountDue.toInt()} দেবেন"
                            else "আগামী $dateStr তারিখে $name কে ৳${amountDue.toInt()} দিতে হবে"
                            alerts.add(AlertItem(msg, AlertType.UPCOMING))
                        }
                    }
                }
            }
        }
        alerts
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 150 },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            DateRangeFilter(
                selectedOption = selectedDateFilter,
                onOptionSelected = { option ->
                    if (option == "কাস্টম রেঞ্জ") {
                        showCustomDateDialog = true
                    } else {
                        selectedDateFilter = option
                    }
                }
            )

            // --- Custom Date Range Dialogs ---
            if (showCustomDateDialog) {
                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
                val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

                AlertDialog(
                    onDismissRequest = { showCustomDateDialog = false },
                    title = { Text("তারিখ নির্বাচন করুন", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(startStr, fontSize = 16.sp)
                            }
                            OutlinedButton(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(endStr, fontSize = 16.sp)
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showCustomDateDialog = false; selectedDateFilter = "কাস্টম রেঞ্জ" }) { Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { showCustomDateDialog = false; customStartDate = null; customEndDate = null }) { Text("বাতিল", color = Color.Red) } }
                )
            }
            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = { TextButton(onClick = { customStartDate = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text("ঠিক আছে") } },
                    dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল") } }
                ) { DatePicker(state = datePickerState) }
            }
            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = { TextButton(onClick = { customEndDate = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text("ঠিক আছে") } },
                    dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল") } }
                ) { DatePicker(state = datePickerState) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ১. কাস্টম পাই চার্ট অথবা খালি অবস্থার (Empty State) লেআউট
            if (allTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.LightGray)
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("হিসাব শুরু করুন!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("আপনার প্রথম আয় বা ব্যয় যোগ করতে নিচের বাটনগুলোতে ক্লিক করুন।", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShortcutCard(title = "আয় যোগ", icon = Icons.Rounded.Add, color = Color(0xFF10B981), modifier = Modifier.weight(1f)) { navController.navigate("add_income") }
                            ShortcutCard(title = "ব্যয় যোগ", icon = Icons.Rounded.Add, color = Color(0xFFF43F5E), modifier = Modifier.weight(1f)) { navController.navigate("add_expense") }
                        }
                    }
                }
            } else {
                // --- শুধুমাত্র পাই চার্ট (মাঝখানে বড় করে) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomUnevenPieChart(income = totalIncome, expense = totalExpense, balance = balance)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ডাইনামিক এলার্ট টিকার ---
                DynamicAlertTicker(alerts = realAlerts)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ২. ডাইনামিক ফিন্যান্সিয়াল সামারি কার্ডস এবং ব্লার স্লাইডিং বাটন
            FinancialSummarySection(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                realReceivable = realReceivable,
                realPayable = realPayable
            )

            // 🔥 স্পেসারটি এখান থেকে ডিলিট করা হয়েছে!

            if (allTransactions.isNotEmpty()) {
                QuickShortcutsSection(navController = navController)
            }

            Spacer(modifier = Modifier.height(32.dp))
            TopExpensesSection(filteredTransactions)
            Spacer(modifier = Modifier.height(40.dp))
            RecentTransactionsSection(filteredTransactions)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ১. ডেটা ক্লাস
data class SummaryCardItem(val title: String, val amount: Float, val color: Color)

// ২. মূল সামারি সেকশন কম্পোজেবল
@Composable
fun FinancialSummarySection(
    totalIncome: Float,
    totalExpense: Float,
    balance: Float,
    realReceivable: Float,
    realPayable: Float
) {
    var expanded by remember { mutableStateOf(false) }

    val summaryItems = listOf(
        SummaryCardItem("মোট আয়", totalIncome, Color(0xFFFFCA28)),
        SummaryCardItem("মোট ব্যয়", totalExpense, Color(0xFFFF7043)),
        SummaryCardItem("বর্তমান ব্যালেন্স", balance, Color(0xFF26C6DA)),
        SummaryCardItem("মোট পাওনা", realReceivable, Color(0xFF10B981)),
        SummaryCardItem("মোট ঋণ", realPayable, Color(0xFFF43F5E))
    )

    val columns = 3
    val rows = summaryItems.chunked(columns)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize( // 🔥 smooth push animation
                animationSpec = spring(dampingRatio = 0.85f)
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔹 First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows[0].forEach {
                SummaryMiniCard(
                    title = it.title,
                    amount = it.amount,
                    color = it.color,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(columns - rows[0].size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (rows.size > 1) {

            val blur by animateDpAsState(
                targetValue = if (expanded) 0.dp else 4.dp,
                label = "blur"
            )

            val alphaValue by animateFloatAsState(
                targetValue = if (expanded) 1f else 0.6f,
                label = "alpha"
            )

            // 🔥 Layout Fixed: Box alignment instead of hardcoded offset
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter // বাটনটি সবসময় নিচে ফিক্সড থাকবে
            ) {

                // Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (expanded) Dp.Unspecified else 85.dp) // সঙ্কুচিত অবস্থায় সুন্দর একটি হাইট
                        .clipToBounds()
                ) {

                    Column(
                        modifier = Modifier
                            .graphicsLayer { alpha = alphaValue }
                            .blur(blur)
                            // এক্সপ্যান্ড হলে বাটনের জন্য জায়গা তৈরি করতে নিচে padding দেওয়া হলো
                            .padding(bottom = if (expanded) 48.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.drop(1).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach {
                                    SummaryMiniCard(
                                        title = it.title,
                                        amount = it.amount,
                                        color = it.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 🔹 Fade gradient
                    if (!expanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0xFFF8FAFC) // ব্যাকগ্রাউন্ডের সাথে মেলানো
                                        )
                                    )
                                )
                        )
                    }
                }

                // 🔥 Floating Button (Standard Flow)
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(50)
                        )
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {

                    AnimatedContent(
                        targetState = expanded,
                        transitionSpec = {
                            fadeIn(tween(200)) + slideInVertically { it / 2 } togetherWith
                                    fadeOut(tween(200)) + slideOutVertically { -it / 2 }
                        },
                        label = "btn_anim"
                    ) { state ->

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Text(
                                text = if (state) "বন্ধ করুন" else "আরো দেখুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = if (state)
                                    Icons.Rounded.KeyboardArrowUp
                                else
                                    Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ৩. গ্লোয়িং শ্যাডো সহ মিনি কার্ড
@Composable
fun SummaryMiniCard(
    title: String,
    amount: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // গ্লোয়িং শ্যাডো ইফেক্ট
            .drawBehind {
                val shadowColor = color.copy(alpha = 0.4f).toArgb() // গ্লো এর অস্বচ্ছতা (Alpha)

                val paint = Paint().asFrameworkPaint().apply {
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        16f, // ব্লার রেডিয়াস (যত বাড়াবেন, গ্লো তত ছড়াবে)
                        0f, // X অক্ষ বরাবর সরণ
                        8f, // Y অক্ষ বরাবর সরণ (নিচের দিকে শ্যাডো নামবে)
                        shadowColor
                    )
                }

                drawContext.canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    12.dp.toPx(), // আপনার কার্ডের কর্নার রেডিয়াসের সাথে মিলিয়ে দিন
                    12.dp.toPx(),
                    paint
                )
            }
            // কার্ডের নিজস্ব স্টাইল
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "৳ $amount",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilter(selectedOption: String, onOptionSelected: (String) -> Unit) {
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
            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray,
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
                            color = if (selectedOption == selectionOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
fun SectionTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TopExpensesSection(transactions: List<Transaction>) {
    val totalExpense = transactions.filter { tx: Transaction -> tx.type == "Expense" }.map { it.amount }.sum().toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "শীর্ষ ৫ খরচের খাত")
        Spacer(modifier = Modifier.height(24.dp))

        val topExpenses = transactions
            .filter { tx: Transaction -> tx.type == "Expense" }
            .groupBy { tx: Transaction -> tx.category }
            .map { (category, list) -> Pair(category, list.map { it.amount }.sum().toFloat()) }
            .sortedByDescending { it.second }
            .take(5)

        val barColors = listOf(
            Color(0xFFF44336),
            Color(0xFFFF9800),
            Color(0xFF2196F3),
            Color(0xFF9C27B0),
            Color(0xFF4CAF50)
        )

        val placeholderColor = Color(0xFFE0E0E0)
        val maxExpense = if (topExpenses.isNotEmpty()) topExpenses.maxOf { it.second } else 1f

        val paddedExpenses = List(5) { index ->
            if (index < topExpenses.size) {
                val data = topExpenses[index]
                Triple(data.first, data.second, barColors[index])
            } else {
                Triple("-", 0f, placeholderColor)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            paddedExpenses.forEachIndexed { index, (category, amount, color) ->
                val fillPercentage = if (amount > 0) amount / maxExpense else 0f
                val displayPercentage = if (totalExpense > 0) amount / totalExpense else 0f

                CustomVerticalBar(
                    rank = index + 1,
                    categoryName = category,
                    amount = amount,
                    fillPercentage = fillPercentage,
                    displayPercentage = displayPercentage,
                    barColor = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CustomVerticalBar(
    rank: Int,
    categoryName: String,
    amount: Float,
    fillPercentage: Float,
    displayPercentage: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedFill by animateFloatAsState(
        targetValue = if (animationPlayed) fillPercentage else 0f,
        animationSpec = tween(durationMillis = 1000), label = "bar_anim"
    )

    LaunchedEffect(key1 = fillPercentage) { animationPlayed = true }
    val textMeasurer = rememberTextMeasurer()
    val isPlaceholder = amount == 0f

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%02d", rank),
            color = if (isPlaceholder) Color.LightGray else barColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = categoryName,
            color = if (isPlaceholder) Color.LightGray else Color.Gray,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(if (isPlaceholder) 1.dp else 4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val baseH = 45.dp.toPx()
                val maxFillH = h - baseH
                val currentFillH = if (isPlaceholder) maxFillH * 0.05f else maxFillH * animatedFill
                val vDepth = 12.dp.toPx()

                drawRect(
                    color = Color(0xFFF0F2F5),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h - baseH)
                )

                val topY = h - baseH - currentFillH
                val path = Path().apply {
                    moveTo(0f, topY)
                    lineTo(w, topY)
                    lineTo(w, h - baseH)
                    lineTo(w / 2f, h - baseH + vDepth)
                    lineTo(0f, h - baseH)
                    close()
                }
                drawPath(path, barColor)

                if (animatedFill > 0.05f && !isPlaceholder) {
                    val pctStr = "${(displayPercentage * 100).roundToInt()}%"
                    val pctText = textMeasurer.measure(
                        text = pctStr,
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    val textX = (w - pctText.size.width) / 2f
                    var textY = topY + 12.dp.toPx()

                    if (textY + pctText.size.height > h - baseH) {
                        textY = h - baseH - pctText.size.height
                    }
                    drawText(pctText, topLeft = Offset(textX, textY))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaceholder) "-" else "৳${amount.toInt()}",
                    color = if (isPlaceholder) Color.LightGray else Color.DarkGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(transactions: List<Transaction>) {
    var selectedFilter by remember { mutableStateOf("সব") }
    val filterOptions = listOf("সব", "আয়", "ব্যয়", "দেনা-পাওনা")

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "সাম্প্রতিক লেনদেন")
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            filterOptions.forEach { option ->
                val isSelected = selectedFilter == option
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { selectedFilter = option }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val filteredTransactions = transactions.filter { tx: Transaction ->
            when (selectedFilter) {
                "আয়" -> tx.type == "Income"
                "ব্যয়" -> tx.type == "Expense"
                "দেনা-পাওনা" -> tx.type == "Lending" || tx.type == "Borrowing"
                else -> true
            }
        }.sortedByDescending { it.date }.take(10)

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "এই সময়ে কোনো লেনদেন হয়নি।",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.animateContentSize().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // কার্ডগুলোর মাঝের দূরত্ব কমানো হয়েছে (10dp থেকে 8dp)
            ) {
                filteredTransactions.forEach { transaction ->
                    key(transaction.id) {
                        RecentTransactionCard(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionCard(transaction: Transaction) {

    val (themeColor, amountPrefix) = when (transaction.type) {
        "Income" -> Pair(Color(0xFF10B981), "+")
        "Expense" -> Pair(Color(0xFFF43F5E), "-")
        "Borrowing" -> Pair(Color(0xFFF59E0B), "+")
        "Lending" -> Pair(Color(0xFF3B82F6), "-")
        else -> Pair(Color.Gray, "")
    }

    val dateString = SimpleDateFormat(
        "dd MMM, yyyy  •  hh:mm a",
        Locale("bn", "BD")
    ).format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
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
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

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

// --- ডাইনামিক এলার্ট টিকার কম্পোনেন্ট ---
enum class AlertType { UPCOMING, TODAY, OVERDUE }
data class AlertItem(val message: String, val type: AlertType)

@Composable
fun DynamicAlertTicker(alerts: List<AlertItem>) {
    if (alerts.isEmpty()) return

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(alerts) {
        while (true) {
            delay(4000) // প্রতি ৪ সেকেন্ড পর পর স্লাইড করবে
            if (alerts.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % alerts.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp)) // হালকা হলুদ ব্যাকগ্রাউন্ড
            .clickable { /* ভবিষ্যতে আপডেটের পপ-আপ আসবে */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                }, label = "ticker_anim"
            ) { targetIndex ->
                if (alerts.isNotEmpty() && targetIndex < alerts.size) {
                    val alert = alerts[targetIndex]
                    val color = when (alert.type) {
                        AlertType.UPCOMING -> Color.DarkGray
                        AlertType.TODAY -> Color(0xFFFF9800)
                        AlertType.OVERDUE -> Color.Red
                    }
                    Text(
                        text = alert.message,
                        fontSize = 13.sp,
                        fontWeight = if (alert.type == AlertType.OVERDUE) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ডেটা ক্লাস: মেনু আইটেমগুলোকে সাজিয়ে রাখার জন্য
data class GridShortcut(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: androidx.compose.ui.graphics.Color, val route: String)

// --- নতুন করে সাজানো এক্সপ্যান্ডেবল ৪-কলামের শর্টকাট সেকশন ---
@Composable
fun QuickShortcutsSection(navController: NavController) {

    val context = LocalContext.current
    var showMoreShortcuts by remember { mutableStateOf(false) }

    val shortcuts = listOf(
        GridShortcut("লেনদেন", Icons.Rounded.ReceiptLong, MaterialTheme.colorScheme.primary, "transactions"),
        GridShortcut("আয় যোগ", Icons.Rounded.Add, Color(0xFF10B981), "add_income"),
        GridShortcut("ব্যয় যোগ", Icons.Rounded.Add, Color(0xFFF43F5E), "add_expense"),
        GridShortcut("আয়-ব্যয়", Icons.Rounded.AccountBalanceWallet, Color(0xFF3B82F6), "income_expense"),
        GridShortcut("দেনা-পাওনা", Icons.Rounded.Handshake, Color(0xFF9C27B0), "debt_credit")
    )

    val columns = 4
    val chunkedShortcuts = shortcuts.chunked(columns)

    val totalRows = chunkedShortcuts.size
    val shouldShowExpand = totalRows > 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔥 Top Smooth Blend Effect (আগের সেকশনের সাথে স্মুথলি মিশে যাওয়ার জন্য)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background, // একদম সলিড ব্যাকগ্রাউন্ড থেকে শুরু
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f), // হালকা ব্লেন্ড
                            Color.Transparent // পুরোপুরি স্বচ্ছ হয়ে শর্টকাটগুলো দেখাবে
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (!shouldShowExpand || showMoreShortcuts)
                                Dp.Unspecified
                            else
                                200.dp
                        )
                        .clipToBounds()
                ) {

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        val visibleRows = when {
                            !shouldShowExpand -> chunkedShortcuts
                            showMoreShortcuts -> chunkedShortcuts
                            else -> chunkedShortcuts.take(2)
                        }

                        visibleRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    GridShortcutCard(
                                        item = item,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            navigateToShortcut(item, navController, context)
                                        }
                                    )
                                }

                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 🔹 Bottom Fade gradient (Smooth Blend with Theme Color)
                    if (shouldShowExpand && !showMoreShortcuts) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.background // 🔥 হার্ডকোড কালারের বদলে ডাইনামিক থিম কালার
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // 🔥 NEW SMOOTH "আরো দেখুন"
            androidx.compose.animation.AnimatedVisibility(
                visible = shouldShowExpand && !showMoreShortcuts,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 300f
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.9f)
                ) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(50)
                        )
                        .clickable { showMoreShortcuts = true }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "আরো দেখুন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 🔥 NEW SMOOTH "বন্ধ করুন"
        androidx.compose.animation.AnimatedVisibility(
            visible = shouldShowExpand && showMoreShortcuts,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 250f
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.9f)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { showMoreShortcuts = false }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "বন্ধ করুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// নেভিগেশন হ্যান্ডেল করার একটি ছোট হেল্পার ফাংশন
fun navigateToShortcut(item: GridShortcut, navController: NavController, context: android.content.Context) {
    try {
        if (item.route == "transactions") {
            navController.navigate(item.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(item.route)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Routing Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- ৪ কলামের জন্য পারফেক্ট মিনিমাল কার্ড ডিজাইন ---
@Composable
fun GridShortcutCard(item: GridShortcut, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(95.dp) // সব কার্ডের সমান উচ্চতা
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = item.color.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp), // কম্প্যাক্ট প্যাডিং
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                fontSize = 11.sp, // ৪টি কার্ড এক লাইনে বসানোর জন্য ছোট ফন্ট
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// এই ShortcutCard টি শুধুমাত্র খালি অবস্থার (Empty State) জন্য রাখা হলো
@Composable
fun ShortcutCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }
}

data class SliceData(val name: String, val value: Float, val percentage: Float, val color: Color)

@Composable
fun CustomUnevenPieChart(income: Float, expense: Float, balance: Float) {
    val totalVolume = income + expense + if (balance > 0) balance else 0f

    if (totalVolume == 0f) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("এই সময়ে কোনো হিসাব নেই", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        return
    }

    val incPct = (income / totalVolume) * 100
    val expPct = (expense / totalVolume) * 100
    val balPct = if (balance > 0) (balance / totalVolume) * 100 else 0f

    val slices = listOf(
        SliceData("আয়", income, incPct, Color(0xFFFFCA28)),
        SliceData("খরচ", expense, expPct, Color(0xFFFF7043)),
        SliceData("আছে", if (balance > 0) balance else 0f, balPct, Color(0xFF26C6DA))
    ).filter { it.value > 0f }

    val sortedSlices = slices.sortedByDescending { it.value }

    val textMeasurer = rememberTextMeasurer()
    var animationPlayed by remember { mutableStateOf(false) }

    val animatableSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(1200),
        label = "pie_anim"
    )

    LaunchedEffect(key1 = totalVolume) { animationPlayed = true }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasCenter = center
        val maxRadius = size.minDimension / 2.8f

        var startAngle = -90f

        // গ্লো ইফেক্টের জন্য Paint অবজেক্ট তৈরি করা হচ্ছে
        val paintWithGlow = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        drawIntoCanvas { canvas ->
            slices.forEach { slice ->
                val sweepAngle = (slice.value / totalVolume) * animatableSweep

                val rank = sortedSlices.indexOf(slice)
                val radiusMultiplier = when(rank) {
                    0 -> 1.0f
                    1 -> 0.88f
                    else -> 0.78f
                }
                val sliceRadius = maxRadius * radiusMultiplier

                // গ্লো শ্যাডোর রঙ নির্ধারণ (স্লাইসের রঙের কিছুটা স্বচ্ছ সংস্করণ)
                val shadowColorArgb = slice.color.copy(alpha = 0.5f).toArgb()

                // নেটিভ Paint-এ শ্যাডো লেয়ার সেট করা হচ্ছে
                // parameters: blurRadius, offsetX, offsetY, shadowColor
                paintWithGlow.setShadowLayer(
                    30f, // গ্লো কতটা ছড়াবে (blur)
                    0f,  // X অক্ষের দিকে সরবে না
                    0f,  // Y অক্ষের দিকে সরবে না (চারপাশে সমানভাবে ছড়াবে)
                    shadowColorArgb
                )
                paintWithGlow.color = slice.color.toArgb()

                // আর্কের জন্য রেক্টেঙ্গেল নির্ধারণ
                val rect = Rect(
                    Offset(canvasCenter.x - sliceRadius, canvasCenter.y - sliceRadius),
                    Size(sliceRadius * 2, sliceRadius * 2)
                )

                // নেটিভ ক্যানভাসে গ্লো শ্যাডো সহ আর্ক আঁকা হচ্ছে
                canvas.nativeCanvas.drawArc(
                    rect.left, rect.top, rect.right, rect.bottom,
                    startAngle, sweepAngle,
                    true, // useCenter
                    paintWithGlow
                )

                // লেবেল এবং লাইন আঁকার লজিক (আগের মতোই রাখা হয়েছে)
                if (animatableSweep == 360f && slice.percentage > 0f) {
                    val midAngle = (startAngle + sweepAngle / 2) * (Math.PI / 180f)

                    val lineStartRadius = sliceRadius * 0.85f
                    val extension = when {
                        slice.percentage < 5f -> 45f
                        slice.percentage < 15f -> 30f
                        else -> 15f
                    }

                    val lineEndRadius = sliceRadius + extension

                    val startX = canvasCenter.x + (cos(midAngle) * lineStartRadius).toFloat()
                    val startY = canvasCenter.y + (sin(midAngle) * lineStartRadius).toFloat()

                    val endX = canvasCenter.x + (cos(midAngle) * lineEndRadius).toFloat()
                    val endY = canvasCenter.y + (sin(midAngle) * lineEndRadius).toFloat()

                    val isRightSide = cos(midAngle) >= 0
                    val elbowLength = 15f
                    val elbowX = endX + if (isRightSide) elbowLength else -elbowLength

                    // লাইনগুলো স্লাইসের রঙে আঁকা হচ্ছে
                    drawLine(color = slice.color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3f)
                    drawLine(color = slice.color, start = Offset(endX, endY), end = Offset(elbowX, endY), strokeWidth = 3f)

                    val labelText = textMeasurer.measure(
                        text = slice.name,
                        style = TextStyle(color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    )

                    val textX = if (isRightSide) elbowX + 4f else elbowX - labelText.size.width - 4f
                    val textY = endY - labelText.size.height / 2f

                    // টেক্সট লেবেল আঁকা হচ্ছে
                    drawText(textLayoutResult = labelText, topLeft = Offset(textX, textY))
                }

                startAngle += sweepAngle
            }
        }
    }
}