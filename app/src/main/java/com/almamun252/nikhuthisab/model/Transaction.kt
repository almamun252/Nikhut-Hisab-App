package com.almamun252.nikhuthisab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: Long,
    val note: String? = null // <-- শর্ট নোটের জন্য নতুন ফিল্ড যোগ করা হলো
)