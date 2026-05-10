package com.almamun252.nikhuthisab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.nikhuthisab.data.AppDatabase
import com.almamun252.nikhuthisab.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    // ডাটাবেস থেকে বাজেট ডাও (DAO) ইনিশিয়ালাইজ করা হচ্ছে
    private val budgetDao = AppDatabase.getDatabase(application).budgetDao()

    /**
     * নির্দিষ্ট মাসের বাজেট লিস্ট পাওয়ার জন্য
     * @param monthYear উদাহরণ: "05-2024" (মাস-বছর)
     */
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    /**
     * নতুন বাজেট যোগ বা বিদ্যমান বাজেট আপডেট করার জন্য
     */
    fun insertBudget(budget: Budget) {
        viewModelScope.launch {
            budgetDao.insertBudget(budget)
        }
    }

    /**
     * কোনো বাজেট ডিলিট করার জন্য
     */
    fun deleteBudget(budgetId: Int) {
        viewModelScope.launch {
            budgetDao.deleteBudget(budgetId)
        }
    }
}