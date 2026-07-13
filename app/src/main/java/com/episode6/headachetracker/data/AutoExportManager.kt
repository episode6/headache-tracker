package com.episode6.headachetracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.skip
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(AppScope::class)
class AutoExportManager(
    private val context: Context,
    private val dao: HeadacheDao,
    private val backupManager: HeadacheBackupManager,
    private val settingsRepository: SettingsRepository,
    private val appScope: CoroutineScope,
) {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun startObserving() {
        appScope.launch(Dispatchers.IO.limitedParallelism(1)) {
            settingsRepository.autoExportUri
                .filterNotNull()
                .combine(dao.getAllEntries().drop(1).distinctUntilChanged()) { uriString, _ -> uriString }
                .debounce(1.seconds)
                .collectLatest { performExport(it.toUri()) }
        }
    }

    private suspend fun performExport(uri: Uri) {
        val result = backupManager.exportTo(uri)
        if (result is BackupResult.Success) {
            settingsRepository.setLastAutoExportTimestamp(Instant.now().toEpochMilli())
        }
    }

    fun takePersistablePermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }
}
