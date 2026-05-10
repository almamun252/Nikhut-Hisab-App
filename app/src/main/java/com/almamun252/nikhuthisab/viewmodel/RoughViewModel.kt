package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.model.RoughTransaction
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.launch

class RoughViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val roughDao = db.roughDao()
    private val transactionDao = db.transactionDao()

    val allRoughTransactions = roughDao.getAllRough()

    fun insertRough(rough: RoughTransaction) = viewModelScope.launch { roughDao.insertRough(rough) }

    fun deleteRough(id: Int) = viewModelScope.launch { roughDao.deleteRough(id) }

    fun clearAll() = viewModelScope.launch { roughDao.clearAllRough() }

    // 🔥 সেই কিলার ফিচার: মুভ টু মেইন খরচের খাতা
    fun moveToMain(rough: RoughTransaction) = viewModelScope.launch {
        // ১. মেইন ট্রানজ্যাকশন মডেলে কনভার্ট করা
        val mainExpense = Transaction(
            title = rough.title,
            amount = rough.amount,
            category = "অন্যান্য", // ডিফল্ট ক্যাটাগরি
            type = "Expense",
            date = System.currentTimeMillis()
        )
        // ২. মেইন ডাটাবেসে সেভ করা
        transactionDao.insertTransaction(mainExpense)

        // ৩. রাফ খাতায় 'Moved' হিসেবে আপডেট করা
        roughDao.updateRough(rough.copy(isMovedToMain = 1))
    }
}