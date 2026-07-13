package com.episode6.headachetracker.di

import android.content.Context
import com.episode6.headachetracker.HeadacheTrackerApp
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.data.HeadacheDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@DependencyGraph
@SingleIn(AppScope::class)
interface AppGraph {

    val viewModelFactory: AppViewModelFactory

    fun inject(app: HeadacheTrackerApp)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(context: Context): HeadacheDatabase = HeadacheDatabase.getDatabase(context)

    @Provides
    @SingleIn(AppScope::class)
    fun provideDao(database: HeadacheDatabase): HeadacheDao = database.headacheDao()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}
