package com.lifetracker.mobile.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopScreenIconsTest {
    @Test
    fun itemType2_usesScience_itemType3_usesStar_itemType4_usesShield_itemType5_usesInventory() {
        assertEquals(Icons.Filled.Science, iconForItemType(2))
        assertEquals(Icons.Filled.Star, iconForItemType(3))
        assertEquals(Icons.Filled.Shield, iconForItemType(4))
        assertEquals(Icons.Filled.Inventory2, iconForItemType(5))
    }
}
