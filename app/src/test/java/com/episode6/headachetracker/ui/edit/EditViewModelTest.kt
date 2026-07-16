package com.episode6.headachetracker.ui.edit

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.data.SecondPillReminderScheduler
import com.episode6.headachetracker.data.SettingsRepository
import com.episode6.headachetracker.model.HeadacheEntry
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dao = FakeHeadacheDao()
    private val scheduler = FakeSecondPillReminderScheduler()
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

    // OTHER_DATE default keeps pre-existing tests clear of the reminder logic,
    // which only engages when the edited date matches today()
    private fun viewModel(now: Long = NOW, todayDate: String = OTHER_DATE) =
        EditViewModel(DATE, dao, settingsRepository, scheduler).apply {
            clock = { now }
            today = { LocalDate.parse(todayDate) }
        }

    @Test
    fun `selecting 1 pill defaults first pill time to now`() = runTest {
        val vm = viewModel()

        vm.onPillsTakenChanged(1)

        assertEquals(NOW, vm.state.value.firstPillTime)
        assertNull(vm.state.value.secondPillTime)
    }

    @Test
    fun `selecting 2 pills defaults both pill times to now`() = runTest {
        val vm = viewModel()

        vm.onPillsTakenChanged(2)

        assertEquals(NOW, vm.state.value.firstPillTime)
        assertEquals(NOW, vm.state.value.secondPillTime)
    }

    @Test
    fun `selecting 2 pills keeps an existing first pill time`() = runTest {
        val vm = viewModel()
        vm.onFirstPillTimeChanged(EARLIER)

        vm.onPillsTakenChanged(2)

        assertEquals(EARLIER, vm.state.value.firstPillTime)
        assertEquals(NOW, vm.state.value.secondPillTime)
    }

    @Test
    fun `re-selecting a pill count does not overwrite an existing time`() = runTest {
        val vm = viewModel()
        vm.onFirstPillTimeChanged(EARLIER)

        vm.onPillsTakenChanged(1)

        assertEquals(EARLIER, vm.state.value.firstPillTime)
    }

    @Test
    fun `loading an existing entry populates pill times`() = runTest {
        dao.entry = HeadacheEntry(
            date = DATE,
            intensity = 2,
            pillsTaken = 2,
            firstPillTime = EARLIER,
            secondPillTime = NOW
        )

        val vm = viewModel()

        assertEquals(EARLIER, vm.state.value.firstPillTime)
        assertEquals(NOW, vm.state.value.secondPillTime)
    }

    @Test
    fun `saving with 1 pill selected drops the second pill time`() = runTest {
        val vm = viewModel()
        vm.onPillsTakenChanged(2)
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}

        assertEquals(NOW, dao.saved?.firstPillTime)
        assertNull(dao.saved?.secondPillTime)
    }

    @Test
    fun `loading an existing entry populates notes`() = runTest {
        dao.entry = HeadacheEntry(date = DATE, intensity = 1, notes = "started after lunch")

        val vm = viewModel()

        assertEquals("started after lunch", vm.state.value.notes)
    }

    @Test
    fun `saving trims notes`() = runTest {
        val vm = viewModel()
        vm.onNotesChanged("  started after lunch  ")

        vm.saveEntry {}

        assertEquals("started after lunch", dao.saved?.notes)
    }

    @Test
    fun `saving blank notes stores null`() = runTest {
        val vm = viewModel()
        vm.onNotesChanged("   ")

        vm.saveEntry {}

        assertNull(dao.saved?.notes)
    }

    @Test
    fun `saving with no pills selected drops both pill times`() = runTest {
        val vm = viewModel()
        vm.onPillsTakenChanged(2)
        vm.onPillsTakenChanged(0)

        vm.saveEntry {}

        assertNull(dao.saved?.firstPillTime)
        assertNull(dao.saved?.secondPillTime)
    }

    @Test
    fun `saving today with 1 pill schedules reminder at first pill time plus default delay`() = runTest {
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}

        assertEquals(NOW + 60 * MINUTE_MILLIS, scheduler.scheduledAt)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `reminder delay honors the stored setting`() = runTest {
        settingsRepository.setSecondPillReminderMinutes(45)
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}

        assertEquals(NOW + 45 * MINUTE_MILLIS, scheduler.scheduledAt)
    }

    @Test
    fun `reminder delay setting is clamped to the minimum`() = runTest {
        settingsRepository.setSecondPillReminderMinutes(5)
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}

        assertEquals(NOW + 45 * MINUTE_MILLIS, scheduler.scheduledAt)
    }

    @Test
    fun `saving today with 1 pill taken long ago cancels instead of scheduling`() = runTest {
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(1)
        vm.onFirstPillTimeChanged(NOW - 120 * MINUTE_MILLIS)

        vm.saveEntry {}

        assertNull(scheduler.scheduledAt)
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `saving today with 2 pills cancels the reminder`() = runTest {
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(2)

        vm.saveEntry {}

        assertNull(scheduler.scheduledAt)
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `saving today with no pills cancels the reminder`() = runTest {
        val vm = viewModel(todayDate = DATE)

        vm.saveEntry {}

        assertNull(scheduler.scheduledAt)
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `saving a past date with 1 pill never touches the reminder`() = runTest {
        val vm = viewModel()
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}

        assertEquals(0, scheduler.scheduleCount)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `re-saving today with 1 pill reschedules to the same time`() = runTest {
        val vm = viewModel(todayDate = DATE)
        vm.onPillsTakenChanged(1)

        vm.saveEntry {}
        vm.saveEntry {}

        assertEquals(2, scheduler.scheduleCount)
        assertEquals(NOW + 60 * MINUTE_MILLIS, scheduler.scheduledAt)
    }

    private companion object {
        const val DATE = "2026-07-11"
        const val OTHER_DATE = "2026-07-10"
        const val NOW = 1_752_240_000_000L
        const val EARLIER = 1_752_230_000_000L
        const val MINUTE_MILLIS = 60_000L
    }
}

private class FakeHeadacheDao(var entry: HeadacheEntry? = null) : HeadacheDao {
    var saved: HeadacheEntry? = null

    override fun getAllEntries(): Flow<List<HeadacheEntry>> = flowOf(emptyList())
    override suspend fun getAllEntriesOnce(): List<HeadacheEntry> = emptyList()
    override suspend fun getEntryByDate(date: String): HeadacheEntry? = entry
    override suspend fun upsertEntry(entry: HeadacheEntry) {
        saved = entry
    }
    override suspend fun upsertEntries(entries: List<HeadacheEntry>) {}
    override suspend fun deleteEntryByDate(date: String) {}
}

private class FakeSecondPillReminderScheduler : SecondPillReminderScheduler {
    var scheduledAt: Long? = null
    var scheduleCount = 0
    var cancelCount = 0

    override fun schedule(fireAtMillis: Long) {
        scheduledAt = fireAtMillis
        scheduleCount++
    }

    override fun cancel() {
        cancelCount++
    }
}
