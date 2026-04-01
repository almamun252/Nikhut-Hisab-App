package com.almamun252.nikhuthisab.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: TransactionViewModel = viewModel()) {
    // ডেটাবেস থেকে রিয়েল-টাইম ডেটা আনা হচ্ছে
    val allTransactions by viewModel.allTransactions.collectAsState()

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

    // আসল ফিল্টারিং লজিক (যা ডেটা আপডেট করবে)
    val filteredTransactions = allTransactions.filter { tx ->
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

    // ফিল্টার করা ডেটার উপর ভিত্তি করে আয়, ব্যয় এবং ব্যালেন্স হিসাব করা
    val totalIncome = filteredTransactions.filter { it.type == "Income" }.sumOf { it.amount }.toFloat()
    val totalExpense = filteredTransactions.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()
    val balance = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ড্যাশবোর্ড", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    // ডানপাশে সুন্দর আইকন এবং অ্যাপের নাম
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নিখুঁত হিসাব",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Modern clean look
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 150 },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ডেট রেঞ্জ ড্রপডাউন
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
                                selectedDateFilter = "কাস্টম রেঞ্জ"
                            }) {
                                Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showCustomDateDialog = false
                                customStartDate = null
                                customEndDate = null
                            }) {
                                Text("বাতিল", color = Color.Red)
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

                Spacer(modifier = Modifier.height(24.dp))

                // ১. কাস্টম পাই চার্ট
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomUnevenPieChart(income = totalIncome, expense = totalExpense, balance = balance)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ২. ফিন্যান্সিয়াল সামারি কার্ড
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMiniCard(
                        title = "মোট আয়",
                        amount = totalIncome,
                        color = Color(0xFFFFCA28), // Yellow
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniCard(
                        title = "মোট ব্যয়",
                        amount = totalExpense,
                        color = Color(0xFFFF7043), // Orange/Red
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniCard(
                        title = "ব্যালেন্স",
                        amount = balance,
                        color = Color(0xFF26C6DA), // Teal
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ৩. কুইক শর্টকাট সেকশন
                QuickShortcutsSection(navController = navController)

                Spacer(modifier = Modifier.height(32.dp))

                // ৪. প্রফেশনাল বার চার্ট
                TopExpensesSection(filteredTransactions)

                Spacer(modifier = Modifier.height(40.dp))

                // ৫. সাম্প্রতিক লেনদেন
                RecentTransactionsSection(filteredTransactions)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- শর্টকাট সেকশন ---
@Composable
fun QuickShortcutsSection(navController: NavController) {
    val navigateToTab = { route: String ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShortcutCard(
            title = "লেনদেন",
            icon = Icons.Rounded.ReceiptLong,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { navigateToTab("transactions") }
        )
        ShortcutCard(
            title = "আয় যোগ",
            icon = Icons.Rounded.Add,
            color = Color(0xFF4CAF50), // Green
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("add_income") }
        )
        ShortcutCard(
            title = "ব্যয় যোগ",
            icon = Icons.Rounded.Add,
            color = Color(0xFFF44336), // Red
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("add_expense") }
        )
    }
}

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
        val maxRadius = size.minDimension / 3.2f

        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = (slice.value / totalVolume) * animatableSweep

            val rank = sortedSlices.indexOf(slice)
            val radiusMultiplier = when(rank) {
                0 -> 1.0f
                1 -> 0.88f
                else -> 0.78f
            }
            val sliceRadius = maxRadius * radiusMultiplier

            drawArc(
                color = Color.Black.copy(alpha = 0.08f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(canvasCenter.x - sliceRadius + 8f, canvasCenter.y - sliceRadius + 12f),
                size = Size(sliceRadius * 2, sliceRadius * 2)
            )

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(canvasCenter.x - sliceRadius, canvasCenter.y - sliceRadius),
                size = Size(sliceRadius * 2, sliceRadius * 2)
            )

            if (animatableSweep == 360f && slice.percentage > 0f) {
                val midAngle = (startAngle + sweepAngle / 2) * (Math.PI / 180f)

                val lineStartRadius = sliceRadius * 0.85f

                // ডাইনামিক লাইনের দৈর্ঘ্য: পার্সেন্টেজ কম হলে লাইন লম্বা হবে
                val extension = when {
                    slice.percentage < 5f -> 65f   // খুব কম হলে অনেক লম্বা
                    slice.percentage < 15f -> 45f  // একটু কম হলে মাঝারি লম্বা
                    else -> 25f                    // স্বাভাবিক
                }

                val lineEndRadius = sliceRadius + extension

                val startX = canvasCenter.x + (cos(midAngle) * lineStartRadius).toFloat()
                val startY = canvasCenter.y + (sin(midAngle) * lineStartRadius).toFloat()

                val endX = canvasCenter.x + (cos(midAngle) * lineEndRadius).toFloat()
                val endY = canvasCenter.y + (sin(midAngle) * lineEndRadius).toFloat()

                val isRightSide = cos(midAngle) >= 0
                val elbowLength = 20f // অনুভূমিক (ডান/বাম) লাইনের দৈর্ঘ্য
                val elbowX = endX + if (isRightSide) elbowLength else -elbowLength

                drawLine(color = slice.color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 4f)
                drawLine(color = slice.color, start = Offset(endX, endY), end = Offset(elbowX, endY), strokeWidth = 4f)

                val pctText = textMeasurer.measure(text = "${slice.percentage.roundToInt()}%", style = TextStyle(color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
                val nameText = textMeasurer.measure(text = slice.name, style = TextStyle(color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold))

                val textWidth = maxOf(pctText.size.width, nameText.size.width)

                val textX = if (isRightSide) elbowX + 6f else elbowX - textWidth - 6f
                val textY = endY - (pctText.size.height + nameText.size.height) / 2f

                val pctX = if (isRightSide) textX else textX + textWidth - pctText.size.width
                val nameX = if (isRightSide) textX else textX + textWidth - nameText.size.width

                drawText(textLayoutResult = pctText, topLeft = Offset(pctX, textY))
                drawText(textLayoutResult = nameText, topLeft = Offset(nameX, textY + pctText.size.height))
            }

            startAngle += sweepAngle
        }
    }
}

@Composable
fun SummaryMiniCard(title: String, amount: Float, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f))
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "৳${amount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "শীর্ষ ৫ খরচের খাত")
        Spacer(modifier = Modifier.height(24.dp))

        val topExpenses = transactions
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .map { (category, list) -> Pair(category, list.sumOf { it.amount }.toFloat()) }
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
    val filterOptions = listOf("সব", "আয়", "ব্যয়")

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
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val filteredTransactions = transactions.filter {
            when (selectedFilter) {
                "আয়" -> it.type == "Income"
                "ব্যয়" -> it.type == "Expense"
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
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val isIncome = transaction.type == "Income"
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (isIncome) "+" else "-"
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val dateString = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = transaction.category.take(1).uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${transaction.category} • $dateString", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }

            Text(
                text = "$sign ৳${transaction.amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}