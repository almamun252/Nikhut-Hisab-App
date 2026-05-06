package com.almamun252.nikhuthisab.view

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // স্টেট মেইনটেইন করার জন্য (ভবিষ্যতে ViewModel থেকে আসবে)
    var autoBackupEnabled by remember { mutableStateOf(true) }

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
                    text = user?.displayName ?: "ব্যবহারকারীর নাম",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = user?.email ?: "example@gmail.com",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ব্যাকআপ এবং সেটিংস সেকশন ---
        Text(
            text = "ডেটা ব্যাকআপ ও সেটিংস",
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )

        SettingsItem(
            title = "এখনই ব্যাকআপ নিন",
            subtitle = "সব ডেটা সার্ভারে সেভ করুন",
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
                    Text("রাত ১২টায় অটো ব্যাকআপ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("প্রতিদিন নীরবে আপডেট হবে", fontSize = 12.sp, color = Color.Gray)
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
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("লগআউট করুন", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
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