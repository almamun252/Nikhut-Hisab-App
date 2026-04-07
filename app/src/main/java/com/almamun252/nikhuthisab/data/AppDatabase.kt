package com.almamun252.nikhuthisab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.almamun252.nikhuthisab.model.Transaction

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nikhut_hisab_database"
                )
                    .fallbackToDestructiveMigration() // <-- এই লাইনটি যোগ করা হয়েছে
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}