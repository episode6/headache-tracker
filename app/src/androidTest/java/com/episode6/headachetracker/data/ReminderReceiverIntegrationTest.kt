package com.episode6.headachetracker.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderReceiverIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settings = SettingsRepository(context.settingsDataStore)
        runBlocking {
            settings.setMorningCheckInEnabled(true)
            settings.setPendingSecondPillReminderAt(null)
        }
        NotificationManagerCompat.from(context).cancelAll()
    }

    @After
    fun tearDown() {
        runBlocking {
            settings.setMorningCheckInEnabled(true)
            settings.setPendingSecondPillReminderAt(null)
        }
        AlarmManagerMorningCheckInScheduler(context).cancel()
        NotificationManagerCompat.from(context).cancelAll()
    }

    @Test
    fun exactAlarmPermission_isGrantedOnDevice() {
        // sanity-check the USE_EXACT_ALARM manifest declaration actually grants access
        assertTrue(context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())
    }

    @Test
    fun secondPillReceiver_postsNotificationAndClearsPersistedState() = runBlocking {
        settings.setPendingSecondPillReminderAt(System.currentTimeMillis())

        SecondPillReminderReceiver().handle(context)

        awaitNotificationCount(SecondPillReminderReceiver.NOTIFICATION_ID, 1)
        assertNull(settings.pendingSecondPillReminderAt.first())
    }

    @Test
    fun secondPillScheduler_persistsAndClearsFireTime() = runBlocking {
        val scheduler = AlarmManagerSecondPillReminderScheduler(context, settings)
        val fireAt = System.currentTimeMillis() + 60 * 60_000L

        scheduler.schedule(fireAt)
        assertEquals(fireAt, settings.pendingSecondPillReminderAt.first())

        scheduler.cancel()
        assertNull(settings.pendingSecondPillReminderAt.first())
    }

    @Test
    fun morningCheckInReceiver_postsNotificationWhenEnabled() = runBlocking {
        MorningCheckInReceiver().handle(context)

        awaitNotificationCount(MorningCheckInReceiver.NOTIFICATION_ID, 1)
    }

    @Test
    fun morningCheckInReceiver_postsNothingWhenDisabled() = runBlocking {
        settings.setMorningCheckInEnabled(false)

        MorningCheckInReceiver().handle(context)

        // notification posting is async, so an immediate zero-count is trivially true;
        // give a would-be notification a grace period to appear before asserting absence
        Thread.sleep(NEGATIVE_GRACE_MS)
        assertEquals(0, activeNotifications(MorningCheckInReceiver.NOTIFICATION_ID))
    }

    // NotificationManager.notify posts asynchronously — asserting the active list right
    // after a receiver returns is a race that flakes on slow CI emulators, so poll
    private fun awaitNotificationCount(id: Int, expected: Int) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        var count = activeNotifications(id)
        while (count != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
            count = activeNotifications(id)
        }
        assertEquals(expected, count)
    }

    private fun activeNotifications(id: Int): Int = context
        .getSystemService(NotificationManager::class.java)
        .activeNotifications
        .count { it.id == id }

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
        const val NEGATIVE_GRACE_MS = 500L
    }
}
