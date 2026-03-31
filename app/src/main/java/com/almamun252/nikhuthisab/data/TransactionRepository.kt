package com.almamun252.nikhuthisab.data

import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // ডেটাবেস থেকে সব ডেটা পড়ার জন্য
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // নতুন হিসাব সেভ করার জন্য
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    // কোনো হিসাব ডিলিট করার জন্য
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    // কোনো হিসাব আপডেট করার জন্য
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }
}