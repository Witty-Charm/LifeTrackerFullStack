package com.lifetracker.mobile.api

import com.lifetracker.mobile.data.Hero
import retrofit2.http.GET

interface HeroApi {
    @GET("api/Hero/1")
    suspend fun getHero(): Hero
}