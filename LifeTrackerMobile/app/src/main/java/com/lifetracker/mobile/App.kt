package com.lifetracker.mobile

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.lifetracker.mobile.core.sync.HeroTimeZoneSyncManager
import com.lifetracker.mobile.core.sync.TimeZoneChangedReceiver
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.di.appModule
import com.lifetracker.mobile.di.workManagerModule
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application(), Configuration.Provider {
    private lateinit var workerFactory: KoinWorkerFactory
    private lateinit var heroTimeZoneSyncManager: HeroTimeZoneSyncManager
    private lateinit var timeZoneChangedReceiver: TimeZoneChangedReceiver

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidContext(this@App)
            workManagerFactory()
            modules(appModule, workManagerModule)
        }
        workerFactory = koinApp.koin.get()

        val themeController = GlobalContext.get().get<ThemeController>()
        heroTimeZoneSyncManager = GlobalContext.get().get<HeroTimeZoneSyncManager>()

        runBlocking {
            themeController.applyInitialTheme()
        }
        themeController.startObserving()

        timeZoneChangedReceiver = TimeZoneChangedReceiver(heroTimeZoneSyncManager)
        ContextCompat.registerReceiver(
            this,
            timeZoneChangedReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    heroTimeZoneSyncManager.syncIfNeededAsync()
                }
            }
        )
    }

    override fun onTerminate() {
        unregisterReceiver(timeZoneChangedReceiver)
        super.onTerminate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}