package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.data.TransactionRepository
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: StateFlow<List<Transaction>>

    init {
        // ডেটাবেস এবং ডাও (Dao) ইনিশিয়ালাইজ করা হচ্ছে
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)

        // Flow-কে StateFlow-তে কনভার্ট করা হচ্ছে, যাতে Compose UI সহজে ডেটা পড়তে পারে
        allTransactions = repository.allTransactions.stateIn(
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
    }
}