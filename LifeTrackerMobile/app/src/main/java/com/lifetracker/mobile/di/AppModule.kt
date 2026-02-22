package com.lifetracker.mobile.di

import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.data.remote.NetworkModule
import com.lifetracker.mobile.data.repository.HeroRepositoryImpl
import com.lifetracker.mobile.data.repository.TaskRepositoryImpl
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.domain.usecase.HeroUseCases
import com.lifetracker.mobile.domain.usecase.TaskUseCases
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
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
    single<HeroRepository> { HeroRepositoryImpl(api = get(), caller = get()) }
    single<TaskRepository> { TaskRepositoryImpl(api = get(), caller = get()) }
    viewModel {
        HeroViewModel(
            heroUseCases = get(),
            taskUseCases = get(),
            isDebug = BuildConfig.DEBUG,
        )
    }
    factory {
        HeroUseCases(
            getHeroes = get(),
            getHero = get(),
            getFirstHero = get(),
            createHero = get(),
            getHeroStats = get(),
            respawnHero = get(),
            healHero = get()
        )
    }
    factory {
        TaskUseCases(
            getTask = get(),
            getTasks = get(),
            completeTask = get(),
            createTask = get(),
            failTask = get(),
            deleteTask = get(),
            checkOverdue = get()
        )
    }
}
