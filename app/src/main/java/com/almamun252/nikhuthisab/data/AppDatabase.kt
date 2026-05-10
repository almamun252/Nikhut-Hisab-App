package com.almamun252.nikhuthisab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.almamun252.nikhuthisab.model.Budget
import com.almamun252.nikhuthisab.model.RoughTransaction
import com.almamun252.nikhuthisab.model.ShoppingItem
import com.almamun252.nikhuthisab.model.Transaction

// ভার্সন 6 করা হলো
@Database(entities = [Transaction::class, Budget::class, RoughTransaction::class, ShoppingItem::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun roughDao(): RoughDao
    abstract fun shoppingDao(): ShoppingDao // নতুন ডাও যুক্ত করা হলো

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // মাইগ্রেশন (১ থেকে ২)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                val currentTime = System.currentTimeMillis()
                database.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime")
            }
        }

        // মাইগ্রেশন (২ থেকে ৩) - বাজেট টেবিল
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `limitAmount` REAL NOT NULL, 
                        `monthYear` TEXT NOT NULL, 
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        // মাইগ্রেশন (৩ থেকে ৪) - রাফ খাতা টেবিল
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rough_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `isMovedToMain` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        // মাইগ্রেশন (৪ থেকে ৫) - ক্যাটাগরি এবং নোট যোগ করা
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rough_transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'অন্যান্য'")
                database.execSQL("ALTER TABLE rough_transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        // নতুন মাইগ্রেশন (৫ থেকে ৬) - বাজারের ফর্দ টেবিল তৈরি করার জন্য
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shopping_list` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `estimatedPrice` REAL NOT NULL DEFAULT 0.0, 
                        `actualPrice` REAL NOT NULL DEFAULT 0.0, 
                        `note` TEXT NOT NULL DEFAULT '', 
                        `isPurchased` INTEGER NOT NULL DEFAULT 0,
                        `isMovedToMain` INTEGER NOT NULL DEFAULT 0,
                        `dateAdded` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nikhut_hisab_database"
                )
                    // এখানে নতুন MIGRATION_5_6 যুক্ত করা হলো
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}