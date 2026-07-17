package com.episode6.headachetracker.data

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MorningCheckInIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerMorningCheckInScheduler
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerMorningCheckInScheduler(context)
        settings = SettingsRepository(context.settingsDataStore)
        runBlocking { settings.setMorningCheckInEnabled(true) }
        NotificationManagerCompat.from(context).cancelAll()
    }

    @After
    fun tearDown() {
        scheduler.cancel()
        runBlocking { settings.setMorningCheckInEnabled(true) }
        NotificationManagerCompat.from(context).cancelAll()
    }

    @Test
    fun scheduleNext_enqueuesSingleUniqueWork() {
        scheduler.scheduleNext(8 * 60)
        scheduler.scheduleNext(9 * 60)

        val infos = pendingWork()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos[0].state)
    }

    @Test
    fun worker_postsNotificationAndReschedulesNextDay() {
        scheduler.scheduleNext(8 * 60)
        val workId = pendingWork()[0].id

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(workId)

        assertEquals(WorkInfo.State.SUCCEEDED, awaitFinished(workId).state)
        assertEquals(1, activeCheckInNotifications())
        // the worker re-chains itself: a fresh ENQUEUED item must exist under the same name
        val pending = pendingWork()
        assertEquals(1, pending.size)
        assertTrue(pending[0].id != workId)
    }

    @Test
    fun worker_postsNothingWhenDisabled() {
        runBlocking { settings.setMorningCheckInEnabled(false) }
        scheduler.scheduleNext(8 * 60)
        val workId = pendingWork()[0].id

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(workId)

        assertEquals(WorkInfo.State.SUCCEEDED, awaitFinished(workId).state)
        assertEquals(0, activeCheckInNotifications())
        // disabled worker must not re-chain either
        assertEquals(0, pendingWork().size)
    }

    @Test
    fun cancel_removesPendingWork() {
        scheduler.scheduleNext(8 * 60)

        scheduler.cancel()

        val infos = workManager
            .getWorkInfosForUniqueWork(WorkManagerMorningCheckInScheduler.WORK_NAME)
            .get()
        assertTrue(infos.all { it.state == WorkInfo.State.CANCELLED })
    }

    // CoroutineWorker runs on Dispatchers.Default even with a SynchronousExecutor,
    // so completion must be awaited rather than asserted immediately
    private fun awaitFinished(id: java.util.UUID): WorkInfo {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            val info = workManager.getWorkInfoById(id).get()
            if (info != null && info.state.isFinished) return info
            Thread.sleep(50)
        }
        throw AssertionError("work $id did not finish within 10s")
    }

    private fun pendingWork(): List<WorkInfo> = workManager
        .getWorkInfosForUniqueWork(WorkManagerMorningCheckInScheduler.WORK_NAME)
        .get()
        .filter { !it.state.isFinished }

    private fun activeCheckInNotifications(): Int = context
        .getSystemService(NotificationManager::class.java)
        .activeNotifications
        .count { it.id == MorningCheckInWorker.NOTIFICATION_ID }
}
