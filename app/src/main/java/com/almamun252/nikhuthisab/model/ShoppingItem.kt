package com.almamun252.nikhuthisab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val estimatedPrice: Double = 0.0, // আন্দাজ করা দাম (ঐচ্ছিক)
    val actualPrice: Double = 0.0,    // আসল দাম (কেনার পর)
    val note: String = "",            // মার্জ করার পর এখানে বিস্তারিত আইটেমের নামগুলো থাকবে
    val isPurchased: Boolean = false, // কেনা হয়েছে কিনা (চেক-বক্সে টিক দিলে true হবে)
    val isMovedToMain: Boolean = false, // মূল হিসাবে পাঠানো হয়েছে কিনা
    val dateAdded: Long
)