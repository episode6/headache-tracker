package com.episode6.headachetracker

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test. Snapshot builds (everything except CI
        // release-tag builds) install under com.episode6.snapshots.headachetracker,
        // so only assert the shared prefix.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(appContext.packageName.endsWith("headachetracker"))
        assertTrue(appContext.packageName.startsWith("com.episode6"))
    }
}