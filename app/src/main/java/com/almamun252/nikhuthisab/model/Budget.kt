package com.almamun252.nikhuthisab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val limitAmount: Double,
    val monthYear: String, // উদাহরণ: "05-2024"
    val isSynced: Int = 0
)