package com.lifetracker.mobile.data.remote

import com.lifetracker.mobile.data.remote.dto.AuthResponseDto
import com.lifetracker.mobile.data.remote.dto.AuthUserDto
import com.lifetracker.mobile.data.remote.dto.GoogleSignInRequestDto
import com.lifetracker.mobile.data.remote.dto.LogoutRequestDto
import com.lifetracker.mobile.data.remote.dto.RefreshRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/google")
    suspend fun signInWithGoogle(@Body request: GoogleSignInRequestDto): Response<AuthResponseDto>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): Response<AuthResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Unit>

    @GET("api/auth/me")
    suspend fun me(): Response<AuthUserDto>
}
