package com.almamun252.nikhuthisab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.almamun252.nikhuthisab.model.RoughTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoughDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRough(transaction: RoughTransaction)

    @Update
    suspend fun updateRough(transaction: RoughTransaction)

    @Query("SELECT * FROM rough_transactions ORDER BY date DESC")
    fun getAllRough(): Flow<List<RoughTransaction>>

    @Query("DELETE FROM rough_transactions WHERE id = :id")
    suspend fun deleteRough(id: Int)

    @Query("DELETE FROM rough_transactions")
    suspend fun clearAllRough()
}