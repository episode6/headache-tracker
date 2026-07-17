package com.episode6.headachetracker

import android.app.Application
import android.content.Context
import com.episode6.headachetracker.data.AutoExportManager
import com.episode6.headachetracker.data.MorningCheckInManager
import com.episode6.headachetracker.data.MorningCheckInWorker
import com.episode6.headachetracker.data.SecondPillReminderWorker
import com.episode6.headachetracker.di.AppGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory

class HeadacheTrackerApp : Application() {
    lateinit var appGraph: AppGraph
        private set

    @Inject lateinit var autoExportManager: AutoExportManager
    @Inject lateinit var morningCheckInManager: MorningCheckInManager

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        appGraph.inject(this)
        autoExportManager.startObserving()
        morningCheckInManager.startObserving()
        SecondPillReminderWorker.ensureNotificationChannel(this)
        MorningCheckInWorker.ensureNotificationChannel(this)
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as HeadacheTrackerApp).appGraph
