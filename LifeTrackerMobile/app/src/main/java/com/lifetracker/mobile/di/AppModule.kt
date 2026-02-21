package com.lifetracker.mobile.di

import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.data.remote.NetworkModule
import com.lifetracker.mobile.data.repository.HeroRepositoryImpl
import com.lifetracker.mobile.data.repository.TaskRepositoryImpl
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { NetworkModule.json }
    single { NetworkModule.safeApiCaller }
    single { NetworkModule.provideOkHttpClient(isDebug = BuildConfig.DEBUG) }
    single { NetworkModule.provideApi(baseUrl = BuildConfig.BASE_URL, client = get()) }
    single<HeroRepository> { HeroRepositoryImpl(api = get(), caller = get()) }
    single<TaskRepository> { TaskRepositoryImpl(api = get(), caller = get()) }
    viewModel { HeroViewModel(heroRepo = get(), taskRepo = get()) }
}