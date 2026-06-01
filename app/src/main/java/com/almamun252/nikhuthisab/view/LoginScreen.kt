package com.almamun252.nikhuthisab.view

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                val errorMsg = context.getString(R.string.login_error)
                Toast.makeText(context, "$errorMsg: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💸", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name_display),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- Google Login Button ---
            Button(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.start_with_google), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- শুধু এইবারের জন্য স্কিপ করার বাটন ---
            TextButton(
                onClick = {
                    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    // শুধু গেস্ট মোড ট্রু করছি, কিন্তু never_show_login ট্রু করছি না
                    sharedPref.edit().putBoolean("is_guest_mode", true).apply()

                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.continue_without_login),
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            // --- আর কখনো না দেখানোর বাটন ---
            TextButton(
                onClick = {
                    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit()
                        .putBoolean("is_guest_mode", true)
                        .putBoolean("never_show_login", true) // 🔥 চিরতরে লগ-ইন পেজ বন্ধ করা হলো
                        .apply()

                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.never_show_again),
                    color = Color.Red.copy(alpha = 0.7f), // একটু লালচে রঙ দেওয়া হলো বোঝানোর জন্য
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline // নিচে আন্ডারলাইন দেওয়া হলো
                )
            }
        }
    }
}