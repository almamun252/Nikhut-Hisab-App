package com.almamun252.nikhuthisab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ১. নতুন হিসাব ডেটাবেসে সেভ করার জন্য
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    // ২. কোনো হিসাব আপডেট বা এডিট করার জন্য
    @Update
    suspend fun updateTransaction(transaction: Transaction)

    // ৩. কোনো হিসাব ডিলিট করার জন্য
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // ৪. সবগুলো হিসাব একসাথে দেখার জন্য (নতুন তারিখগুলো আগে দেখাবে)
    // Note: transaction_table এর জায়গায় transactions দেওয়া হয়েছে
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
}