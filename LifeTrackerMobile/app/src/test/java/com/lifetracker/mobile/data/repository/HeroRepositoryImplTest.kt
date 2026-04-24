package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.ApiError
import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.local.entity.HeroEntity
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.domain.model.DomainResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroRepositoryImplTest {
    @Test
    fun getCurrentHero_returnsSuccessNull_whenApiReturns404AndNoCache() =
        runTest {
            val api = mockk<LifeTrackerApi>()
            val caller = mockk<SafeApiCaller>()
            val heroDao = FakeHeroDao()
            val repository = HeroRepositoryImpl(api = api, caller = caller, heroDao = heroDao)

            coEvery { caller.safeApiCall<com.lifetracker.mobile.data.remote.dto.HeroDto>(any()) } returns NetworkResult.Error(
                404,
                ApiError(title = "Not Found", status = 404),
            )

            val result = repository.getCurrentHero()

            assertTrue(result is DomainResult.Success && result.data == null)
            coVerify(exactly = 1) { caller.safeApiCall<com.lifetracker.mobile.data.remote.dto.HeroDto>(any()) }
        }

    private class FakeHeroDao : HeroDao {
        private val heroes = LinkedHashMap<Int, HeroEntity>()

        override suspend fun getAll(): List<HeroEntity> = heroes.values.toList()

        override suspend fun getById(id: Int): HeroEntity? = heroes[id]

        override suspend fun getFirst(): HeroEntity? = heroes.values.firstOrNull()

        override suspend fun upsert(hero: HeroEntity) {
            heroes[hero.id] = hero
        }

    }
}
