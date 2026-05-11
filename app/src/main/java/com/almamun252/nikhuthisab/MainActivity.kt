package com.almamun252.nikhuthisab

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // <-- এটি পরিবর্তন করা হয়েছে
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.almamun252.nikhuthisab.data.LanguagePreferences
import com.almamun252.nikhuthisab.ui.theme.NikhutHisabTheme
import com.almamun252.nikhuthisab.view.LanguageSelectionScreen
import com.almamun252.nikhuthisab.view.LoginScreen
import com.almamun252.nikhuthisab.view.MainScreen
import com.almamun252.nikhuthisab.worker.ReminderScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

// ComponentActivity এর বদলে AppCompatActivity ব্যবহার করা হলো
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // অ্যাপ ক্র্যাশ রোধ করতে ফায়ারবেস সবার আগে ইনিশিয়ালাইজ করা হলো
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        // প্রতিদিন রাত ৮টার রিমাইন্ডার চালু করা
        ReminderScheduler.scheduleDailyReminder(this)

        // ভাষা নির্বাচনের প্রেফারেন্স ইনিশিয়ালাইজ করা
        val languagePreferences = LanguagePreferences(this)

        setContent {
            NikhutHisabTheme {

                // DataStore থেকে চেক করা হচ্ছে ইউজার আগে ভাষা নির্বাচন করেছে কি না
                val isLangSelected by languagePreferences.isLanguageSelected.collectAsState(initial = null)

                // --- Android 13+ এর জন্য গ্লোবাল Notification Permission ---
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // পারমিশন দেওয়া হলে বা না হলে যা করার তা এখানে করা যায়
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // DataStore লোড হওয়ার জন্য একটু অপেক্ষা করা হচ্ছে
                    if (isLangSelected == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    // অ্যাপ চালুর সাথে সাথেই চেক করবে ইউজার আগে থেকে লগইন করা আছে কি না
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    // স্টার্ট ডেস্টিনেশন লজিক:
                    // ১. ভাষা সিলেক্ট না করলে Language Screen
                    // ২. লগইন করা থাকলে Main Screen
                    // ৩. নাহলে Login Screen
                    val startDestination = when {
                        isLangSelected == false -> "language_selection"
                        currentUser != null -> "main"
                        else -> "login"
                    }

                    // একটি টপ-লেভেল নেভিগেশন কন্ট্রোলার তৈরি করা হলো
                    val topLevelNavController = rememberNavController()

                    NavHost(navController = topLevelNavController, startDestination = startDestination) {

                        // ভাষা নির্বাচনের স্ক্রিন
                        composable("language_selection") {
                            LanguageSelectionScreen(navController = topLevelNavController)
                        }

                        // লগইন স্ক্রিন
                        composable("login") {
                            LoginScreen(navController = topLevelNavController)
                        }

                        // মেইন স্ক্রিন (যেখানে আপনার হোম এবং বটম নেভিগেশন আছে)
                        composable("main") {
                            // MainScreen নিজের ভেতরের নেভিগেশন নিজেই ম্যানেজ করবে
                            MainScreen(onLogout = {
                                topLevelNavController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}