package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.data.remote.dto.BuyResultDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopMapperTest {
    @Test
    fun buyResultDto_toDomain_mapsRecoveryFields() {
        val dto =
            JsonDefaults.decodeFromString<BuyResultDto>(
                """
                {
                  "newGold": 100,
                  "newHp": 50,
                  "maxHp": 50,
                  "purchasedItem": {
                    "id": 5,
                    "name": "Revival Token",
                    "description": "Removes recovery debuff instantly",
                    "price": 100,
                    "itemType": 5,
                    "effectValue": 1
                  },
                  "message": "Purchased Revival Token for 100 gold!",
                  "effect": "Recovery debuff removed",
                  "xpBoostPercent": 0,
                  "xpBoostTasksRemaining": 0,
                  "recoveryDebuffActive": false,
                  "recoveryMultiplier": 1.0
                }
                """.trimIndent(),
            )

        val domain = dto.toDomain()

        assertEquals(false, domain.recoveryDebuffActive)
        assertEquals(1.0, domain.recoveryMultiplier, 0.0)
        assertEquals("Recovery debuff removed", domain.effect)
    }
}
