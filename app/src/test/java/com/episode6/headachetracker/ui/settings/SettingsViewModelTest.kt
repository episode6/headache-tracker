package com.episode6.headachetracker.ui.settings

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.episode6.headachetracker.data.AutoExportManager
import com.episode6.headachetracker.data.HeadacheBackupManager
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.data.SettingsRepository
import com.episode6.headachetracker.model.HeadacheEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    private lateinit var backupManager: HeadacheBackupManager
    private lateinit var autoExportManager: AutoExportManager

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
        val context = ContextWrapper(null)
        val dao = FakeHeadacheDao()
        backupManager = HeadacheBackupManager(dao, context)
        autoExportManager = AutoExportManager(context, dao, backupManager, settingsRepository, dataStoreScope)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    // state uses stateIn(WhileSubscribed), so tests need an active collector for
    // state.value to reflect updates
    private fun TestScope.createVm(): SettingsViewModel {
        val vm = SettingsViewModel(settingsRepository, backupManager, autoExportManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `initial state loads the default of 60 minutes`() = runTest {
        val vm = createVm()

        assertTrue(vm.state.value.isLoaded)
        assertEquals("60", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `initial state loads a previously stored value`() = runTest {
        settingsRepository.setSecondPillReminderMinutes(90)

        val vm = createVm()

        assertEquals("90", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `changing minutes persists the value`() = runTest {
        val vm = createVm()

        vm.onReminderMinutesChanged("90")

        assertEquals(90, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `values below the minimum are clamped to 45`() = runTest {
        val vm = createVm()

        vm.onReminderMinutesChanged("10")

        assertEquals("10", vm.state.value.reminderMinutesText)
        assertEquals(45, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `values above the maximum are clamped to 150`() = runTest {
        val vm = createVm()

        vm.onReminderMinutesChanged("500")

        assertEquals(150, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `non-numeric input is filtered and not persisted`() = runTest {
        val vm = createVm()

        vm.onReminderMinutesChanged("abc")

        assertEquals("", vm.state.value.reminderMinutesText)
        assertEquals(60, settingsRepository.secondPillReminderMinutes.first())
    }

    @Test
    fun `input longer than 4 digits is truncated`() = runTest {
        val vm = createVm()

        vm.onReminderMinutesChanged("123456")

        assertEquals("1234", vm.state.value.reminderMinutesText)
    }

    @Test
    fun `morning check-in defaults to enabled at 8am`() = runTest {
        val vm = createVm()

        assertTrue(vm.state.value.morningCheckInEnabled)
        assertEquals(8 * 60, vm.state.value.morningCheckInTimeMinutes)
    }

    @Test
    fun `toggling morning check-in off persists`() = runTest {
        val vm = createVm()

        vm.onMorningCheckInToggled(false)

        assertFalse(vm.state.value.morningCheckInEnabled)
        assertFalse(settingsRepository.morningCheckInEnabled.first())
    }

    @Test
    fun `changing the check-in time persists minutes of day`() = runTest {
        val vm = createVm()

        vm.onMorningCheckInTimeChanged(hour = 6, minute = 45)

        assertEquals(6 * 60 + 45, vm.state.value.morningCheckInTimeMinutes)
        assertEquals(6 * 60 + 45, settingsRepository.morningCheckInTimeMinutes.first())
    }
}

private class FakeHeadacheDao : HeadacheDao {
    override fun getAllEntries(): Flow<List<HeadacheEntry>> = flowOf(emptyList())
    override suspend fun getAllEntriesOnce(): List<HeadacheEntry> = emptyList()
    override suspend fun getEntryByDate(date: String): HeadacheEntry? = null
    override suspend fun upsertEntry(entry: HeadacheEntry) {}
    override suspend fun upsertEntries(entries: List<HeadacheEntry>) {}
    override suspend fun deleteEntryByDate(date: String) {}
}
