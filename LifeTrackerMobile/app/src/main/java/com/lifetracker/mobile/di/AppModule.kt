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
import com.lifetracker.mobile.core.sync.HeroTimeZoneSyncManager
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.data.local.AppDatabase
import com.lifetracker.mobile.data.local.MIGRATION_2_3
import com.lifetracker.mobile.data.local.MIGRATION_3_4
import com.lifetracker.mobile.data.local.MIGRATION_4_5
import com.lifetracker.mobile.data.local.MIGRATION_5_6
import com.lifetracker.mobile.data.local.MIGRATION_6_7
import com.lifetracker.mobile.data.local.MIGRATION_7_8
import com.lifetracker.mobile.data.auth.AuthRepositoryImpl
import com.lifetracker.mobile.data.auth.AuthTokenStore
import com.lifetracker.mobile.data.auth.EncryptedAuthTokenStore
import com.lifetracker.mobile.data.auth.GoogleSignInClient
import com.lifetracker.mobile.data.remote.AuthApi
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.NetworkModule
import com.lifetracker.mobile.data.repository.DataStoreSettingsRepository
import com.lifetracker.mobile.data.repository.HeroRepositoryImpl
import com.lifetracker.mobile.data.repository.ShopRepositoryImpl
import com.lifetracker.mobile.data.repository.TaskRepositoryImpl
import com.lifetracker.mobile.domain.auth.AuthRepository
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.SettingsRepository
import com.lifetracker.mobile.domain.repository.ShopRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.domain.usecase.hero.CreateHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetCurrentHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetFirstHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroAchievementsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroStatsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroesUseCase
import com.lifetracker.mobile.domain.usecase.hero.HealHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.hero.RespawnHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.UpdateHeroTimeZoneUseCase
import com.lifetracker.mobile.domain.usecase.settings.ObserveThemeModeUseCase
import com.lifetracker.mobile.domain.usecase.settings.SetThemeModeUseCase
import com.lifetracker.mobile.domain.usecase.settings.ThemeSettingsUseCases
import com.lifetracker.mobile.domain.usecase.shop.BuyItemUseCase
import com.lifetracker.mobile.domain.usecase.shop.GetInventoryUseCase
import com.lifetracker.mobile.domain.usecase.shop.GetShopItemsUseCase
import com.lifetracker.mobile.domain.usecase.shop.ShopUseCases
import com.lifetracker.mobile.domain.usecase.task.CheckOverdueTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.CompleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.CreateTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteLocalTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.FailTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.RetryTaskSyncUseCase
import com.lifetracker.mobile.domain.usecase.task.SetDailyTaskStateUseCase
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.domain.usecase.task.UpdateTaskUseCase
import com.lifetracker.mobile.ui.viewmodel.AchievementsViewModel
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import com.lifetracker.mobile.ui.viewmodel.SignInViewModel
import com.lifetracker.mobile.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.lifetracker.mobile.ui.viewmodel.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val AUTH_OKHTTP = "auth_okhttp"
private const val AUTH_RETROFIT = "auth_retrofit"
private const val APPLICATION_SCOPE = "application_scope"

