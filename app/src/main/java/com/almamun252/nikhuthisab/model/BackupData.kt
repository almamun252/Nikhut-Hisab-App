package com.almamun252.nikhuthisab.model

import androidx.annotation.Keep

@Keep
data class BackupData(
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val roughTransactions: List<RoughTransaction> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val backupTimestamp: Long = System.currentTimeMillis() // ব্যাকআপের সময় মনে রাখার জন্য
)