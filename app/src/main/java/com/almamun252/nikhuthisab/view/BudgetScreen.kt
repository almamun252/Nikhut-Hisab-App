package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.Budget
import com.almamun252.nikhuthisab.viewmodel.BudgetViewModel
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel() // খরচের হিসাব আনার জন্য
) {
    val context = LocalContext.current

    // বর্তমান মাস বের করা (যেমন: "05-2026")
    val cal = Calendar.getInstance()
    val currentMonthStr = SimpleDateFormat("MM-yyyy", Locale.US).format(cal.time)
    // Locale.getDefault() ব্যবহার করা হয়েছে যাতে ভাষা পরিবর্তনের সাথে সাথে মাসের নাম বদলে যায়
    val displayMonthStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

    // ডাটাবেস থেকে ডেটা আনা
    val budgets by budgetViewModel.getBudgetsForMonth(currentMonthStr).collectAsState(initial = emptyList())
    val allTransactions by transactionViewModel.allTransactions.collectAsState()

    // এই মাসের সবগুলো খরচ ফিল্টার করা
    val thisMonthExpenses = remember(allTransactions) {
        allTransactions.filter { tx ->
            tx.type == "Expense" &&
                    SimpleDateFormat("MM-yyyy", Locale.US).format(Date(tx.date)) == currentMonthStr
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Dialog States
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedBudgetToEdit by remember { mutableStateOf<Budget?>(null) }

    // Form States
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    // ক্যাটাগরিগুলো ডাইনামিক করা হলো
    val availableCategories = listOf(
        stringResource(R.string.cat_food),
        stringResource(R.string.cat_transport),
        stringResource(R.string.cat_rent),
        stringResource(R.string.cat_shopping),
        stringResource(R.string.cat_bills),
        stringResource(R.string.cat_health),
        stringResource(R.string.cat_education),
        stringResource(R.string.cat_other)
    )

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(tween(600, delayMillis = 300)) { 150 } + fadeIn(tween(600)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedBudgetToEdit = null
                        amountInput = ""
                        selectedCategory = ""
                        showAddBudgetDialog = true
                    },
                    modifier = Modifier.padding(bottom = 90.dp),
                    containerColor = Color(0xFF4F46E5), // Indigo Theme
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_new_budget), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header ---
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
                    Column {
                        Text(stringResource(R.string.title_monthly_budget), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Text(displayMonthStr, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // --- List View ---
            if (budgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF4F46E5).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Category, contentDescription = null, tint = Color(0xFF4F46E5).copy(alpha = 0.6f), modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(stringResource(R.string.msg_no_budget_this_month), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.desc_set_budget_to_control), fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        // এই ক্যাটাগরিতে এই মাসে কত খরচ হয়েছে তা বের করা
                        val spentAmount = thisMonthExpenses
                            .filter { it.category == budget.category }
                            .sumOf { it.amount }

                        BudgetProgressCard(
                            budget = budget,
                            spentAmount = spentAmount,
                            onEditClick = {
                                selectedBudgetToEdit = budget
                                amountInput = if (budget.limitAmount % 1.0 == 0.0) budget.limitAmount.toInt().toString() else budget.limitAmount.toString()
                                selectedCategory = budget.category
                                showAddBudgetDialog = true
                            },
                            onDeleteClick = {
                                budgetViewModel.deleteBudget(budget.id)
                                Toast.makeText(context, context.getString(R.string.msg_budget_deleted), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Add/Edit Budget Dialog ---
    if (showAddBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (selectedBudgetToEdit == null) stringResource(R.string.title_set_new_budget) else stringResource(R.string.title_update_budget),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_select_category)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4F46E5))
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(stringResource(R.string.label_budget_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5), modifier = Modifier.padding(start = 12.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4F46E5))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0 && selectedCategory.isNotEmpty()) {
                            // চেক করা যে এই ক্যাটাগরিতে অলরেডি বাজেট আছে কি না
                            val exists = budgets.find { it.category == selectedCategory }
                            if (exists != null && selectedBudgetToEdit == null) {
                                Toast.makeText(context, context.getString(R.string.msg_budget_exists), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newBudget = Budget(
                                id = selectedBudgetToEdit?.id ?: 0,
                                category = selectedCategory,
                                limitAmount = amount,
                                monthYear = currentMonthStr
                            )
                            budgetViewModel.insertBudget(newBudget)
                            showAddBudgetDialog = false
                            Toast.makeText(context, context.getString(R.string.msg_budget_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_enter_valid_info), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.Gray)
                }
            }
        )
    }
}

// --- Traffic Light Progress Card ---
@Composable
fun BudgetProgressCard(
    budget: Budget,
    spentAmount: Double,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // ডাইনামিক স্ট্রিং ভ্যারিয়েবলগুলো নেয়া হচ্ছে
    val statusWarning = stringResource(R.string.status_budget_warning)
    val statusCareful = stringResource(R.string.status_spend_carefully)
    val statusSafe = stringResource(R.string.status_budget_safe)

    // Traffic Light Logic 🚦
    val progressRatio = if (budget.limitAmount > 0) (spentAmount / budget.limitAmount).toFloat() else 0f
    val safeRatio = progressRatio.coerceIn(0f, 1f) // 1 এর বেশি যেন না যায় প্রগ্রেস বারের জন্য

    // কালার ডিসিশন
    val (progressColor, lightColor, statusText) = when {
        progressRatio >= 0.8f -> Triple(Color(0xFFF43F5E), Color(0xFFFFF1F2), statusWarning) // লাল (৮০% এর ওপর)
        progressRatio >= 0.5f -> Triple(Color(0xFFF59E0B), Color(0xFFFFFBEB), statusCareful) // হলুদ (৫০% থেকে ৮০%)
        else -> Triple(Color(0xFF10B981), Color(0xFFECFDF5), statusSafe) // সবুজ (০% থেকে ৫০%)
    }

    val animProgress by animateFloatAsState(targetValue = safeRatio, animationSpec = tween(1200), label = "progress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = progressColor.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // Top Header: Icon, Category & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(lightColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getCategoryIcon(budget.category), contentDescription = null, tint = progressColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(budget.category, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = progressColor)
                    }
                }

                // Edit & Delete Menu
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Rounded.Warning, contentDescription = stringResource(R.string.desc_options), tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White).clip(RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Row{ Icon(Icons.Filled.Edit, null, tint=Color.Gray, modifier=Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_edit)) } },
                            onClick = { expanded = false; onEditClick() }
                        )
                        DropdownMenuItem(
                            text = { Row{ Icon(Icons.Filled.Delete, null, tint=Color.Red, modifier=Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_delete), color=Color.Red) } },
                            onClick = { expanded = false; onDeleteClick() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_spent, spentAmount.toInt()), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Text(stringResource(R.string.label_budget_colon, budget.limitAmount.toInt()), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Traffic Light Progress Bar
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = progressColor,
                trackColor = Color(0xFFF1F5F9),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Remaining Amount or Over Budget alert
            val remaining = budget.limitAmount - spentAmount
            if (remaining < 0) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF1F2)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.msg_over_budget, Math.abs(remaining).toInt()), color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Text(stringResource(R.string.msg_remaining_budget, remaining.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
            }
        }
    }
}