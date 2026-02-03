package com.lifetracker.mobile.api

import com.lifetracker.mobile.data.GameTask
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TaskApi {
    @GET("api/Task")
    suspend fun getTasks(): List<GameTask>

    @POST("api/Task")
    suspend fun createTask(@Body task: GameTask): retrofit2.Response<GameTask>

    @PUT("api/Task/{id}/complete")
    suspend fun completeTask(@Path("id") id: Int): retrofit2.Response<Unit>
}

