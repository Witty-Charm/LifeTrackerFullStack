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
import com.lifetracker.mobile.ui.model.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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

    @Test
    fun buyItem_emitsRecoveryUpdateEvent_afterSuccessfulRevivalTokenPurchase() =
        runTest {
            val repository =
                FakeShopRepository().apply {
                    shopItemsResults +=
                        DomainResult.Success(
                            listOf(
                                testShopItem(
                                    id = 5,
                                    name = "Revival Token",
                                    description = "Removes recovery debuff instantly",
                                    price = 100,
                                    itemType = 5,
                                    effectValue = 1,
                                ),
                            ),
                        )
                    buyItemResults +=
                        DomainResult.Success(
                            BuyResult(
                                newGold = 100,
                                newHp = 50,
                                maxHp = 50,
                                purchasedItem =
                                    testShopItem(
                                        id = 5,
                                        name = "Revival Token",
                                        description = "Removes recovery debuff instantly",
                                        price = 100,
                                        itemType = 5,
                                        effectValue = 1,
                                    ),
                                message = "Purchased Revival Token for 100 gold!",
                                effect = "Recovery debuff removed",
                                xpBoostPercent = 0,
                                xpBoostTasksRemaining = 0,
                                recoveryDebuffActive = false,
                                recoveryMultiplier = 1.0,
                            ),
                        )
                }
            val viewModel = buildViewModel(repository)

            viewModel.loadForHero(heroId = 1)
            advanceUntilIdle()

            val eventsDeferred = async { viewModel.events.take(6).toList() }

            viewModel.buyItem(heroId = 1, itemId = 5, heroGold = 200)
            advanceUntilIdle()

            val events = eventsDeferred.await()

            assertEquals(UiEvent.HeroGoldUpdated(100), events[0])
            assertEquals(UiEvent.HeroGoldUpdated(100), events[1])
            assertEquals(UiEvent.HeroHpUpdated(50, 50), events[2])
            assertEquals(UiEvent.HeroXpBoostUpdated(0, 0), events[3])
            assertEquals(UiEvent.HeroRecoveryUpdated(false, 1.0), events[4])
            assertEquals(UiEvent.ShowSnackbar("Purchased Revival Token for 100 gold!"), events[5])
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
        val buyItemResults = ArrayDeque<DomainResult<BuyResult>>()

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
        ): DomainResult<BuyResult> =
            if (buyItemResults.isNotEmpty()) {
                buyItemResults.removeFirst()
            } else {
                DomainResult.Failure(GameError.Unknown("Not used in this test"))
            }

        override suspend fun getInventory(heroId: Int): DomainResult<List<InventoryItemDomain>> {
            getInventoryCalls++
            return DomainResult.Success(emptyList())
        }
    }

    private fun testShopItem(
        id: Int = 1,
        name: String = "Potion",
        description: String = "Heal",
        price: Int = 10,
        itemType: Int = 1,
        effectValue: Int = 20,
    ) = ShopItemDomain(
        id = id,
        name = name,
        description = description,
        price = price,
        itemType = itemType,
        effectValue = effectValue,
    )
}
