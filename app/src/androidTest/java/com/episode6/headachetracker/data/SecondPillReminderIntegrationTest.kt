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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecondPillReminderIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerSecondPillReminderScheduler

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
        scheduler = WorkManagerSecondPillReminderScheduler(context)
        NotificationManagerCompat.from(context).cancelAll()
    }

    @After
    fun tearDown() {
        scheduler.cancel()
        NotificationManagerCompat.from(context).cancelAll()
    }

    @Test
    fun schedule_enqueuesUniqueWorkWithInitialDelay() {
        scheduler.schedule(System.currentTimeMillis() + 60 * 60_000L)

        val infos = pendingWork()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos[0].state)
        // delay pending means the worker must not have run yet
        assertEquals(0, activeReminderNotifications())
    }

    @Test
    fun schedule_replacesExistingWorkInsteadOfStackingIt() {
        scheduler.schedule(System.currentTimeMillis() + 60 * 60_000L)
        val firstId = pendingWork()[0].id

        scheduler.schedule(System.currentTimeMillis() + 90 * 60_000L)

        val infos = pendingWork()
        assertEquals(1, infos.size)
        assertTrue(infos[0].id != firstId)
        assertEquals(WorkInfo.State.ENQUEUED, infos[0].state)
    }

    @Test
    fun cancel_removesPendingWork() {
        scheduler.schedule(System.currentTimeMillis() + 60 * 60_000L)

        scheduler.cancel()

        val infos = workManager
            .getWorkInfosForUniqueWork(WorkManagerSecondPillReminderScheduler.WORK_NAME)
            .get()
        assertTrue(infos.all { it.state == WorkInfo.State.CANCELLED })
        assertEquals(0, activeReminderNotifications())
    }

    @Test
    fun worker_postsReminderNotificationWhenDelayElapses() {
        scheduler.schedule(System.currentTimeMillis() + 60 * 60_000L)
        val workId = pendingWork()[0].id

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(workId)

        val info = workManager.getWorkInfoById(workId).get()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
        assertEquals(1, activeReminderNotifications())
    }

    private fun pendingWork(): List<WorkInfo> = workManager
        .getWorkInfosForUniqueWork(WorkManagerSecondPillReminderScheduler.WORK_NAME)
        .get()
        .filter { !it.state.isFinished }

    private fun activeReminderNotifications(): Int = context
        .getSystemService(NotificationManager::class.java)
        .activeNotifications
        .count { it.id == SecondPillReminderWorker.NOTIFICATION_ID }
}
