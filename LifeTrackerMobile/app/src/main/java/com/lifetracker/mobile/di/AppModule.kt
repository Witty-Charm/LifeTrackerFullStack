package com.lifetracker.mobile.di

import androidx.room.Room
import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.data.local.AppDatabase
import com.lifetracker.mobile.data.remote.NetworkModule
import com.lifetracker.mobile.data.repository.HeroRepositoryImpl
import com.lifetracker.mobile.data.repository.TaskRepositoryImpl
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.domain.usecase.hero.CreateHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetFirstHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroStatsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroesUseCase
import com.lifetracker.mobile.domain.usecase.hero.HealHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.hero.RespawnHeroUseCase
import com.lifetracker.mobile.domain.usecase.task.CheckOverdueTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.CompleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.CreateTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.FailTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import kotlinx.serialization.json.Json
import androidx.work.WorkManager
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.core.sync.SyncWorker
import org.koin.core.module.dsl.viewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            coerceInputValues = true
        }
    }
    single { SafeApiCaller(json = get()) }
    single { NetworkModule.provideOkHttpClient(isDebug = BuildConfig.DEBUG) }
    single { NetworkModule.provideApi(baseUrl = BuildConfig.BASE_URL, client = get(), json = get()) }
    single { SyncScheduler(workManager = get()) }
    single<HeroRepository> { HeroRepositoryImpl(api = get(), caller = get(), heroDao = get()) }
    single<TaskRepository> { TaskRepositoryImpl(api = get(), caller = get(), taskDao = get(), syncScheduler = get()) }
    single { WorkManager.getInstance(androidContext()) }

    viewModel {
        HeroViewModel(
            heroUseCases = get(),
            taskUseCases = get(),
            workManager = get(),
            isDebug = BuildConfig.DEBUG,
        )
    }
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
            .build()
    }

    single { get<AppDatabase>().heroDao() }
    single { get<AppDatabase>().taskDao() }

    workerOf(::SyncWorker)
}
