package com.almamun252.nikhuthisab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.almamun252.nikhuthisab.ui.theme.NikhutHisabTheme
import com.almamun252.nikhuthisab.view.LoginScreen
import com.almamun252.nikhuthisab.view.MainScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // অ্যাপ ক্র্যাশ রোধ করতে ফায়ারবেস সবার আগে ইনিশিয়ালাইজ করা হলো
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        // অ্যাপ চালুর সাথে সাথেই চেক করবে ইউজার আগে থেকে লগইন করা আছে কি না
        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) "main" else "login"

        setContent {
            NikhutHisabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // একটি টপ-লেভেল নেভিগেশন কন্ট্রোলার তৈরি করা হলো
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDestination) {

                        // লগইন স্ক্রিন
                        composable("login") {
                            LoginScreen(navController = navController)
                        }

                        // মেইন স্ক্রিন (যেখানে আপনার হোম এবং বটম নেভিগেশন আছে)
                        composable("main") {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
}