val appModule =
    module {
        single { JsonDefaults }
        single { SafeApiCaller(json = get()) }

        single<AuthTokenStore> { EncryptedAuthTokenStore(context = androidContext()) }

        single(named(AUTH_OKHTTP)) {
            val settingsRepository: SettingsRepository = get()
            NetworkModule.provideAuthOkHttpClient(
                tokenStore = get(),
                deviceIdProvider = settingsRepository::getOrCreateDeviceIdBlocking,
                isDebug = BuildConfig.DEBUG,
            )
        }

        single(named(AUTH_RETROFIT)) {
            NetworkModule.provideRetrofit(
                baseUrl = BuildConfig.BASE_URL,
                client = get(named(AUTH_OKHTTP)),
                json = get(),
            )
        }

        single<AuthApi> { NetworkModule.provideAuthApi(get(named(AUTH_RETROFIT))) }

        single<OkHttpClient> {
            val settingsRepository: SettingsRepository = get()
            NetworkModule.provideOkHttpClient(
                tokenStore = get(),
                deviceIdProvider = settingsRepository::getOrCreateDeviceIdBlocking,
                authApiProvider = { get<AuthApi>() },
                isDebug = BuildConfig.DEBUG,
            )
        }

        single<Retrofit> {
            NetworkModule.provideRetrofit(
                baseUrl = BuildConfig.BASE_URL,
                client = get(),
                json = get(),
            )
        }

        single<LifeTrackerApi> { NetworkModule.provideApi(get()) }

        single(named(APPLICATION_SCOPE)) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        single<AuthRepository> {
            AuthRepositoryImpl(
                authApi = get(),
                tokenStore = get(),
                settings = get(),
                scope = get(named(APPLICATION_SCOPE)),
            )
        }

        single {
            GoogleSignInClient(
                context = androidContext(),
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            )
        }
        single { SyncScheduler(workManager = get()) }
        single { ReminderScheduler(workManager = get(), json = get()) }

        single<HeroRepository> { HeroRepositoryImpl(api = get(), caller = get(), heroDao = get()) }
        single<TaskRepository> { TaskRepositoryImpl(api = get(), caller = get(), taskDao = get(), syncScheduler =
            get()) }
        single<ShopRepository> { ShopRepositoryImpl(api = get(), caller = get()) }

        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.create {
                androidContext().preferencesDataStoreFile("settings")
            }
        }

        single<SettingsRepository> { DataStoreSettingsRepository(dataStore = get()) }

        single {
            ThemeSettingsUseCases(
                observeThemeMode = ObserveThemeModeUseCase(get()),
                setThemeMode = SetThemeModeUseCase(get()),
            )
        }

        single { ThemeController(themeSettingsUseCases = get()) }

        single {
            val heroRepo: HeroRepository = get()
            HeroUseCases(
                getHeroes = GetHeroesUseCase(heroRepo),
                getHero = GetHeroUseCase(heroRepo),
                getCurrentHero = GetCurrentHeroUseCase(heroRepo),
                getFirstHero = GetFirstHeroUseCase(heroRepo),
                createHero = CreateHeroUseCase(heroRepo),
                getHeroStats = GetHeroStatsUseCase(heroRepo),
                getHeroAchievements = GetHeroAchievementsUseCase(heroRepo),
                respawnHero = RespawnHeroUseCase(heroRepo),
                healHero = HealHeroUseCase(heroRepo),
                updateHeroTimeZone = UpdateHeroTimeZoneUseCase(heroRepo),
            )
        }

        single {
            val heroUseCases: HeroUseCases = get()
            HeroTimeZoneSyncManager(
                getCurrentHero = { heroUseCases.getCurrentHero() },
                updateHeroTimeZone = { heroId, timeZoneId ->
                    heroUseCases.updateHeroTimeZone(heroId, timeZoneId)
                },
            )
        }

        single {
            val taskRepo: TaskRepository = get()
            TaskUseCases(
                getTask = GetTaskUseCase(taskRepo),
                getTasks = GetTasksUseCase(taskRepo),
                completeTask = CompleteTaskUseCase(taskRepo),
                setDailyTaskState = SetDailyTaskStateUseCase(taskRepo),
                createTask = CreateTaskUseCase(taskRepo),
                updateTask = UpdateTaskUseCase(taskRepo),
                failTask = FailTaskUseCase(taskRepo),
                deleteTask = DeleteTaskUseCase(taskRepo),
                checkOverdue = CheckOverdueTasksUseCase(taskRepo),
                retryTaskSync = RetryTaskSyncUseCase(taskRepo),
                deleteLocalTask = DeleteLocalTaskUseCase(taskRepo),
            )
        }

        single {
            val shopRepo: ShopRepository = get()
            ShopUseCases(
                getShopItems = GetShopItemsUseCase(shopRepo),
                buyItem = BuyItemUseCase(shopRepo),
                getInventory = GetInventoryUseCase(shopRepo),
            )
        }

        viewModel { params ->
            val heroIdParam: Int = params.get()
            val editingIdParam: Int = params.get()
            CreateDailyViewModel(
                heroId = heroIdParam,
                editingTaskId = editingIdParam.takeIf { it > 0 },
                taskUseCases = get(),
                reminderScheduler = get(),
            )
        }

        viewModelOf(::HeroViewModel)
        viewModelOf(::ShopViewModel)
        viewModelOf(::AchievementsViewModel)
        viewModelOf(::StatsViewModel)
        viewModelOf(::SignInViewModel)

        single {
            Room
                .databaseBuilder(androidContext(), AppDatabase::class.java, "lifetracker.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        single { get<AppDatabase>().heroDao() }
        single { get<AppDatabase>().taskDao() }
    }