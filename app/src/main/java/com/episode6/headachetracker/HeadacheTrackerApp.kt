package com.episode6.headachetracker

import android.app.Application
import android.content.Context
import com.episode6.headachetracker.data.AutoExportManager
import com.episode6.headachetracker.di.AppGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory

class HeadacheTrackerApp : Application() {
    lateinit var appGraph: AppGraph
        private set

    @Inject lateinit var autoExportManager: AutoExportManager

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        appGraph.inject(this)
        autoExportManager.startObserving()
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as HeadacheTrackerApp).appGraph
