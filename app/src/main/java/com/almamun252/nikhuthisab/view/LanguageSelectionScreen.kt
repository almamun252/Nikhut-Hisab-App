package com.almamun252.nikhuthisab.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.almamun252.nikhuthisab.data.CurrencyManager
import com.almamun252.nikhuthisab.data.LanguagePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val languagePreferences = remember { LanguagePreferences(context) }

    var selectedLanguage by remember { mutableStateOf("") }
    var selectedCurrencySymbol by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val isButtonEnabled = selectedLanguage.isNotEmpty() && selectedCurrencySymbol.isNotEmpty()

    // 🔥 CurrencyManager থেকে লিস্টটি মেমোরি থেকে রিড করা হচ্ছে (ল্যাগ-ফ্রি)
    val currencyList = remember { CurrencyManager.currencyList }
    val filteredCurrencies = currencyList.filter {
        it.country.contains(searchText, ignoreCase = true) ||
                it.code.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Rounded.Language,
            contentDescription = "Globe",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF10B981)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose your language",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(text = "আপনার ভাষা নির্বাচন করুন", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        LanguageCard("English", "English", selectedLanguage == "en") { selectedLanguage = "en" }
        Spacer(modifier = Modifier.height(12.dp))
        LanguageCard("বাংলা", "Bengali", selectedLanguage == "bn") { selectedLanguage = "bn" }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (selectedLanguage == "bn") "আপনার মুদ্রা নির্বাচন করুন" else "Choose your currency",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    expanded = true
                },
                placeholder = { Text(if (selectedLanguage == "bn") "দেশ বা মুদ্রা খুঁজুন..." else "Search country or currency...") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White).fillMaxHeight(0.5f)
            ) {
                filteredCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.symbol} - ${currency.country} (${currency.code})") },
                        onClick = {
                            selectedCurrencySymbol = currency.symbol
                            searchText = "${currency.symbol} - ${currency.code} (${currency.country})"
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (isButtonEnabled) {
                    coroutineScope.launch {
                        languagePreferences.saveLanguageAndCurrency(selectedLanguage, selectedCurrencySymbol)
                        navController.navigate("login") { popUpTo("language_selection") { inclusive = true } }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = isButtonEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), disabledContainerColor = Color.LightGray)
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
fun LanguageCard(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val themeColor = Color(0xFF10B981)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) themeColor.copy(0.1f) else Color.White)
            .border(2.dp, if (isSelected) themeColor else Color.LightGray.copy(0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = themeColor, modifier = Modifier.size(28.dp))
    }
}