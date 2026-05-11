package com.almamun252.nikhuthisab.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.ShoppingItem
import com.almamun252.nikhuthisab.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(
    navController: NavController,
    shoppingViewModel: ShoppingViewModel = viewModel()
) {
    val context = LocalContext.current
    val shoppingItems by shoppingViewModel.allShoppingItems.collectAsState(initial = emptyList())

    // UI States
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Multi-Select States
    var selectedItemIds by remember { mutableStateOf(setOf<Int>()) }
    val isMultiSelectMode = selectedItemIds.isNotEmpty()

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Purchase Dialog State
    var purchaseItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var actualPriceInput by remember { mutableStateOf("") }

    // Dynamic Strings initialization for defaults
    val defaultMergeTitle = stringResource(R.string.default_merge_title)
    val catOtherStr = stringResource(R.string.cat_other)

    // Merge Dialog State
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeTitleInput by remember { mutableStateOf(defaultMergeTitle) }
    var mergeTotalActualPriceInput by remember { mutableStateOf("") }

    // Transfer Dialog State
    var transferItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

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

    // Input States for Add
    var inputName by remember { mutableStateOf("") }
    var inputEstimatedPrice by remember { mutableStateOf("") }

    // Analytics
    val totalEstimated = shoppingItems.filter { !it.isPurchased }.sumOf { it.estimatedPrice }
    val totalActual = shoppingItems.filter { it.isPurchased }.sumOf { it.actualPrice }
    val totalItems = shoppingItems.size
    val purchasedItems = shoppingItems.count { it.isPurchased }

    val themeColor = Color(0xFF0F766E) // Deep Modern Teal
    val lightThemeColor = Color(0xFFF0FDFA)
    val bgColor = Color(0xFFF8FAFC)

    Scaffold(
        containerColor = bgColor,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isVisible && !isMultiSelectMode,
                enter = slideInVertically(tween(600, delayMillis = 300)) { 150 } + fadeIn(tween(600)),
                exit = slideOutVertically(tween(300)) { 150 } + fadeOut(tween(300))
            ) {
                FloatingActionButton(
                    onClick = {
                        inputName = ""
                        inputEstimatedPrice = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.padding(bottom = 90.dp).size(60.dp),
                    containerColor = themeColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.desc_add), modifier = Modifier.size(30.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Contextual Selection Bar (Only visible when multi-select is active) ---
            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedItemIds = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.desc_close), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.label_items_selected, selectedItemIds.size), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row {
                        IconButton(
                            onClick = {
                                mergeTitleInput = defaultMergeTitle
                                showMergeDialog = true
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.CallMerge, contentDescription = stringResource(R.string.desc_merge), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                shoppingViewModel.deleteMultipleItems(selectedItemIds.toList())
                                selectedItemIds = emptySet()
                            },
                            modifier = Modifier.background(Color(0xFFFFB4AB).copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.desc_delete), tint = Color(0xFFFFB4AB))
                        }
                    }
                }
            }

            if (!isMultiSelectMode && shoppingItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.desc_clear_all_items), fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!isMultiSelectMode) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Modern Summary Card (Gradient & Glass effect) ---
            AnimatedVisibility(visible = isVisible && !isMultiSelectMode, enter = fadeIn(tween(600))) {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = themeColor.copy(alpha = 0.3f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(themeColor, Color(0xFF14B8A6))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(stringResource(R.string.label_estimated_cost_remaining), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("৳${totalEstimated.toInt()}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(stringResource(R.string.label_actual_cost_purchased), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("৳${totalActual.toInt()}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            val progress = if (totalItems > 0) purchasedItems.toFloat() / totalItems.toFloat() else 0f
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("$purchasedItems / $totalItems", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // --- Minimalist List View ---
            if (shoppingItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).background(lightThemeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ShoppingBasket, contentDescription = null, tint = themeColor.copy(alpha = 0.6f), modifier = Modifier.size(50.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(stringResource(R.string.msg_shopping_list_empty), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.msg_shopping_list_empty_desc), fontSize = 15.sp, color = Color(0xFF94A3B8))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shoppingItems, key = { it.id }) { item ->
                        val isSelected = selectedItemIds.contains(item.id)
                        ModernShoppingItemCard(
                            item = item,
                            themeColor = themeColor,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedItemIds -= item.id else selectedItemIds += item.id
                            },
                            onCheckClick = {
                                if (!isMultiSelectMode) {
                                    if (item.isPurchased) {
                                        shoppingViewModel.updateItem(item.copy(isPurchased = false, actualPrice = 0.0))
                                    } else {
                                        purchaseItem = item
                                        actualPriceInput = if (item.estimatedPrice > 0) item.estimatedPrice.toInt().toString() else ""
                                    }
                                }
                            },
                            onTransferClick = {
                                transferItem = item
                                selectedCategory = ""
                            }
                        )
                    }
                }
            }
        }

        // --- Dialogs ---

        // 1. Add Item Dialog (Fixed Floating Labels)
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text(stringResource(R.string.title_new_item), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AlwaysFloatingOutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = stringResource(R.string.hint_item_name),
                            themeColor = themeColor
                        )
                        AlwaysFloatingOutlinedTextField(
                            value = inputEstimatedPrice,
                            onValueChange = { inputEstimatedPrice = it },
                            label = stringResource(R.string.hint_estimated_price),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = themeColor) },
                            themeColor = themeColor
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputName.isNotBlank()) {
                                val estPrice = inputEstimatedPrice.toDoubleOrNull() ?: 0.0
                                shoppingViewModel.insertItem(ShoppingItem(name = inputName.trim(), estimatedPrice = estPrice, dateAdded = System.currentTimeMillis()))
                                showAddDialog = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.msg_enter_item_name), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.btn_add_item), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // 2. Purchase Dialog (Fixed Floating Labels)
        if (purchaseItem != null) {
            AlertDialog(
                onDismissRequest = { purchaseItem = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text(stringResource(R.string.title_enter_actual_price), fontWeight = FontWeight.ExtraBold) },
                text = {
                    AlwaysFloatingOutlinedTextField(
                        value = actualPriceInput,
                        onValueChange = { actualPriceInput = it },
                        label = stringResource(R.string.hint_actual_price_of_item, purchaseItem?.name ?: ""),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = themeColor) },
                        themeColor = themeColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = actualPriceInput.toDoubleOrNull() ?: 0.0
                            if (price >= 0) {
                                shoppingViewModel.updateItem(purchaseItem!!.copy(isPurchased = true, actualPrice = price))
                                purchaseItem = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.btn_confirm), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { purchaseItem = null }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // 3. Merge Items Dialog (Fixed Floating Labels)
        if (showMergeDialog) {
            val preCalculatedTotal = selectedItemIds.mapNotNull { id -> shoppingItems.find { it.id == id } }
                .sumOf { if (it.isPurchased) it.actualPrice else it.estimatedPrice }

            LaunchedEffect(showMergeDialog) {
                if (mergeTotalActualPriceInput.isEmpty() && preCalculatedTotal > 0) {
                    mergeTotalActualPriceInput = preCalculatedTotal.toInt().toString()
                }
            }

            AlertDialog(
                onDismissRequest = { showMergeDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text(stringResource(R.string.title_merge_items), fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.msg_merge_description, selectedItemIds.size), fontSize = 14.sp, color = Color.Gray)

                        AlwaysFloatingOutlinedTextField(
                            value = mergeTitleInput,
                            onValueChange = { mergeTitleInput = it },
                            label = stringResource(R.string.hint_new_name_merge),
                            themeColor = themeColor
                        )
                        AlwaysFloatingOutlinedTextField(
                            value = mergeTotalActualPriceInput,
                            onValueChange = { mergeTotalActualPriceInput = it },
                            label = stringResource(R.string.hint_total_actual_price),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = themeColor) },
                            themeColor = themeColor
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = mergeTotalActualPriceInput.toDoubleOrNull()
                            if (mergeTitleInput.isNotBlank() && price != null && price >= 0) {
                                val itemsToMerge = shoppingItems.filter { selectedItemIds.contains(it.id) }
                                shoppingViewModel.mergeItems(itemsToMerge, mergeTitleInput.trim(), price)
                                selectedItemIds = emptySet()
                                showMergeDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.btn_merge), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showMergeDialog = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // 4. Transfer to Main Dialog (Fixed Floating Labels)
        if (transferItem != null) {
            AlertDialog(
                onDismissRequest = { transferItem = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = themeColor, modifier = Modifier.size(40.dp)) },
                title = { Text(stringResource(R.string.title_transfer_to_main), fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(stringResource(R.string.msg_transfer_to_main_desc), color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            AlwaysFloatingOutlinedTextField(
                                value = selectedCategory,
                                onValueChange = { },
                                readOnly = true,
                                label = stringResource(R.string.label_category),
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                themeColor = themeColor
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedCategory.isNotBlank()) {
                                shoppingViewModel.moveToMain(transferItem!!, selectedCategory)
                                transferItem = null
                                Toast.makeText(context, context.getString(R.string.msg_moved_to_main_success), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.msg_transfer_category_required), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.btn_add), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { transferItem = null }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }

        // 5. Clear All Confirmation
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text(stringResource(R.string.title_clear_shopping_list), color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold) },
                text = { Text(stringResource(R.string.msg_clear_shopping_list_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            shoppingViewModel.clearAll()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(50)
                    ) { Text(stringResource(R.string.btn_yes_clear), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
                }
            )
        }
    }
}

// --- Custom Always Floating Label Component ---
@Composable
fun AlwaysFloatingOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    themeColor: Color
) {
    Box(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            singleLine = true
        )

        // Custom always-floating label text placed precisely over the border line
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp)
                .offset(y = (-8).dp)
                .background(Color.White) // Matches Dialog background perfectly
                .padding(horizontal = 4.dp)
        )
    }
}

