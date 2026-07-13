package com.episode6.headachetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "headache_entries")
data class HeadacheEntry(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val intensity: Int, // 0 to 3
    val pillsTaken: Int = 0, // 0 to 2
    val firstPillTime: Long? = null, // Epoch millis; null unless pillsTaken >= 1
    val secondPillTime: Long? = null // Epoch millis; null unless pillsTaken == 2
)
