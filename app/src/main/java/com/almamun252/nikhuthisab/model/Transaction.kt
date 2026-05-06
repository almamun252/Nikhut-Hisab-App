package com.almamun252.nikhuthisab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "Income", "Expense", "Lending", "Borrowing"
    val category: String,
    val date: Long,
    val note: String? = null, // <-- শর্ট নোট

    // --- দেনা-পাওনা ফিচারের জন্য নতুন যোগ করা ফিল্ডসমূহ ---
    val dueDate: Long? = null,      // টাকা ফেরত দেওয়ার/পাওয়ার সম্ভাব্য তারিখ (ডেডলাইন)
    val settledAmount: Double = 0.0, // এ পর্যন্ত কত টাকা আংশিক বা সম্পূর্ণ পরিশোধ করা হয়েছে

    // --- অনলাইন ব্যাকআপ (Sync) এর জন্য নতুন ফিল্ডসমূহ ---
    val userId: String = "",               // ইউজারের ফায়ারবেস UID
    val isSynced: Boolean = false,         // সার্ভারে আপলোড হয়েছে কি না
    val isDeleted: Boolean = false,        // ইউজার ডিলিট করেছে কি না (Soft Delete)
    val updatedAt: Long = System.currentTimeMillis() // সর্বশেষ আপডেটের সময়
)