// --- Modern Minimalist Item Card ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernShoppingItemCard(
    item: ShoppingItem,
    themeColor: Color,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCheckClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    val isMoved = item.isMovedToMain

    // Smooth background transition for selection
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) themeColor.copy(alpha = 0.08f) else Color.White,
        animationSpec = tween(200), label = "bg_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (isMultiSelectMode) onToggleSelect() else onCheckClick() },
                onLongClick = { onToggleSelect() }
            )
            .then(
                if (isSelected) Modifier.border(2.dp, themeColor, RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                // Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> themeColor
                                item.isPurchased -> Color(0xFF10B981)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .then(
                            if (!item.isPurchased && !isSelected) Modifier.border(1.dp, Color.LightGray, CircleShape)
                            else Modifier
                        )
                        .clickable { if (isMultiSelectMode) onToggleSelect() else onCheckClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected || item.isPurchased) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Item Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPurchased) Color(0xFF94A3B8) else Color(0xFF1E293B),
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Bundle Note Indicator
                    if (item.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ViewModule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.note,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Price Section
                Column(horizontalAlignment = Alignment.End) {
                    if (item.isPurchased) {
                        Text("৳${item.actualPrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
                    } else {
                        if (item.estimatedPrice > 0) {
                            Text("৳${item.estimatedPrice.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF94A3B8))
                        } else {
                            Text("—", color = Color.LightGray, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Transfer Action Area
            AnimatedVisibility(visible = item.isPurchased && !isMultiSelectMode, enter = expandVertically(spring())) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isMoved) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0FDFA), RoundedCornerShape(8.dp)).padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.status_moved_to_main), color = Color(0xFF0D9488), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTransferClick() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = "Move", tint = themeColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_move_to_main), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColor)
                        }
                    }
                }
            }
        }
    }
}