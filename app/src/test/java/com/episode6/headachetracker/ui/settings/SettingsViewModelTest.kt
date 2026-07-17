package com.episode6.headachetracker.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.episode6.headachetracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        settingsRepository = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { tmpFolder.newFile("test.preferences_pb") },
            )
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads the default of 60 minutes`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        assertTrue(vm.state.value.isLoaded)
        assertEquals("60", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `initial state loads a previously stored value`() = runTest {
        settingsRepository.setSecondPillReminderMinutes(90)

        val vm = SettingsViewModel(settingsRepository)

        assertEquals("90", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `changing minutes persists the value`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onReminderMinutesChanged("90")

        assertEquals(90, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `values below the minimum are clamped to 45`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onReminderMinutesChanged("10")

        assertEquals("10", vm.state.value.reminderMinutesText)
        assertEquals(45, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `values above the maximum are clamped to 150`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onReminderMinutesChanged("500")

        assertEquals(150, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `non-numeric input is filtered and not persisted`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onReminderMinutesChanged("abc")

        assertEquals("", vm.state.value.reminderMinutesText)
        assertEquals(60, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `input longer than 4 digits is truncated`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onReminderMinutesChanged("123456")

        assertEquals("1234", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `morning check-in defaults to enabled at 8am`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        assertTrue(vm.state.value.morningCheckInEnabled)
        assertEquals(8 * 60, vm.state.value.morningCheckInTimeMinutes)
    }

    @Test
    fun `toggling morning check-in off persists`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onMorningCheckInToggled(false)

        assertFalse(vm.state.value.morningCheckInEnabled)
        assertFalse(settingsRepository.morningCheckInEnabled.first())
    }

    @Test
    fun `changing the check-in time persists minutes of day`() = runTest {
        val vm = SettingsViewModel(settingsRepository)

        vm.onMorningCheckInTimeChanged(hour = 6, minute = 45)

        assertEquals(6 * 60 + 45, vm.state.value.morningCheckInTimeMinutes)
        assertEquals(6 * 60 + 45, settingsRepository.morningCheckInTimeMinutes.first())
    }
}
