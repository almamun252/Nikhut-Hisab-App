package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.R
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
        val catOtherStr = getApplication<Application>().getString(R.string.cat_other)

        // Reflection ব্যবহার করে ক্যাটাগরি ও নোট আনা হচ্ছে (যাতে পুরানো ভার্সনের সাথে ক্র্যাশ না করে)
        val categoryName = try { rough.javaClass.getMethod("getCategory").invoke(rough) as? String } catch (e: Exception) { null }
        val noteText = try { rough.javaClass.getMethod("getNote").invoke(rough) as? String } catch (e: Exception) { null }

        // ১. মেইন ট্রানজ্যাকশন মডেলে কনভার্ট করা
        val mainExpense = Transaction(
            title = rough.title,
            amount = rough.amount,
            category = categoryName?.ifEmpty { catOtherStr } ?: catOtherStr, // রাফ খাতার ক্যাটাগরি থাকলে সেটি বসবে, না থাকলে ডিফল্ট
            type = "Expense",
            date = System.currentTimeMillis(),
            note = noteText?.ifEmpty { null } // রাফ খাতার নোট থাকলে সেটিও মেইন খাতায় চলে যাবে
        )
        // ২. মেইন ডাটাবেসে সেভ করা
        transactionDao.insertTransaction(mainExpense)

        // ৩. রাফ খাতায় 'Moved' হিসেবে আপডেট করা
        roughDao.updateRough(rough.copy(isMovedToMain = 1))
    }
}