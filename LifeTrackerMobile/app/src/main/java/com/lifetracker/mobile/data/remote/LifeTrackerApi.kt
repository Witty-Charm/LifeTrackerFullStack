package com.lifetracker.mobile.data.remote

import com.lifetracker.mobile.data.remote.dto.BuyItemRequestDto
import com.lifetracker.mobile.data.remote.dto.BuyResultDto
import com.lifetracker.mobile.data.remote.dto.CompleteTaskResponse
import com.lifetracker.mobile.data.remote.dto.CreateHeroRequest
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.FailTaskResponse
import com.lifetracker.mobile.data.remote.dto.HealResponse
import com.lifetracker.mobile.data.remote.dto.HeroDto
import com.lifetracker.mobile.data.remote.dto.HeroStatsDto
import com.lifetracker.mobile.data.remote.dto.OverdueCheckResponse
import com.lifetracker.mobile.data.remote.dto.PurchasedItemDto
import com.lifetracker.mobile.data.remote.dto.RespawnResponse
import com.lifetracker.mobile.data.remote.dto.ShopItemDto
import com.lifetracker.mobile.data.remote.dto.TaskDto
import com.lifetracker.mobile.data.remote.dto.UpdateHeroTimeZoneRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LifeTrackerApi {

    @GET("api/Hero")
    suspend fun getHeroes(): Response<List<HeroDto>>

    @GET("api/Hero/{id}")
    suspend fun getHero(@Path("id") id: Int): Response<HeroDto>

    @POST("api/Hero")
    suspend fun createHero(@Body request: CreateHeroRequest): Response<HeroDto>

    @GET("api/Hero/{id}/stats")
    suspend fun getHeroStats(@Path("id") id: Int): Response<HeroStatsDto>

    @PATCH("api/Hero/{id}/timezone")
    suspend fun updateHeroTimeZone(
        @Path("id") id: Int,
        @Body request: UpdateHeroTimeZoneRequest,
    ): Response<Unit>

    @POST("api/Hero/{id}/respawn")
    suspend fun respawnHero(@Path("id") id: Int): Response<RespawnResponse>

    @POST("api/Hero/{id}/heal")
    suspend fun healHero(
        @Path("id") id: Int,
        @Query("amount") amount: Int? = null,
    ): Response<HealResponse>

    @GET("api/Task")
    suspend fun getTasks(@Query("heroId") heroId: Int? = null): Response<List<TaskDto>>

    @GET("api/Task/{id}")
    suspend fun getTask(@Path("id") id: Int): Response<TaskDto>

    @POST("api/Task")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<TaskDto>

    @PUT("api/Task/{id}/complete")
    suspend fun completeTask(@Path("id") id: Int): Response<CompleteTaskResponse>

    @PUT("api/Task/{id}/fail")
    suspend fun failTask(@Path("id") id: Int): Response<FailTaskResponse>

    @POST("api/Task/check-overdue")
    suspend fun checkOverdueTasks(@Query("heroId") heroId: Int? = null): Response<OverdueCheckResponse>

    @DELETE("api/Task/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @GET("api/Shop/items")
    suspend fun getShopItems(): Response<List<ShopItemDto>>

    @POST("api/Shop/buy")
    suspend fun buyItem(@Body request: BuyItemRequestDto): Response<BuyResultDto>

    @GET("api/Shop/inventory/{heroId}")
    suspend fun getInventory(@Path("heroId") heroId: Int): Response<List<PurchasedItemDto>>
}
