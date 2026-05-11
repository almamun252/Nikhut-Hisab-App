package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.RoughTransaction
import com.almamun252.nikhuthisab.viewmodel.RoughViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoughKhataScreen(
    navController: NavController,
    roughViewModel: RoughViewModel = viewModel()
) {
    val context = LocalContext.current
    val roughTransactions by roughViewModel.allRoughTransactions.collectAsState(initial = emptyList<RoughTransaction>())

    // UI States
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var transferConfirmDialogItem by remember { mutableStateOf<RoughTransaction?>(null) }

    // Form States
    var inputTitle by remember { mutableStateOf("") }
    var inputAmount by remember { mutableStateOf("") }
    var inputNote by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val catOtherStr = stringResource(R.string.cat_other)

    val availableCategories = listOf(
        stringResource(R.string.cat_food),
        stringResource(R.string.cat_transport),
        stringResource(R.string.cat_rent),
        stringResource(R.string.cat_shopping),
        stringResource(R.string.cat_bills),
        stringResource(R.string.cat_health),
        stringResource(R.string.cat_education),
        catOtherStr
    )

    val totalAmount = roughTransactions.sumOf { it.amount }
    val themeColor = Color(0xFF6366F1) // Indigo/Purple theme for Rough Khata
    val lightThemeColor = Color(0xFFEEF2FF)

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(tween(600, delayMillis = 300)) { 150 } + fadeIn(tween(600)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        inputTitle = ""
                        inputAmount = ""
                        inputNote = ""
                        selectedCategory = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.padding(bottom = 90.dp),
                    containerColor = themeColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_new_rough_khata), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header Section ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Text(stringResource(R.string.title_rough_khata), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                            Text(stringResource(R.string.subtitle_rough_khata), fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (roughTransactions.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.background(Color(0xFFFEF2F2), CircleShape)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.desc_clear_all), tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // --- Total Summary Card ---
            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(600))) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = themeColor.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = themeColor),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.label_rough_total_expense), color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("৳ ${totalAmount.toInt()}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // --- List View ---
            if (roughTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(lightThemeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Draw, contentDescription = null, tint = themeColor.copy(alpha = 0.6f), modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(stringResource(R.string.msg_rough_empty), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.msg_rough_empty_desc), fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(roughTransactions, key = { it.id }) { tx ->
                        RoughTransactionCard(
                            transaction = tx,
                            themeColor = themeColor,
                            onMoveClick = { transferConfirmDialogItem = tx },
                            onDeleteClick = { roughViewModel.deleteRough(tx.id) }
                        )
                    }
                }
            }
        }

        // --- Add Dialog ---
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color.White,
                title = { Text(stringResource(R.string.btn_new_rough_khata), fontWeight = FontWeight.Bold) },
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
                                label = { Text(stringResource(R.string.label_category)) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColor,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
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

                        RoughKhataFloatingTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = stringResource(R.string.label_rough_name_hint),
                            themeColor = themeColor,
                            labelBackgroundColor = Color.White
                        )

                        RoughKhataFloatingTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = stringResource(R.string.label_amount_only),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = themeColor, modifier = Modifier.padding(start = 12.dp)) },
                            themeColor = themeColor,
                            labelBackgroundColor = Color.White
                        )

                        RoughKhataFloatingTextField(
                            value = inputNote,
                            onValueChange = { inputNote = it },
                            label = stringResource(R.string.label_detailed_note_optional),
                            themeColor = themeColor,
                            labelBackgroundColor = Color.White
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = inputAmount.toDoubleOrNull()
                            if (amount != null && amount > 0 && inputTitle.isNotBlank()) {
                                roughViewModel.insertRough(
                                    RoughTransaction(
                                        title = inputTitle.trim(),
                                        amount = amount,
                                        category = selectedCategory.ifEmpty { catOtherStr },
                                        note = inputNote.trim(),
                                        date = System.currentTimeMillis()
                                    )
                                )
                                showAddDialog = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.msg_enter_valid_info), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.btn_add), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // --- Transfer Confirmation Dialog ---
        if (transferConfirmDialogItem != null) {
            AlertDialog(
                onDismissRequest = { transferConfirmDialogItem = null },
                containerColor = Color.White,
                icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = themeColor, modifier = Modifier.size(40.dp)) },
                title = { Text(stringResource(R.string.title_move_to_main_confirm), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.msg_move_to_main_desc, transferConfirmDialogItem?.category ?: "")) },
                confirmButton = {
                    Button(
                        onClick = {
                            roughViewModel.moveToMain(transferConfirmDialogItem!!)
                            transferConfirmDialogItem = null
                            Toast.makeText(context, context.getString(R.string.msg_moved_to_main_success), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) { Text(stringResource(R.string.btn_yes_add), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { transferConfirmDialogItem = null }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // --- Clear All Confirmation Dialog ---
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = Color.White,
                title = { Text(stringResource(R.string.title_clear_all_rough), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.msg_clear_all_rough_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            roughViewModel.clearAll()
                            showClearDialog = false
                            Toast.makeText(context, context.getString(R.string.msg_all_deleted_successfully), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text(stringResource(R.string.btn_yes_delete), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun RoughTransactionCard(
    transaction: RoughTransaction,
    themeColor: Color,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val isMoved = transaction.isMovedToMain == 1

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                // Left Side (Icon, Title, Category & Note)
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Assuming getCategoryIcon is imported or globally available
                        Icon(getCategoryIcon(transaction.category), contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = transaction.category, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text(text = " • $dateString", fontSize = 11.sp, color = Color.LightGray)
                        }

                        // নোট থাকলে দেখাবে
                        if (transaction.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_note_prefix, transaction.note),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right Side (Amount & Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "৳${transaction.amount.toInt()}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.desc_delete), tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Magic Transfer / Status Badge
            if (isMoved) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFECFDF5)).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.status_moved_to_main), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onMoveClick,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = "Move", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_move_to_main), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// --- Custom Always Floating Label Component ---
@Composable
private fun RoughKhataFloatingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    themeColor: Color,
    labelBackgroundColor: Color = Color.White
) {
    Box(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!singleLine) Modifier.heightIn(min = 120.dp) else Modifier),
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            textStyle = textStyle
        )

        // Custom always-floating label text placed exactly over the border line
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp)
                .offset(y = (-8).dp)
                .background(labelBackgroundColor) // Background color to match exactly with screen or dialog
                .padding(horizontal = 4.dp)
        )
    }
}