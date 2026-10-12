package com.astute.pause.ui.settings

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.TierMode
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TierEditViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var config: ConfigRepository

    @Before fun setUp() { db = TestDb.create(); config = ConfigRepository(db) }
    @After fun tearDown() { db.close() }

    private suspend fun vm(tierId: Long) =
        TierEditViewModel(SavedStateHandle(mapOf("tierId" to tierId)), config).also { v -> v.state.awaitFirst { !it.loading } }

    @Test fun loads_existing_tier_with_days_unit() = runBlocking<Unit> {
        val s = vm(3).state.value      // Large: 72h
        assertEquals("Large", s.name)
        assertEquals("100.01", s.minPrice)
        assertEquals("3", s.waitAmount)
        assertEquals(WaitUnit.DAYS, s.waitUnit)
    }

    @Test fun validation_messages() = runBlocking<Unit> {
        val vm = vm(0)
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)                             // no name
        vm.update { it.copy(name = "Pricey", minPrice = "abc") }
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)                             // bad price
        vm.update { it.copy(minPrice = "25", waitAmount = "2") }
        vm.save { error("should not save") }
        assertNotNull(vm.state.awaitFirst { it.error != null }.error)   // clashes with Medium at $25
        vm.update { it.copy(minPrice = "200", waitAmount = "400", waitUnit = WaitUnit.DAYS) }
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)                             // over a year
        vm.update { it.copy(waitAmount = "2", mode = TierMode.SHORT) }
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)                             // short list with no questions
    }

    @Test fun saves_new_tier_in_hours() = runBlocking<Unit> {
        val vm = vm(0)
        vm.update { it.copy(name = "Splurge", minPrice = "250", waitAmount = "2", waitUnit = WaitUnit.DAYS, mode = TierMode.SHORT) }
        vm.toggleQuestion(1); vm.toggleQuestion(14)
        val done = CompletableDeferred<Unit>()
        vm.save { done.complete(Unit) }
        done.await()
        val saved = config.tiers().first { it.name == "Splurge" }
        assertEquals(25_000L, saved.minCents)
        assertEquals(48, saved.waitHours)
        assertEquals(listOf(1L, 14L), saved.questionIds)
    }

    @Test fun deletes_tier() = runBlocking<Unit> {
        val vm = vm(5)
        val done = CompletableDeferred<Unit>()
        vm.delete { done.complete(Unit) }
        done.await()
        assertEquals(4, config.tiers().size)
    }

    @Test fun double_save_runs_done_once_and_creates_one_tier() = runBlocking<Unit> {
        val vm = vm(0)
        vm.update { it.copy(name = "Splurge", minPrice = "250", waitAmount = "2") }
        var calls = 0
        val done = CompletableDeferred<Unit>()
        vm.save { calls++; done.complete(Unit) }
        vm.save { calls++ }
        done.await()
        vm.save { calls++ }
        kotlinx.coroutines.delay(200)
        assertEquals(1, calls)
        assertEquals(1, config.tiers().count { it.name == "Splurge" })
    }

    @Test fun double_delete_runs_done_once() = runBlocking<Unit> {
        val vm = vm(5)
        var calls = 0
        val done = CompletableDeferred<Unit>()
        vm.delete { calls++; done.complete(Unit) }
        vm.delete { calls++ }
        done.await()
        kotlinx.coroutines.delay(200)
        assertEquals(1, calls)
    }
}
