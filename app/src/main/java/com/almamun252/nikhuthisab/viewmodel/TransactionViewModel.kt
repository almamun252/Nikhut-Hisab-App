package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.data.TransactionRepository
import com.almamun252.nikhuthisab.model.Transaction
import com.almamun252.nikhuthisab.worker.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: StateFlow<List<Transaction>>

    // ইন-অ্যাপ নোটিফিকেশনের জন্য আসন্ন রিমাইন্ডারগুলোর লিস্ট
    val upcomingReminders: StateFlow<List<Transaction>>

    init {
        // ডেটাবেস এবং ডাও (Dao) ইনিশিয়ালাইজ করা হচ্ছে
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)

        // Flow-কে StateFlow-তে কনভার্ট করা হচ্ছে, যাতে Compose UI সহজে ডেটা পড়তে পারে
        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // ৩ দিনের মধ্যে যেসব ডেডলাইন আছে বা পার হয়ে গেছে, সেগুলো ফিল্টার করা
        upcomingReminders = repository.allTransactions.map { transactions ->
            val currentTime = System.currentTimeMillis()
            val threeDaysLater = currentTime + TimeUnit.DAYS.toMillis(3)

            transactions.filter { tx ->
                (tx.type == "Lending" || tx.type == "Borrowing") &&
                        tx.dueDate != null &&
                        tx.dueDate <= threeDaysLater && // ৩ দিনের মধ্যে বা পার হয়ে গেছে
                        tx.settledAmount < tx.amount // এখনো পুরোপুরি শোধ হয়নি
            }.sortedBy { it.dueDate } // যেটার ডেট আগে, সেটা লিস্টের ওপরে থাকবে
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // সেভ বাটনে ক্লিক করলে এই ফাংশনটি কল হবে
    fun insertTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }

    // ডিলিট করার ফাংশন
    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)

        // লেনদেন ডিলিট হলে সাথে সাথে তার ব্যাকগ্রাউন্ড নোটিফিকেশন অ্যালার্মটিও ক্যানসেল করে দেওয়া
        val reminderId = if (transaction.id != 0) transaction.id else transaction.hashCode()
        ReminderScheduler.cancelReminder(getApplication(), reminderId)
    }
}