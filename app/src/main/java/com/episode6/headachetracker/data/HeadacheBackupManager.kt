package com.episode6.headachetracker.data

import android.content.Context
import android.net.Uri
import com.episode6.headachetracker.model.HEADACHE_BACKUP_VERSION
import com.episode6.headachetracker.model.HeadacheBackup
import com.episode6.headachetracker.model.HeadacheBackupEntry
import com.episode6.headachetracker.model.toBackupEntry
import com.episode6.headachetracker.model.toHeadacheEntry
import dev.zacsweers.metro.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface BackupResult {
    data class Success(val entryCount: Int) : BackupResult
    data class Error(val message: String) : BackupResult
}

@Inject
class HeadacheBackupManager(
    private val dao: HeadacheDao,
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun exportTo(uri: Uri): BackupResult {
        return try {
            val entries = dao.getAllEntriesOnce()
            val backup = HeadacheBackup(
                exportedAt = Instant.now().toString(),
                entries = entries.map { it.toBackupEntry() },
            )
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.writer().use { writer ->
                    writer.write(json.encodeToString(HeadacheBackup.serializer(), backup))
                }
            } ?: return BackupResult.Error("Could not open file for writing")

            BackupResult.Success(entries.size)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Export failed")
        }
    }

    suspend fun importFrom(uri: Uri): BackupResult {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return BackupResult.Error("Could not open file for reading")

            val backup = json.decodeFromString(HeadacheBackup.serializer(), content)
            if (backup.version != HEADACHE_BACKUP_VERSION) {
                return BackupResult.Error("Unsupported backup version: ${backup.version}")
            }

            val entries = backup.entries.mapIndexed { index, entry ->
                validateEntry(entry, index)
            }

            dao.upsertEntries(entries.map { it.toHeadacheEntry() })
            BackupResult.Success(entries.size)
        } catch (e: SerializationException) {
            BackupResult.Error("Invalid backup file format")
        } catch (e: IllegalArgumentException) {
            BackupResult.Error(e.message ?: "Invalid backup data")
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Import failed")
        }
    }

    private fun validateEntry(entry: HeadacheBackupEntry, index: Int): HeadacheBackupEntry {
        try {
            LocalDate.parse(entry.date)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Entry ${index + 1} has invalid date: ${entry.date}")
        }
        if (entry.intensity !in 0..3) {
            throw IllegalArgumentException("Entry ${index + 1} has invalid intensity: ${entry.intensity}")
        }
        if (entry.pillsTaken !in 0..2) {
            throw IllegalArgumentException("Entry ${index + 1} has invalid pills taken: ${entry.pillsTaken}")
        }
        return entry
    }
}
