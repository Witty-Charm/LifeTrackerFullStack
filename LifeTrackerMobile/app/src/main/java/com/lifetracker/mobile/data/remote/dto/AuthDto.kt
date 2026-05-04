package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInRequestDto(
    @SerialName("idToken") val idToken: String,
    @SerialName("claimDeviceId") val claimDeviceId: String? = null,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class AuthResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("accessTokenExpiresAt") val accessTokenExpiresAt: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("refreshTokenExpiresAt") val refreshTokenExpiresAt: String,
    @SerialName("user") val user: AuthUserDto,
)

@Serializable
data class AuthUserDto(
    @SerialName("id") val id: Int,
    @SerialName("email") val email: String,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
)
