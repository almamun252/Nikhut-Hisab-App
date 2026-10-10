package com.almamun252.nikhuthisab.view

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // 🔥 নতুন ইম্পোর্ট
import androidx.compose.foundation.lazy.items // 🔥 নতুন ইম্পোর্ট
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.data.CurrencyManager // 🔥 নতুন ইম্পোর্ট
import com.almamun252.nikhuthisab.data.LanguagePreferences
import com.almamun252.nikhuthisab.viewmodel.AuthViewModel
import com.almamun252.nikhuthisab.viewmodel.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 🔥 রিয়েল-টাইমে ইউজারের স্টেট ট্র্যাক করা হচ্ছে (লগ-ইন হওয়ামাত্রই UI আপডেট হবে)
    val authUser by authViewModel.currentUser.collectAsState()
    val isGuestMode = authUser == null

    val uiState by viewModel.uiState.collectAsState()
    val languagePreferences = remember { LanguagePreferences(context) }

    // 🔥 বর্তমান কারেন্সি স্টেট রিড করা হচ্ছে
    val currentCurrency by languagePreferences.selectedCurrency.collectAsState(initial = "৳")

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) } // 🔥 কারেন্সি শিটের স্টেট
    var showLoginSheet by remember { mutableStateOf(false) }

    // 🔥 লগ-আউট কনফার্মেশন ডায়ালগের জন্য স্টেট
    var showLogoutDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // --- Google Auth Launcher ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    authViewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Login Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 লগ-ইন সফল হলে পপ-আপ নিজে থেকে বন্ধ হবে এবং গেস্ট মোড মুছে যাবে
    LaunchedEffect(authUser) {
        if (authUser != null) {
            showLoginSheet = false
            val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("is_guest_mode", false).apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // --- প্রোফাইল কার্ড (User Info) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(enabled = isGuestMode) {
                    showLoginSheet = true
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // প্রোফাইল পিকচার
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = authUser?.photoUrl ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png",
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // নাম এবং ইমেইল (Multi-language সাপোর্টসহ)
                Text(
                    text = if (isGuestMode) stringResource(R.string.guest_user) else uiState.userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isGuestMode) stringResource(R.string.login_desc) else uiState.userEmail,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isGuestMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showLoginSheet = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Rounded.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.login_with_google), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            iconColor = Color(0xFF10B981),
            onClick = { showLanguageSheet = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 🔥 কারেন্সি বা মুদ্রা পরিবর্তন সেটিংস
        SettingsItem(
            title = "Change Currency", // আপনি চাইলে এটি strings.xml এ নিতে পারেন
            subtitle = "Current: $currentCurrency",
            icon = Icons.Rounded.Payments,
            iconColor = Color(0xFF3B82F6), // সুন্দর নীল রঙ
            onClick = { showCurrencySheet = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ব্যাকআপ বাটন
        val loginRequiredBackupStr = stringResource(R.string.login_required_backup)
        SettingsItem(
            title = stringResource(R.string.backup_now),
            subtitle = if (isGuestMode) loginRequiredBackupStr else if (uiState.isBackingUp) "Backing up..." else "Last Backup: ${uiState.lastBackupTime}",
            icon = Icons.Rounded.CloudUpload,
            iconColor = Color(0xFF005088),
            isLoading = uiState.isBackingUp,
            onClick = {
                if (isGuestMode) {
                    showLoginSheet = true
                } else if (!uiState.isBackingUp && !uiState.isRestoring) {
                    viewModel.triggerBackup { isSuccess, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // রিস্টোর বাটন
        val loginRequiredRestoreStr = stringResource(R.string.login_required_restore)
        SettingsItem(
            title = "Restore Data",
            subtitle = if (isGuestMode) loginRequiredRestoreStr else if (uiState.isRestoring) "Restoring data..." else "Restore from Google Drive",
            icon = Icons.Rounded.Restore,
            iconColor = Color(0xFFD97706),
            isLoading = uiState.isRestoring,
            onClick = {
                if (isGuestMode) {
                    showLoginSheet = true
                } else if (!uiState.isBackingUp && !uiState.isRestoring) {
                    viewModel.triggerRestore { isSuccess, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
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
                    checked = if (isGuestMode) false else uiState.isAutoBackupEnabled,
                    onCheckedChange = {
                        if (isGuestMode) {
                            showLoginSheet = true
                        } else {
                            viewModel.toggleAutoBackup(it)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- লগআউট বাটন ---
        if (!isGuestMode) {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // --- 🔥 লগ-আউট কনফার্মেশন ডায়ালগ ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(text = stringResource(R.string.logout_confirm_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = stringResource(R.string.logout_confirm_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.yes), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.no), color = Color.Gray)
                }
            }
        )
    }

    // --- পপ-আপ লগ-ইন স্ক্রিন (Bottom Sheet) ---
    if (showLoginSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val authLoading by authViewModel.isLoading.collectAsState()

        ModalBottomSheet(
            onDismissRequest = { showLoginSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💸", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("নিখুঁত হিসাব", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.login_desc), textAlign = TextAlign.Center, color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

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
                    enabled = !authLoading
                ) {
                    if (authLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.login_with_google), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
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

                LanguageOptionCard(
                    title = stringResource(R.string.english),
                    subtitle = stringResource(R.string.english),
                    onClick = {
                        coroutineScope.launch {
                            languagePreferences.saveLanguage("en")
                            showLanguageSheet = false
                            (context as? Activity)?.recreate()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LanguageOptionCard(
                    title = stringResource(R.string.bangla),
                    subtitle = stringResource(R.string.bengali),
                    onClick = {
                        coroutineScope.launch {
                            languagePreferences.saveLanguage("bn")
                            showLanguageSheet = false
                            (context as? Activity)?.recreate()
                        }
                    }
                )
            }
        }
    }

    // --- 🔥 কারেন্সি বা মুদ্রা পরিবর্তনের Bottom Sheet ---
    if (showCurrencySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchText by remember { mutableStateOf("") }

        // CurrencyManager থেকে ডেটা নিয়ে ফিল্টার করা হচ্ছে
        val currencyList = remember { CurrencyManager.currencyList }
        val filteredCurrencies = currencyList.filter {
            it.country.contains(searchText, ignoreCase = true) ||
                    it.code.contains(searchText, ignoreCase = true)
        }

        ModalBottomSheet(
            onDismissRequest = { showCurrencySheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight(0.85f) // স্ক্রিনের 85% জায়গা নেবে
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Select Currency",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // সার্চ বক্স
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search country or currency...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // দেশ ও মুদ্রার লিস্ট (LazyColumn দিয়ে অপ্টিমাইজড)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredCurrencies) { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        // ডাটাবেসে নতুন কারেন্সি সেভ হচ্ছে
                                        languagePreferences.saveCurrency(currency.symbol)
                                        showCurrencySheet = false
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currency.symbol,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = currency.country,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = currency.code,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(icon, contentDescription = null, tint = iconColor)
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = if (isLoading) iconColor else Color.Gray)
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