package com.episode6.headachetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.episode6.headachetracker.model.HeadacheEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadacheDao {
    @Query("SELECT * FROM headache_entries")
    fun getAllEntries(): Flow<List<HeadacheEntry>>

    @Query("SELECT * FROM headache_entries")
    suspend fun getAllEntriesOnce(): List<HeadacheEntry>

    @Query("SELECT * FROM headache_entries WHERE date = :date")
    suspend fun getEntryByDate(date: String): HeadacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: HeadacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<HeadacheEntry>)

    @Query("DELETE FROM headache_entries WHERE date = :date")
    suspend fun deleteEntryByDate(date: String)
}
