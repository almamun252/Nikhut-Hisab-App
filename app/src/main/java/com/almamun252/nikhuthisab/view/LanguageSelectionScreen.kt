package com.almamun252.nikhuthisab.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.data.LanguagePreferences
import kotlinx.coroutines.launch

@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val languagePreferences = remember { LanguagePreferences(context) }

    var selectedLanguage by remember { mutableStateOf("bn") } // ডিফল্ট বাংলা

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Language,
            contentDescription = "Globe",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF10B981) // Emerald Color
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose your language",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "আপনার ভাষা নির্বাচন করুন",
            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // English Option Card
        LanguageCard(
            title = "English",
            subtitle = "English",
            isSelected = selectedLanguage == "en",
            onClick = { selectedLanguage = "en" }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bangla Option Card
        LanguageCard(
            title = "বাংলা",
            subtitle = "Bengali",
            isSelected = selectedLanguage == "bn",
            onClick = { selectedLanguage = "bn" }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    // ভাষা ডাটাবেসে সেভ করা হচ্ছে
                    languagePreferences.saveLanguage(selectedLanguage)

                    // সেভ হওয়ার পর লগইন স্ক্রিনে পাঠিয়ে দেওয়া
                    navController.navigate("login") {
                        popUpTo("language_selection") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text(
                text = if (selectedLanguage == "bn") "সামনে এগিয়ে যান" else "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun LanguageCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColor = Color(0xFF10B981)
    val bgColor = if (isSelected) themeColor.copy(alpha = 0.1f) else Color.White
    val borderColor = if (isSelected) themeColor else Color.LightGray.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = themeColor, modifier = Modifier.size(28.dp))
        }
    }
}