package com.almamun252.nikhuthisab.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// মেনু আইটেমগুলোর লিস্ট (আধুনিক Rounded আইকন সহ)
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "হোম", Icons.Rounded.Home)
    object Transactions : BottomNavItem("transactions", "লেনদেন", Icons.Rounded.ReceiptLong)
    object Income : BottomNavItem("income", "আয়", Icons.Rounded.ArrowDownward)
    object Expense : BottomNavItem("expense", "ব্যয়", Icons.Rounded.ArrowUpward)
}

@Composable
fun MainScreen() {
    // ন্যাভিগেশন কন্ট্রোলার তৈরি করা হলো
    val navController = rememberNavController()

    // মেনু আইটেমগুলোকে একটি লিস্টে রাখা হলো (Transactions যোগ করা হয়েছে)
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Income,
        BottomNavItem.Expense
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp, // ন্যাভিগেশন বারের নিচে সুন্দর শ্যাডো দেওয়ার জন্য
                modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) // উপরের দুই কোণা রাউন্ড করা হলো
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, // সিলেক্ট থাকলে প্রাইমারি কালার
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer, // আইকনের পেছনের ব্যাকগ্রাউন্ড
                            unselectedIconColor = Color.Gray, // সিলেক্ট না থাকলে গ্রে কালার
                            unselectedTextColor = Color.Gray
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                // ব্যাকস্ট্যাক ক্লিন রাখার লজিক
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
    ) { innerPadding ->
        // NavHost হলো সেই জায়গা, যেখানে স্ক্রিনগুলো বদলে বদলে বসবে
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ড্যাশবোর্ড স্ক্রিন
            composable(BottomNavItem.Dashboard.route) {
                // প্রয়োজনে এখানেও navController পাস করতে পারেন: HomeScreen(navController = navController)
                HomeScreen()
            }

            // সব লেনদেন স্ক্রিন (নতুন যুক্ত করা হলো)
            composable(BottomNavItem.Transactions.route) {
                TransactionsScreen(navController = navController)
            }

            // আয় স্ক্রিন
            composable(BottomNavItem.Income.route) {
                IncomeScreen(navController = navController)
            }

            // ব্যয় স্ক্রিন
            composable(BottomNavItem.Expense.route) {
                ExpenseScreen(navController = navController)
            }

            // --- নতুন হিসাব যোগ করার স্ক্রিনগুলো ---
            composable("add_expense") {
                AddTransactionScreen(navController = navController, isIncome = false)
            }
            composable("add_income") {
                AddTransactionScreen(navController = navController, isIncome = true)
            }
        }
    }
}