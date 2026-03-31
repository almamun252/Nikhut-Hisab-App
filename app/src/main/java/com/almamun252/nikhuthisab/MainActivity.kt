package com.almamun252.nikhuthisab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.almamun252.nikhuthisab.ui.theme.NikhutHisabTheme
import com.almamun252.nikhuthisab.view.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NikhutHisabTheme {
                // আমাদের বানানো নতুন হোমস্ক্রিনটি এখানে কল করা হলো
                MainScreen()
            }
        }
    }
}