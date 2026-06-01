package com.almamun252.nikhuthisab.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections

class GoogleDriveHelper(private val context: Context) {

    // এই ফাংশনটি Google Sign-In অ্যাকাউন্ট ব্যবহার করে Drive Service তৈরি করবে
    fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singletonList(DriveScopes.DRIVE_APPDATA) // Hidden ফোল্ডার এক্সেস
        )

        // !! ব্যবহার করা হলো কারণ লগ-ইন সাকসেসফুল হলেই কেবল এখানে ডাটা আসবে
        credential.selectedAccount = account.account!!

        return Drive.Builder(
            NetHttpTransport(), // AndroidHttp এর বদলে আধুনিক NetHttpTransport ব্যবহার করা হলো
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Nikhut Hisab")
            .build()
    }

    // JSON ডাটা গুগল ড্রাইভে আপলোড করার ফাংশন
    suspend fun uploadBackup(driveService: Drive, jsonData: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 🔥 আপডেট: query থেকে spaces অংশটুকু বাদ দেওয়া হয়েছে
                val query = "name='nikhut_hisab_backup.json'"
                val fileList = driveService.files().list().setQ(query).setSpaces("appDataFolder").execute()

                val fileMetadata = File().apply {
                    name = "nikhut_hisab_backup.json"
                    parents = listOf("appDataFolder") // Hidden ফোল্ডার
                }

                // String ডাটাকে ফাইলে কনভার্ট করা হচ্ছে
                val mediaContent = ByteArrayContent.fromString("application/json", jsonData)

                if (fileList.files.isNotEmpty()) {
                    // আগে থেকেই ব্যাকআপ থাকলে শুধু ডাটা আপডেট (Overwrite) করবে
                    val fileId = fileList.files[0].id
                    driveService.files().update(fileId, null, mediaContent).execute()
                } else {
                    // নতুন ব্যাকআপ ফাইল তৈরি করবে
                    driveService.files().create(fileMetadata, mediaContent).execute()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // গুগল ড্রাইভ থেকে JSON ডাটা ডাউনলোড করার ফাংশন
    suspend fun downloadBackup(driveService: Drive): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 🔥 আপডেট: query থেকে spaces অংশটুকু বাদ দেওয়া হয়েছে
                val query = "name='nikhut_hisab_backup.json'"
                val fileList = driveService.files().list().setQ(query).setSpaces("appDataFolder").execute()

                if (fileList.files.isNotEmpty()) {
                    val fileId = fileList.files[0].id
                    val outputStream = ByteArrayOutputStream()

                    // ফাইল ডাউনলোড করে String-এ কনভার্ট করা হচ্ছে
                    driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                    outputStream.toString("UTF-8")
                } else {
                    null // কোনো ব্যাকআপ ফাইল পাওয়া যায়নি
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 🔥 ড্রাইভ থেকে ব্যাকআপ ফাইলের সর্বশেষ মডিফাই করার সময় জানার ফাংশন
    suspend fun getLastBackupTime(driveService: Drive): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val query = "name='nikhut_hisab_backup.json'"
                // খেয়াল করুন: শুধু ফাইলের লিস্ট নয়, আমরা 'modifiedTime' ফিল্ডটিও রিকোয়েস্ট করছি
                val fileList = driveService.files().list()
                    .setQ(query)
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name, modifiedTime)")
                    .execute()

                if (fileList.files.isNotEmpty()) {
                    val file = fileList.files[0]
                    // গুগল ড্রাইভের modifiedTime হলো DateTime অবজেক্ট, সেটিকে Milliseconds এ কনভার্ট করছি
                    file.modifiedTime?.value
                } else {
                    null // ফাইল না থাকলে null রিটার্ন করবে
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}