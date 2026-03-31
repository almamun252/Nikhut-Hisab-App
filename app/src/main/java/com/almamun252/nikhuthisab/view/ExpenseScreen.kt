package com.almamun252.nikhuthisab.view

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    // ডেটাবেস থেকে রিয়েল-টাইম ডেটা আনা
    val allTransactions by viewModel.allTransactions.collectAsState()

    // শুধু ব্যয়ের ডেটা ফিল্টার করা এবং লেটেস্ট ডেটা আগে দেখানো
    val expenses = allTransactions.filter {
        it.type == "Expense" && (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
    }.sortedByDescending { it.date }

    // মোট ব্যয় হিসাব করা
    val totalExpense = expenses.sumOf { it.amount.toDouble() }

    // থিম কালার (লাল - ব্যয়ের জন্য)
    val themeColor = Color(0xFFF44336)
    val lightThemeColor = Color(0xFFFFEBEE)
    val darkThemeColor = Color(0xFFC62828)

    // Bottom Sheet এর জন্য স্টেট
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ব্যয় ট্র্যাকিং", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_expense") },
                containerColor = themeColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন ব্যয়", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Total Expense Summary Card
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
                        text = "মোট ব্যয়",
                        color = darkThemeColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${totalExpense.toInt()}",
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
                placeholder = { Text("খরচ খুঁজুন (নাম বা ক্যাটাগরি)...", color = Color.Gray, fontSize = 14.sp) },
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
                Text("সব খরচের তালিকা", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Expenses
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📉", fontSize = 48.sp)
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
                    items(expenses) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            themeColor = themeColor,
                            lightThemeColor = lightThemeColor,
                            onClick = { selectedTransaction = expense }
                        )
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
                ExpenseTransactionDetailsSheet(
                    transaction = selectedTransaction!!,
                    themeColor = themeColor,
                    lightThemeColor = lightThemeColor,
                    onEdit = {
                        val id = selectedTransaction!!.id
                        selectedTransaction = null
                        // এডিট স্ক্রিনে পাঠানো হচ্ছে
                        navController.navigate("add_expense?transactionId=$id")
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

@Composable
fun ExpenseItemCard(expense: Transaction, themeColor: Color, lightThemeColor: Color, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val dateString = sdf.format(Date(expense.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = expense.category.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${expense.category} • $dateString",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Amount
            Text(
                text = "- ৳${expense.amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor
            )
        }
    }
}

// Bottom Sheet UI কম্পোনেন্ট - নাম ইউনিক করা হয়েছে
@Composable
fun ExpenseTransactionDetailsSheet(
    transaction: Transaction,
    themeColor: Color,
    lightThemeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD")).format(Date(transaction.date))
    val timeString = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(transaction.date))

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
            text = "- ৳${transaction.amount.toInt()}",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themeColor
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))

        // Details list
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExpenseDetailRow(icon = Icons.Rounded.Category, label = "ক্যাটাগরি", value = transaction.category, iconTint = themeColor)
            ExpenseDetailRow(icon = Icons.Rounded.CalendarToday, label = "তারিখ", value = dateString, iconTint = themeColor)
            ExpenseDetailRow(icon = Icons.Rounded.Schedule, label = "সময়", value = timeString, iconTint = themeColor)

            // নোট ফিল্ড
            if (!transaction.note.isNullOrBlank()) {
                ExpenseDetailRow(icon = Icons.Rounded.Info, label = "নোট", value = transaction.note, iconTint = themeColor)
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

// নাম ইউনিক করা হয়েছে
@Composable
fun ExpenseDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconTint: Color) {
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