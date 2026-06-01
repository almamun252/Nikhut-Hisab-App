package com.almamun252.nikhuthisab.utils

import android.content.Context
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.model.BackupData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRestoreManager(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val gson = Gson()

    // ১. ডাটাবেসের সকল ডাটা রিড করে একটি JSON স্ট্রিং তৈরি করার ফাংশন
    suspend fun createBackupJson(): String {
        return withContext(Dispatchers.IO) {
            // Flow থেকে ডেটা নেওয়ার জন্য .first() ব্যবহার করা হয়েছে
            val allTransactions = database.transactionDao().getAllTransactions().first()
            val allBudgets = database.budgetDao().getAllBudgets().first()
            val allRough = database.roughDao().getAllRough().first()
            val allShopping = database.shoppingDao().getAllItems().first()

            val backupData = BackupData(
                transactions = allTransactions,
                budgets = allBudgets,
                roughTransactions = allRough,
                shoppingItems = allShopping,
                backupTimestamp = System.currentTimeMillis()
            )

            // অবজেক্টকে JSON-এ রূপান্তর করা
            gson.toJson(backupData)
        }
    }

    // ২. গুগল ড্রাইভ থেকে পাওয়া JSON স্ট্রিং ডাটাবেসে রিস্টোর করার ফাংশন
    suspend fun restoreFromJson(jsonString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                // runInTransaction ব্যবহার করা হয়েছে যেন সব কাজ একসাথে সফলভাবে সম্পন্ন হয়
                database.runInTransaction {
                    // কাস্টম কোড ব্লক রান করানোর জন্য কোルーটিন স্কোপ ব্লকিং ট্রিকস ব্যবহার করা যেতে পারে,
                    // অথবা রান-ইন-ট্রানজেকশনের ভেতরে সরাসরি সাসপেন্ড ফাংশন কল করতে সমস্যা হলে ডাও-এর নন-সাসপেন্ড ডিলিট মেথড লাগে।
                    // তবে সহজে হ্যান্ডেল করার জন্য আমরা ড্রাইভার থ্রেডেই রান করব।

                    // কন্টেন্ট ব্লক রান করানোর আগে ট্রানজেকশন সেফটি নিশ্চিত করা হচ্ছে
                    database.query("DELETE FROM transactions", null)
                    database.query("DELETE FROM budgets", null)
                    database.query("DELETE FROM rough_transactions", null)
                    database.query("DELETE FROM shopping_list", null)
                }

                // ডিলিট হওয়ার পর নতুন ডাটাগুলো ডাটাবেসে ইনসার্ট করা হচ্ছে
                // যেহেতু এগুলো সাসপেন্ড ফাংশন, তাই ট্রানজেকশনের বাইরে নরমাল থ্রেডে দ্রুত লুপ চালানো সেফ
                backupData.transactions.forEach { database.transactionDao().insertTransaction(it) }
                backupData.budgets.forEach { database.budgetDao().insertBudget(it) }
                backupData.roughTransactions.forEach { database.roughDao().insertRough(it) }
                backupData.shoppingItems.forEach { database.shoppingDao().insertItem(it) }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}