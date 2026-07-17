package com.episode6.headachetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Inject
@SingleIn(AppScope::class)
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val AUTO_EXPORT_URI = stringPreferencesKey("auto_export_uri")
        val LAST_AUTO_EXPORT_TIMESTAMP = longPreferencesKey("last_auto_export_timestamp")
        val SECOND_PILL_REMINDER_MINUTES = intPreferencesKey("second_pill_reminder_minutes")
    }

    val autoExportUri: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.AUTO_EXPORT_URI]
    }

    val lastAutoExportTimestamp: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[Keys.LAST_AUTO_EXPORT_TIMESTAMP]
    }

    val secondPillReminderMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.SECOND_PILL_REMINDER_MINUTES] ?: DEFAULT_SECOND_PILL_REMINDER_MINUTES
    }

    suspend fun setAutoExportUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(Keys.AUTO_EXPORT_URI)
            } else {
                preferences[Keys.AUTO_EXPORT_URI] = uri
            }
        }
    }

    suspend fun setLastAutoExportTimestamp(timestamp: Long?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(Keys.LAST_AUTO_EXPORT_TIMESTAMP)
            } else {
                preferences[Keys.LAST_AUTO_EXPORT_TIMESTAMP] = timestamp
            }
        }
    }

    suspend fun setSecondPillReminderMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.SECOND_PILL_REMINDER_MINUTES] =
                minutes.coerceIn(MIN_REMINDER_MINUTES, MAX_REMINDER_MINUTES)
        }
    }

    suspend fun clearAutoExportSettings() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.AUTO_EXPORT_URI)
            preferences.remove(Keys.LAST_AUTO_EXPORT_TIMESTAMP)
        }
    }

    companion object {
        const val DEFAULT_SECOND_PILL_REMINDER_MINUTES = 60
        const val MIN_REMINDER_MINUTES = 45
        const val MAX_REMINDER_MINUTES = 150
    }
}
