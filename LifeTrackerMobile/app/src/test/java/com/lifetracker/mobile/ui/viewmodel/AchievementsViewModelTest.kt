package com.lifetracker.mobile.ui.viewmodel

import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.usecase.hero.CreateHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetFirstHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroAchievementsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroStatsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroesUseCase
import com.lifetracker.mobile.domain.usecase.hero.HealHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.hero.RespawnHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.UpdateHeroTimeZoneUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAchievements_success_exposesBackendOrder() =
        runTest {
            val repository =
                FakeHeroRepository().apply {
                    achievementsResult =
                        DomainResult.Success(
                            listOf(
                                AchievementDomain(
                                    key = "tasks_10",
                                    title = "Task Starter",
                                    description = "Complete 10 tasks.",
                                    category = "TasksCompleted",
                                    threshold = 10,
                                    sortOrder = 10,
                                    goldReward = 25,
                                    unlocked = false,
                                    unlockedAt = null,
                                ),
                                AchievementDomain(
                                    key = "level_20",
                                    title = "Elite Hero",
                                    description = "Reach level 20.",
                                    category = "LevelReached",
                                    threshold = 20,
                                    sortOrder = 90,
                                    goldReward = 400,
                                    unlocked = true,
                                    unlockedAt = Instant.parse("2026-04-20T10:00:00Z"),
                                ),
                            ),
                        )
                }
            val viewModel = AchievementsViewModel(buildHeroUseCases(repository))

            viewModel.loadAchievements(heroId = 1)
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isLoading)
            assertEquals(2, viewModel.state.value.achievements.size)
            assertEquals(
                "tasks_10",
                viewModel.state.value.achievements[0]
                    .key,
            )
            assertEquals(
                "level_20",
                viewModel.state.value.achievements[1]
                    .key,
            )
            assertNull(viewModel.state.value.actionError)
        }

    @Test
    fun loadAchievements_failure_setsActionError() =
        runTest {
            val repository =
                FakeHeroRepository().apply {
                    achievementsResult = DomainResult.Failure(GameError.Network)
                }
            val viewModel = AchievementsViewModel(buildHeroUseCases(repository))

            viewModel.loadAchievements(heroId = 1)
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isLoading)
            assertEquals(0, viewModel.state.value.achievements.size)
            assertEquals(true, viewModel.state.value.actionError != null)
        }

    private fun buildHeroUseCases(repository: FakeHeroRepository) =
        HeroUseCases(
            getHeroes = GetHeroesUseCase(repository),
            getHero = GetHeroUseCase(repository),
            getFirstHero = GetFirstHeroUseCase(repository),
            createHero = CreateHeroUseCase(repository),
            getHeroStats = GetHeroStatsUseCase(repository),
            getHeroAchievements = GetHeroAchievementsUseCase(repository),
            respawnHero = RespawnHeroUseCase(repository),
            healHero = HealHeroUseCase(repository),
            updateHeroTimeZone = UpdateHeroTimeZoneUseCase(repository),
        )

    private class FakeHeroRepository : HeroRepository {
        var achievementsResult: DomainResult<List<AchievementDomain>> = DomainResult.Success(emptyList())

        override suspend fun getHeroes(): DomainResult<List<HeroDomain>> = DomainResult.Success(emptyList())

        override suspend fun getHero(id: Int): DomainResult<HeroDomain> = DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun getFirstHero(): DomainResult<HeroDomain?> = DomainResult.Success(null)

        override suspend fun createHero(
            name: String,
            startingGold: Int?,
        ): DomainResult<HeroDomain> = DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun getHeroAchievements(heroId: Int): DomainResult<List<AchievementDomain>> = achievementsResult

        override suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun healHero(
            heroId: Int,
            amount: Int?,
        ): DomainResult<HealResult> = DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun updateHeroTimeZone(
            heroId: Int,
            timeZoneId: String,
        ): DomainResult<Unit> = DomainResult.Success(Unit)
    }
}
