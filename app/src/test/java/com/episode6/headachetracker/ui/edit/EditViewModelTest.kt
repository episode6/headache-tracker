package com.episode6.headachetracker.ui.edit

import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.model.HeadacheEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    private val dao = FakeHeadacheDao()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(now: Long = NOW) = EditViewModel(DATE, dao).apply {
        clock = { now }
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
    fun `saving with no pills selected drops both pill times`() = runTest {
        val vm = viewModel()
        vm.onPillsTakenChanged(2)
        vm.onPillsTakenChanged(0)

        vm.saveEntry {}

        assertNull(dao.saved?.firstPillTime)
        assertNull(dao.saved?.secondPillTime)
    }

    private companion object {
        const val DATE = "2026-07-11"
        const val NOW = 1_752_240_000_000L
        const val EARLIER = 1_752_230_000_000L
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
