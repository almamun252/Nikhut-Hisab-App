package com.almamun252.nikhuthisab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.almamun252.nikhuthisab.model.Transaction

// ভার্সন ১ থেকে ২ করা হলো
@Database(entities = [Transaction::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Safe Migration Logic (অ্যাপ ক্র্যাশ রোধ করার জন্য)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // "transactions" টেবিলে নতুন ৪টি কলাম যোগ করা হচ্ছে
                database.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                val currentTime = System.currentTimeMillis()
                database.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nikhut_hisab_database"
                )
                    .addMigrations(MIGRATION_1_2) // এখানে মাইগ্রেশন যুক্ত করা হলো
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}