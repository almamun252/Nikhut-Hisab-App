package com.almamun252.nikhuthisab.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Home
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// মেনু আইটেমগুলোর লিস্ট
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "হোম", Icons.Rounded.Home)
    object Transactions : BottomNavItem("transactions", "লেনদেন", Icons.Rounded.ReceiptLong)
    object Income : BottomNavItem("income", "আয়", Icons.Rounded.ArrowDownward)
    object Expense : BottomNavItem("expense", "ব্যয়", Icons.Rounded.ArrowUpward)
    object Profile : BottomNavItem("profile", "প্রোফাইল", Icons.Rounded.Person)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Income,
        BottomNavItem.Expense,
        BottomNavItem.Profile
    )

    Scaffold(
        containerColor = Color(0xFFF1F5F9), // ব্যাকগ্রাউন্ড একটু গাঢ় গ্রে করা হলো যাতে সাদা বারটি ফুটে ওঠে
        bottomBar = {
            // সম্পূর্ণ কাস্টম এবং অ্যানিমেটেড বটম বার
            Surface(
                color = Color.White, // বারটি পুরোপুরি সাদা
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE2E8F0), // হালকা একটি বর্ডার দেওয়া হলো
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp), // ভেতরের স্পেসিং
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                        AnimatedNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
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
            composable(BottomNavItem.Income.route) {
                IncomeScreen(navController = navController)
            }
            composable(BottomNavItem.Expense.route) {
                ExpenseScreen(navController = navController)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(navController = navController)
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
        }
    }
}

// কাস্টম অ্যানিমেটেড ন্যাভ আইটেম
@Composable
fun AnimatedNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // সিলেক্টেড অবস্থার রং (এখন অনেক বেশি সলিড ও উজ্জ্বল)
    val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp)) // পিল শেইপ
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            // শুধু সিলেক্ট করা থাকলেই নাম দেখাবে, এবং স্লাইডিং অ্যানিমেশন হবে
            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = item.title,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}