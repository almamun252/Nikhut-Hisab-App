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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.worker.ReminderScheduler
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtCreditScreen(
    navController: NavController,
    type: String, // "Lending" বা "Borrowing"
    transactionId: Int? = null,
    viewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val existingTransaction = transactions.find { it.id == transactionId }

    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var settledAmount by remember { mutableStateOf(0.0) }

    // Dates & Time
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var tempDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var tempDueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }

    val isLending = type == "Lending"
    val themeColor = if (isLending) Color(0xFF10B981) else Color(0xFFF43F5E) // Emerald / Rose
    val lightThemeColor = if (isLending) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val screenBackgroundColor = Color(0xFFF8FAFC) // স্ক্রিনের ব্যাকগ্রাউন্ড কালার

    // Dynamic Strings using stringResource
    val screenTitle = if (transactionId != null) {
        if (isLending) stringResource(R.string.title_update_lending) else stringResource(R.string.title_update_borrowing)
    } else {
        if (isLending) stringResource(R.string.title_add_lending) else stringResource(R.string.title_add_borrowing)
    }

    val nameLabel = if (isLending) stringResource(R.string.label_lending_to) else stringResource(R.string.label_borrowing_from)

    LaunchedEffect(existingTransaction) {
        existingTransaction?.let {
            amount = if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
            title = it.title
            selectedDateMillis = it.date
            dueDateMillis = it.dueDate
            settledAmount = it.settledAmount
            note = it.note ?: ""
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBackgroundColor)) {
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
                // --- Top Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = stringResource(R.string.desc_back), tint = Color(0xFF334155))
                    }
                    Text(text = screenTitle, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.size(44.dp))
                }

                // --- Amount Input Field ---
                DebtCreditFloatingTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(R.string.label_amount_required),
                    leadingIcon = {
                        Box(
                            modifier = Modifier.padding(start = 12.dp).size(36.dp).clip(CircleShape).background(lightThemeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("৳", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = themeColor)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = themeColor),
                    shape = RoundedCornerShape(20.dp),
                    themeColor = themeColor,
                    labelBackgroundColor = screenBackgroundColor
                )

                // --- Form Fields ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Person Name
                    DebtCreditFloatingTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = nameLabel,
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = themeColor) },
                        themeColor = themeColor,
                        labelBackgroundColor = screenBackgroundColor
                    )

                    // Transaction Date & Time (Standard OutlinedTextField with transparent bg)
                    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale.getDefault())

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sdf.format(Date(selectedDateMillis)),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_date_time_given)) },
                            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Box(
                            modifier = Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDatePicker = true }
                        )
                    }

                    // Due Date (Deadline) - (Standard OutlinedTextField with transparent bg)
                    val dueSdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale.getDefault())
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dueDateMillis?.let { dueSdf.format(Date(it)) } ?: stringResource(R.string.label_set_deadline_optional),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_when_repay)) },
                            leadingIcon = { Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = if (dueDateMillis != null) themeColor else Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Box(
                            modifier = Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDueDatePicker = true }
                        )
                        if (dueDateMillis != null) {
                            IconButton(
                                onClick = { dueDateMillis = null },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.desc_clear), tint = Color.Gray)
                            }
                        }
                    }

                    // Note Field
                    DebtCreditFloatingTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(R.string.label_detailed_note_optional),
                        leadingIcon = { Icon(Icons.Rounded.Subject, contentDescription = null, tint = themeColor) },
                        singleLine = false,
                        themeColor = themeColor,
                        labelBackgroundColor = screenBackgroundColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        val finalTitle = title.trim()

                        if (amountValue == null || amountValue <= 0) {
                            Toast.makeText(context, context.getString(R.string.msg_valid_amount_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (finalTitle.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.msg_person_name_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val transaction = Transaction(
                            id = existingTransaction?.id ?: 0,
                            title = finalTitle,
                            amount = amountValue,
                            type = type, // Lending or Borrowing
                            category = if (isLending) context.getString(R.string.cat_lent) else context.getString(R.string.cat_borrowed),
                            date = selectedDateMillis,
                            note = note.trim().ifEmpty { null },
                            dueDate = dueDateMillis,
                            settledAmount = settledAmount // এডিট করার সময় আগেরটা ঠিক থাকবে
                        )

                        if (transactionId != null && existingTransaction != null) {
                            viewModel.deleteTransaction(existingTransaction)
                            viewModel.insertTransaction(transaction)
                            Toast.makeText(context, context.getString(R.string.msg_updated_successfully), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.insertTransaction(transaction)
                            Toast.makeText(context, context.getString(R.string.msg_saved_successfully), Toast.LENGTH_SHORT).show()
                        }

                        // --- নোটিফিকেশন শিডিউল বা ক্যানসেল করা ---
                        val reminderId = if (transaction.id != 0) transaction.id else transaction.hashCode()

                        if (dueDateMillis != null) {
                            ReminderScheduler.scheduleReminder(
                                context = context,
                                transactionId = reminderId,
                                title = finalTitle,
                                amount = amountValue.toFloat(),
                                type = type,
                                dueDate = dueDateMillis!!
                            )
                        } else {
                            ReminderScheduler.cancelReminder(context, reminderId)
                        }

                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = if (transactionId != null) stringResource(R.string.btn_update) else stringResource(R.string.btn_save),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // --- Date Picker (সময় সহ) ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { tempDateMillis = it }
                    showDatePicker = false
                    showTimePicker = true // তারিখের পর সময় সিলেক্ট হবে
                }) { Text(stringResource(R.string.btn_next_time), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Red) } }
        ) { DatePicker(state = datePickerState) }
    }

    // --- Time Picker ---
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
                    Text(stringResource(R.string.btn_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Red) }
            },
            title = { Text(stringResource(R.string.title_select_time), fontWeight = FontWeight.Bold) },
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
                }) { Text(stringResource(R.string.btn_next_time), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Red) } }
        ) { DatePicker(state = datePickerState, title = { Text(" " + stringResource(R.string.title_select_deadline), modifier = Modifier.padding(16.dp)) }) }
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
                    Text(stringResource(R.string.btn_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueTimePicker = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Red) }
            },
            title = { Text(stringResource(R.string.title_deadline_time), fontWeight = FontWeight.Bold) },
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

// --- Custom Always Floating Label Component ---
@Composable
private fun DebtCreditFloatingTextField(
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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
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
                .background(labelBackgroundColor) // Background color to match exactly with screen
                .padding(horizontal = 4.dp)
        )
    }
}