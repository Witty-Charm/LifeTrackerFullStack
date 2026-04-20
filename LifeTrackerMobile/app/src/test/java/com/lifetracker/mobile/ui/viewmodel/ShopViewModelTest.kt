package com.lifetracker.mobile.ui.viewmodel

import com.lifetracker.mobile.domain.model.BuyResult
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.model.ShopItemDomain
import com.lifetracker.mobile.domain.repository.ShopRepository
import com.lifetracker.mobile.domain.usecase.shop.BuyItemUseCase
import com.lifetracker.mobile.domain.usecase.shop.GetInventoryUseCase
import com.lifetracker.mobile.domain.usecase.shop.GetShopItemsUseCase
import com.lifetracker.mobile.domain.usecase.shop.ShopUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
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
    fun loadForHero_retriesSameHero_whenFirstItemsLoadFailed() =
        runTest {
            val repository =
                FakeShopRepository().apply {
                    shopItemsResults += DomainResult.Failure(GameError.Network)
                    shopItemsResults += DomainResult.Success(listOf(testShopItem()))
                }
            val viewModel = buildViewModel(repository)

            viewModel.loadForHero(heroId = 1)
            advanceUntilIdle()
            assertEquals(1, repository.getShopItemsCalls)
            assertEquals(0, viewModel.state.value.items.size)

            viewModel.loadForHero(heroId = 1)
            advanceUntilIdle()

            assertEquals(2, repository.getShopItemsCalls)
            assertEquals(1, viewModel.state.value.items.size)
        }

    @Test
    fun loadForHero_skipsSecondRequest_forSameHero_afterSuccessfulLoad() =
        runTest {
            val repository =
                FakeShopRepository().apply {
                    shopItemsResults += DomainResult.Success(listOf(testShopItem()))
                }
            val viewModel = buildViewModel(repository)

            viewModel.loadForHero(heroId = 1)
            advanceUntilIdle()
            viewModel.loadForHero(heroId = 1)
            advanceUntilIdle()

            assertEquals(1, repository.getShopItemsCalls)
            assertEquals(1, repository.getInventoryCalls)
        }

    private fun buildViewModel(repository: FakeShopRepository): ShopViewModel =
        ShopViewModel(
            shopUseCases =
                ShopUseCases(
                    getShopItems = GetShopItemsUseCase(repository),
                    buyItem = BuyItemUseCase(repository),
                    getInventory = GetInventoryUseCase(repository),
                ),
        )

    private class FakeShopRepository : ShopRepository {
        var getShopItemsCalls: Int = 0
        var getInventoryCalls: Int = 0
        val shopItemsResults = ArrayDeque<DomainResult<List<ShopItemDomain>>>()

        override suspend fun getShopItems(): DomainResult<List<ShopItemDomain>> {
            getShopItemsCalls++
            return if (shopItemsResults.isNotEmpty()) {
                shopItemsResults.removeFirst()
            } else {
                DomainResult.Success(emptyList())
            }
        }

        override suspend fun buyItem(
            heroId: Int,
            itemId: Int,
        ): DomainResult<BuyResult> = DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun getInventory(heroId: Int): DomainResult<List<InventoryItemDomain>> {
            getInventoryCalls++
            return DomainResult.Success(emptyList())
        }
    }

    private fun testShopItem() =
        ShopItemDomain(
            id = 1,
            name = "Potion",
            description = "Heal",
            price = 10,
            itemType = 1,
            effectValue = 20,
        )
}
