package com.lifetracker.mobile.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.reminder.ReminderScheduler
import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.data.local.AppDatabase
import com.lifetracker.mobile.data.remote.NetworkModule
import com.lifetracker.mobile.data.repository.DataStoreSettingsRepository
import com.lifetracker.mobile.data.repository.HeroRepositoryImpl
import com.lifetracker.mobile.data.repository.TaskRepositoryImpl
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.SettingsRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.domain.usecase.hero.CreateHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetFirstHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroStatsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroesUseCase
import com.lifetracker.mobile.domain.usecase.hero.HealHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.hero.RespawnHeroUseCase
import com.lifetracker.mobile.domain.usecase.settings.ObserveThemeModeUseCase
import com.lifetracker.mobile.domain.usecase.settings.SetThemeModeUseCase
import com.lifetracker.mobile.domain.usecase.settings.ThemeSettingsUseCases
import com.lifetracker.mobile.domain.usecase.task.CheckOverdueTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.CompleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.CreateTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.FailTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { JsonDefaults }
    single { SafeApiCaller(json = get()) }
    single { NetworkModule.provideOkHttpClient(isDebug = BuildConfig.DEBUG) }
    single { NetworkModule.provideApi(baseUrl = BuildConfig.BASE_URL, client = get(), json = get()) }
    single { SyncScheduler(workManager = get()) }
    single { ReminderScheduler(workManager = get(), json = get()) }
    single<HeroRepository> { HeroRepositoryImpl(api = get(), caller = get(), heroDao = get()) }
    single<TaskRepository> { TaskRepositoryImpl(api = get(), caller = get(), taskDao = get(), syncScheduler = get()) }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("settings")
        }
    }
    single<SettingsRepository> { DataStoreSettingsRepository(dataStore = get()) }
    single {
        val settingsRepo: SettingsRepository = get()
        ThemeSettingsUseCases(
            observeThemeMode = ObserveThemeModeUseCase(settingsRepo),
            setThemeMode = SetThemeModeUseCase(settingsRepo),
        )
    }
    single { ThemeController(themeSettingsUseCases = get()) }

    viewModel { params ->
        CreateDailyViewModel(
            heroId = params.get(),
            taskUseCases = get(),
            reminderScheduler = get(),
        )
    }

    viewModelOf(::HeroViewModel)
    single {
        val heroRepo: HeroRepository = get()
        HeroUseCases(
            getHeroes = GetHeroesUseCase(heroRepo),
            getHero = GetHeroUseCase(heroRepo),
            getFirstHero = GetFirstHeroUseCase(heroRepo),
            createHero = CreateHeroUseCase(heroRepo),
            getHeroStats = GetHeroStatsUseCase(heroRepo),
            respawnHero = RespawnHeroUseCase(heroRepo),
            healHero = HealHeroUseCase(heroRepo),
        )
    }
    single  {
        val taskRepo: TaskRepository = get()
        TaskUseCases(
            getTask = GetTaskUseCase(taskRepo),
            getTasks = GetTasksUseCase(taskRepo),
            completeTask = CompleteTaskUseCase(taskRepo),
            createTask = CreateTaskUseCase(taskRepo),
            failTask = FailTaskUseCase(taskRepo),
            deleteTask = DeleteTaskUseCase(taskRepo),
            checkOverdue = CheckOverdueTasksUseCase(taskRepo),
        )
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "lifetracker.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<AppDatabase>().heroDao() }
    single { get<AppDatabase>().taskDao() }
}
