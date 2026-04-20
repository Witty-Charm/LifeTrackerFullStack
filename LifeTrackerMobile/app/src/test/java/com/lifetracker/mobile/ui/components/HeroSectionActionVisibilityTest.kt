package com.lifetracker.mobile.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroSectionActionVisibilityTest {
    @Test
    fun showRespawnAction_returnsTrue_whenHeroIsDead() {
        assertTrue(showRespawnAction(isDead = true))
    }

    @Test
    fun showRespawnAction_returnsFalse_whenHeroIsAlive() {
        assertFalse(showRespawnAction(isDead = false))
    }
}
