package com.episode6.headachetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Inject
@SingleIn(AppScope::class)
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTO_EXPORT_URI = stringPreferencesKey("auto_export_uri")
        val LAST_AUTO_EXPORT_TIMESTAMP = longPreferencesKey("last_auto_export_timestamp")
    }

    val autoExportUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.AUTO_EXPORT_URI]
    }

    val lastAutoExportTimestamp: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_AUTO_EXPORT_TIMESTAMP]
    }

    suspend fun setAutoExportUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(Keys.AUTO_EXPORT_URI)
            } else {
                preferences[Keys.AUTO_EXPORT_URI] = uri
            }
        }
    }

    suspend fun setLastAutoExportTimestamp(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(Keys.LAST_AUTO_EXPORT_TIMESTAMP)
            } else {
                preferences[Keys.LAST_AUTO_EXPORT_TIMESTAMP] = timestamp
            }
        }
    }

    suspend fun clearAutoExportSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.AUTO_EXPORT_URI)
            preferences.remove(Keys.LAST_AUTO_EXPORT_TIMESTAMP)
        }
    }
}
