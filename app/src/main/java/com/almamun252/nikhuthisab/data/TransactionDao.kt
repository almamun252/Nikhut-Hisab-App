package com.almamun252.nikhuthisab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ১. নতুন হিসাব ডেটাবেসে সেভ করার জন্য
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // ২. কোনো হিসাব আপডেট বা এডিট করার জন্য
    @Update
    suspend fun updateTransaction(transaction: Transaction)

    // ৩. কোনো হিসাব ডাটাবেস থেকে একেবারে মুছে ফেলার জন্য (Hard Delete)
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // ৪. সবগুলো হিসাব একসাথে দেখার জন্য (যেগুলো ইউজার ডিলিট করেনি সেগুলোই শুধু দেখাবে)
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // ৫. বর্তমান ব্যালেন্স (Total Balance) বের করার স্মার্ট কোয়েরি
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type = 'Income' THEN amount ELSE 0.0 END) - 
            SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0.0 END), 
        0.0) 
        FROM transactions 
        WHERE isDeleted = 0
    """)
    fun getCurrentBalance(): Flow<Double>

    // ৬. নির্দিষ্ট কোনো মাসের বা সময়ের ব্যালেন্স দেখার জন্য (যেমন: শুধু মে মাসের ব্যালেন্স)
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type = 'Income' THEN amount ELSE 0.0 END) - 
            SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0.0 END), 
        0.0) 
        FROM transactions 
        WHERE isDeleted = 0 AND date BETWEEN :startDate AND :endDate
    """)
    fun getBalanceForPeriod(startDate: Long, endDate: Long): Flow<Double>

    // ৭. অনলাইন সিঙ্ক (Hostinger) এর জন্য: যেসব ডেটা এখনো সার্ভারে যায়নি সেগুলো খুঁজে বের করার কোয়েরি
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>
}