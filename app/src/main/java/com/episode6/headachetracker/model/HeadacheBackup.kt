package com.episode6.headachetracker.model

import kotlinx.serialization.Serializable

const val HEADACHE_BACKUP_VERSION = 1

@Serializable
data class HeadacheBackup(
    val version: Int = HEADACHE_BACKUP_VERSION,
    val exportedAt: String,
    val entries: List<HeadacheBackupEntry>,
)

@Serializable
data class HeadacheBackupEntry(
    val date: String,
    val intensity: Int,
    val pillsTaken: Int = 0,
    val firstPillTime: Long? = null,
    val secondPillTime: Long? = null,
)

fun HeadacheEntry.toBackupEntry() = HeadacheBackupEntry(
    date = date,
    intensity = intensity,
    pillsTaken = pillsTaken,
    firstPillTime = firstPillTime,
    secondPillTime = secondPillTime,
)

fun HeadacheBackupEntry.toHeadacheEntry() = HeadacheEntry(
    date = date,
    intensity = intensity,
    pillsTaken = pillsTaken,
    firstPillTime = firstPillTime,
    secondPillTime = secondPillTime,
)
