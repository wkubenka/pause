package com.astute.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.pause.data.local.entity.Tier
import kotlinx.coroutines.flow.Flow

@Dao
interface TierDao {
    @Query("SELECT * FROM tiers ORDER BY minCents, id")
    fun observeAll(): Flow<List<Tier>>

    @Query("SELECT * FROM tiers ORDER BY minCents, id")
    suspend fun all(): List<Tier>

    @Query("SELECT * FROM tiers WHERE id = :id")
    suspend fun get(id: Long): Tier?

    @Insert suspend fun insert(tier: Tier): Long
    @Update suspend fun update(tier: Tier)
    @Delete suspend fun delete(tier: Tier)
}
