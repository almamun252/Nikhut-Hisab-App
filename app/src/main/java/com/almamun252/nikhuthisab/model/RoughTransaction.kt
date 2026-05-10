package com.almamun252.nikhuthisab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rough_transactions")
data class RoughTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // নতুন যোগ করা হলো
    val note: String,     // নতুন যোগ করা হলো
    val date: Long,
    val isMovedToMain: Int = 0
)