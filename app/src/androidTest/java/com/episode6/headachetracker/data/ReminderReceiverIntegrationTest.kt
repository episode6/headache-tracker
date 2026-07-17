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

        assertEquals(1, activeNotifications(SecondPillReminderReceiver.NOTIFICATION_ID))
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

        assertEquals(1, activeNotifications(MorningCheckInReceiver.NOTIFICATION_ID))
    }

    @Test
    fun morningCheckInReceiver_postsNothingWhenDisabled() = runBlocking {
        settings.setMorningCheckInEnabled(false)

        MorningCheckInReceiver().handle(context)

        assertEquals(0, activeNotifications(MorningCheckInReceiver.NOTIFICATION_ID))
    }

    private fun activeNotifications(id: Int): Int = context
        .getSystemService(NotificationManager::class.java)
        .activeNotifications
        .count { it.id == id }
}
