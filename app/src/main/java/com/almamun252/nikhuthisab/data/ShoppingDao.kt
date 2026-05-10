package com.almamun252.nikhuthisab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.almamun252.nikhuthisab.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Query("SELECT * FROM shopping_list ORDER BY isPurchased ASC, id DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Query("DELETE FROM shopping_list WHERE id = :itemId")
    suspend fun deleteItem(itemId: Int)

    // একাধিক আইটেম ডিলিট করার জন্য (মার্জ করার সময় কাজে লাগবে)
    @Query("DELETE FROM shopping_list WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<Int>)

    @Query("DELETE FROM shopping_list")
    suspend fun clearAll()
}