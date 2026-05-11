package com.almamun252.nikhuthisab.view

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.data.LanguagePreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // ল্যাঙ্গুয়েজ প্রেফারেন্স ইনিশিয়ালাইজ করা
    val languagePreferences = remember { LanguagePreferences(context) }

    // স্টেট মেইনটেইন করার জন্য
    var autoBackupEnabled by remember { mutableStateOf(true) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // মডার্ন স্লেট ব্যাকগ্রাউন্ড
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // --- প্রোফাইল কার্ড (User Info) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // প্রোফাইল পিকচার (Google থেকে)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = user?.photoUrl ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png",
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // নাম এবং ইমেইল
                Text(
                    text = user?.displayName ?: stringResource(R.string.default_user_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = user?.email ?: stringResource(R.string.default_email),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ব্যাকআপ এবং সেটিংস সেকশন ---
        Text(
            text = stringResource(R.string.data_backup_and_settings),
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )

        // ভাষা পরিবর্তন সেটিংস
        SettingsItem(
            title = stringResource(R.string.change_language),
            subtitle = stringResource(R.string.change_language_desc),
            icon = Icons.Rounded.Language,
            iconColor = Color(0xFF10B981), // Emerald কালার
            onClick = { showLanguageSheet = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsItem(
            title = stringResource(R.string.backup_now),
            subtitle = stringResource(R.string.backup_now_desc),
            icon = Icons.Rounded.CloudUpload,
            iconColor = Color(0xFF005088),
            onClick = { /* ব্যাকআপ ফাংশন কল হবে */ }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // অটো ব্যাকআপ সুইচ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = Color(0xFF11CAA0))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_backup_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(R.string.auto_backup_desc), fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { autoBackupEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- লগআউট বাটন ---
        Button(
            onClick = {
                auth.signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // --- ভাষা পরিবর্তনের Bottom Sheet ---
    if (showLanguageSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.choose_your_preferred_language),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // English Option
                LanguageOptionCard(
                    title = stringResource(R.string.english),
                    subtitle = stringResource(R.string.english),
                    onClick = {
                        coroutineScope.launch {
                            languagePreferences.saveLanguage("en")
                            showLanguageSheet = false
                            // ভাষা পরিবর্তনের পর রিস্টার্ট করা হচ্ছে
                            (context as? Activity)?.recreate()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bangla Option
                LanguageOptionCard(
                    title = stringResource(R.string.bangla),
                    subtitle = stringResource(R.string.bengali),
                    onClick = {
                        coroutineScope.launch {
                            languagePreferences.saveLanguage("bn")
                            showLanguageSheet = false
                            // ভাষা পরিবর্তনের পর রিস্টার্ট করা হচ্ছে
                            (context as? Activity)?.recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LanguageOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val themeColor = Color(0xFF10B981)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
        }
    }
}