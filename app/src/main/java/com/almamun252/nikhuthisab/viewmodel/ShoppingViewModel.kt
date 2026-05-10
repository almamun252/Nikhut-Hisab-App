package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.model.ShoppingItem
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.launch

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val shoppingDao = db.shoppingDao()
    private val transactionDao = db.transactionDao()

    val allShoppingItems = shoppingDao.getAllItems()

    fun insertItem(item: ShoppingItem) = viewModelScope.launch { shoppingDao.insertItem(item) }

    fun updateItem(item: ShoppingItem) = viewModelScope.launch { shoppingDao.updateItem(item) }

    fun deleteItem(id: Int) = viewModelScope.launch { shoppingDao.deleteItem(id) }

    fun clearAll() = viewModelScope.launch { shoppingDao.clearAll() }

    // একাধিক আইটেম ডিলিট (সিলেকশন মোড থেকে)
    fun deleteMultipleItems(itemIds: List<Int>) = viewModelScope.launch {
        shoppingDao.deleteItems(itemIds)
    }

    // 🔥 সেই কিলার ফিচার: আইটেম মার্জ (Merge) করা
    fun mergeItems(itemsToMerge: List<ShoppingItem>, newTitle: String, totalActualPrice: Double) = viewModelScope.launch {
        // ১. নোট তৈরি: আইটেমের নামগুলো কমা দিয়ে যুক্ত করা
        val generatedNote = itemsToMerge.joinToString(", ") { it.name }

        // ২. নতুন মার্জ করা আইটেম তৈরি
        val mergedItem = ShoppingItem(
            name = newTitle,
            estimatedPrice = 0.0,
            actualPrice = totalActualPrice,
            note = generatedNote,
            isPurchased = true, // মার্জ করার মানেই হলো কেনা হয়ে গেছে
            isMovedToMain = false,
            dateAdded = System.currentTimeMillis()
        )

        // ৩. নতুন আইটেম সেভ করা
        shoppingDao.insertItem(mergedItem)

        // ৪. পুরনো আইটেমগুলো ডিলিট করে দেওয়া
        shoppingDao.deleteItems(itemsToMerge.map { it.id })
    }

    // 🔥 মূল হিসাবে পাঠানো (Transfer to Main)
    fun moveToMain(item: ShoppingItem, category: String) = viewModelScope.launch {
        val finalTitle = if (item.note.isNotBlank()) "${item.name} (${item.note})" else item.name

        val mainExpense = Transaction(
            title = finalTitle,
            amount = item.actualPrice,
            category = category,
            type = "Expense",
            date = System.currentTimeMillis()
        )

        // মেইন ডাটাবেসে সেভ করা
        transactionDao.insertTransaction(mainExpense)

        // বাজারের ফর্দে 'Moved' হিসেবে আপডেট করা
        shoppingDao.updateItem(item.copy(isMovedToMain = true))
    }
}