package com.astute.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.pause.data.local.entity.CurrencyTotal
import com.astute.pause.data.local.entity.Item
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ItemDao {
    @Insert suspend fun insert(item: Item): Long
    @Update suspend fun update(item: Item)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): Item?

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<Item?>

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM items WHERE status = 'CONSIDERING' ORDER BY firstConsideredAt DESC")
    fun observeInProgress(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE status = 'WAITING' ORDER BY waitUntil ASC, id ASC")
    fun observeWaiting(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE status IN ('BOUGHT', 'SKIPPED') ORDER BY decidedAt DESC, id DESC")
    fun observeDecided(): Flow<List<Item>>

    @Query(
        "SELECT * FROM items WHERE status = 'BOUGHT' AND followupAnsweredAt IS NULL " +
            "AND followupDueAt <= :now ORDER BY followupDueAt ASC"
    )
    fun observeFollowUpsDue(now: Instant): Flow<List<Item>>

    @Query(
        "SELECT currency, SUM(costCents) AS totalCents FROM items " +
            "WHERE status = 'SKIPPED' AND costCents IS NOT NULL GROUP BY currency ORDER BY currency"
    )
    fun observeSkippedTotals(): Flow<List<CurrencyTotal>>
}
