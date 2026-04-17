package com.lifetracker.mobile.domain.usecase.hero

data class HeroUseCases(
    val getHeroes: GetHeroesUseCase,
    val getHero: GetHeroUseCase,
    val getFirstHero: GetFirstHeroUseCase,
    val createHero: CreateHeroUseCase,
    val getHeroStats: GetHeroStatsUseCase,
    val respawnHero: RespawnHeroUseCase,
    val healHero: HealHeroUseCase,
    val updateHeroTimeZone: UpdateHeroTimeZoneUseCase,
)