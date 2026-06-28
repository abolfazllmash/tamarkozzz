package com.example.network

import android.content.Context
import java.util.UUID

class AuthManager(context: Context) {

    private val prefs = context.getSharedPreferences("tamarkoz_auth", Context.MODE_PRIVATE)

    val deviceId: String
        get() {
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) { prefs.edit().putString("token", value).apply() }

    var userId: Long
        get() = prefs.getLong("user_id", 0L)
        set(value) { prefs.edit().putLong("user_id", value).apply() }

    fun hasToken(): Boolean = !token.isNullOrEmpty()

    fun bearer(): String = "Bearer ${token ?: ""}"

    fun logout() {
        prefs.edit().remove("token").remove("user_id").apply()
    }
}
