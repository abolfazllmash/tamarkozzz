package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerProfile(
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val stars: Int = 0,
    val focusPoints: Int = 0,
    val streak: Int = 1,
    val lastLoginTime: Long = 0,
    val equippedSkin: String = "neon_green",
    val unlockedSkins: String = "neon_green",
    val equippedAbility: String = "shield",
    val unlockedAbilities: String = "shield",
    val upgradesString: String = "shield_duration:0,slow_mo_duration:0,rare_drop:0",
    val achievementsJson: String? = null,
    val questsJson: String? = null
)

@JsonClass(generateAdapter = true)
data class GuestLoginRequest(
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    @Json(name = "id_token") val idToken: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val ok: Boolean = false,
    val token: String? = null,
    @Json(name = "user_id") val userId: Long? = null,
    val profile: ServerProfile? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SaveProfileRequest(
    val profile: ServerProfile
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val ok: Boolean = false,
    val profile: ServerProfile? = null,
    val error: String? = null
)
