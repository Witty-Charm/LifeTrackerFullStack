package com.lifetracker.mobile.di

import androidx.work.WorkManager
import com.lifetracker.mobile.core.reminder.ReminderWorker
import com.lifetracker.mobile.core.sync.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.dsl.module

val workManagerModule = module {
    single { WorkManager.getInstance(androidContext()) }
    single { KoinWorkerFactory() }

    workerOf(::SyncWorker)
    workerOf(::ReminderWorker)
}
