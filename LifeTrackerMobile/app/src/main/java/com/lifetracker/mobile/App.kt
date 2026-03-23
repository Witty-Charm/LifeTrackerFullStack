package com.lifetracker.mobile

import android.app.Application
import androidx.work.Configuration
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

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidContext(this@App)
            workManagerFactory()
            modules(appModule, workManagerModule)
        }
        workerFactory = koinApp.koin.get()
        val themeController = GlobalContext.get().get<ThemeController>()
        runBlocking {
            themeController.applyInitialTheme()
        }
        themeController.startObserving()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}