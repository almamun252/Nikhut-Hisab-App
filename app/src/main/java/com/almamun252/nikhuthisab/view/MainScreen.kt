package com.almamun252.nikhuthisab.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.almamun252.nikhuthisab.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// মেনু আইটেমগুলোর লিস্ট
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "হোম", Icons.Rounded.Home)
    object Transactions : BottomNavItem("transactions", "লেনদেন", Icons.Rounded.ReceiptLong)
    object Profile : BottomNavItem("profile", "প্রোফাইল", Icons.Rounded.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TransactionViewModel = viewModel()) {
    val navController = rememberNavController()

    // বটম ন্যাভিগেশনে ৩টি আইটেম
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")

    val isMainScreen = currentRoute in items.map { it.route } || currentRoute == null

    // --- নতুন টপ বারের স্টেট ---
    var isDarkMode by remember { mutableStateOf(false) } // থিম টগল করার স্টেট (এটি গ্লোবাল স্টেটে কানেক্ট করতে হবে)
    var showNotificationSheet by remember { mutableStateOf(false) } // নোটিফিকেশন শিট দেখানোর স্টেট

    // ভিউমডেল থেকে আসল নোটিফিকেশনের ডেটা পড়া হচ্ছে
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(300))
            ) {
                // সাধারণ ও ফিক্সড টপ বার ডিজাইন
                Surface(
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp) // হালকা একটি শ্যাডো দেওয়া হলো
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding() // স্ট্যাটাস বারের নিচে কন্টেন্ট নামানোর জন্য
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // বাম পাশের অংশ (ডাইনামিক টাইটেল বা গ্রিটিং)
                        val titleText = when (currentRoute) {
                            BottomNavItem.Dashboard.route -> "ড্যাশবোর্ড"
                            "income_expense" -> "আয় ও ব্যয় খাতা"
                            BottomNavItem.Transactions.route -> "সকল লেনদেন"
                            BottomNavItem.Profile.route -> "প্রোফাইল"
                            else -> "ড্যাশবোর্ড"
                        }

                        Text(
                            text = titleText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium, // নরমাল ফন্ট
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // ডান পাশের অংশ (থিম টগল এবং নোটিফিকেশন)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // থিম পরিবর্তন (Dark/Light Mode) আইকন
                            IconButton(
                                onClick = { isDarkMode = !isDarkMode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "থিম পরিবর্তন",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // নোটিফিকেশন বেল আইকন
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable { showNotificationSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "নোটিফিকেশন",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )

                                // আনরিড নোটিফিকেশন লাল ডট (যদি রিমাইন্ডার লিস্ট খালি না হয়)
                                if (upcomingReminders.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300))
            ) {
                Surface(
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // নেভিগেশন বারের প্যাডিং Surface এর ভেতরে দেওয়া হলো,
                            // ফলে সাদা রংটি একদম নিচ পর্যন্ত যাবে এবং আইকনগুলো উপরে থাকবে
                            .navigationBarsPadding()
                            .padding(top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                            BkashStyleNavItem(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Dashboard.route,
                modifier = Modifier.padding(
                    top = if (isMainScreen) innerPadding.calculateTopPadding() else 0.dp,
                    bottom = innerPadding.calculateBottomPadding()
                ),
                enterTransition = { fadeIn(animationSpec = tween(400)) },
                exitTransition = { fadeOut(animationSpec = tween(400)) },
                popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                popExitTransition = { fadeOut(animationSpec = tween(400)) }
            ) {
                composable(BottomNavItem.Dashboard.route) {
                    HomeScreen(navController = navController)
                }
                composable(BottomNavItem.Transactions.route) {
                    TransactionsScreen(navController = navController)
                }
                composable("income_expense") {
                    IncomeExpenseScreen(navController = navController)
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(navController = navController)
                }
                composable("debt_credit") {
                    DebtCreditScreen(navController = navController)
                }
                composable(
                    route = "add_expense?transactionId={transactionId}",
                    arguments = listOf(navArgument("transactionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("transactionId")
                    val transactionId = if (id != -1) id else null
                    AddTransactionScreen(navController = navController, isIncome = false, transactionId = transactionId)
                }
                composable(
                    route = "add_income?transactionId={transactionId}",
                    arguments = listOf(navArgument("transactionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("transactionId")
                    val transactionId = if (id != -1) id else null
                    AddTransactionScreen(navController = navController, isIncome = true, transactionId = transactionId)
                }
                composable(
                    route = "add_debt_credit?type={type}&transactionId={transactionId}",
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = "Lending"
                        },
                        navArgument("transactionId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "Lending"
                    val id = backStackEntry.arguments?.getInt("transactionId")
                    val transactionId = if (id != -1) id else null
                    AddDebtCreditScreen(navController = navController, type = type, transactionId = transactionId)
                }
            }

            // --- নোটিফিকেশন বটম শিট ---
            if (showNotificationSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showNotificationSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "আপনার নোটিফিকেশন",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (upcomingReminders.isEmpty()) {
                            // নোটিফিকেশন না থাকলে খালি মেসেজ দেখাবে
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("কোনো নতুন নোটিফিকেশন নেই।", color = Color.Gray, fontSize = 16.sp)
                                }
                            }
                        } else {
                            // আসল নোটিফিকেশনগুলোর লিস্ট
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(upcomingReminders) { tx ->
                                    val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale("bn", "BD"))
                                    val dateStr = tx.dueDate?.let { sdf.format(Date(it)) } ?: ""

                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red).padding(top = 4.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val msg = if (tx.type == "Lending") {
                                                "${tx.title} এর ৳${tx.amount.toInt()} পরিশোধ করার কথা।"
                                            } else {
                                                "${tx.title} কে ৳${tx.amount.toInt()} পরিশোধ করতে হবে।"
                                            }
                                            Text(msg, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("ডেডলাইন: $dateStr", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// স্ক্রিনশটের মত আইকন এবং তার নিচে টেক্সট সম্বলিত ন্যাভ আইটেম
@Composable
fun RowScope.BkashStyleNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray

    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // স্কয়ার রিপল ইফেক্ট বন্ধ করা হয়েছে, সিম্পল ক্লিক
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}