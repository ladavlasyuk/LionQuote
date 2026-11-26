package com.zentora.app.goalshift.passage

import android.content.SharedPreferences

internal sealed interface ArrivalRoute {
    data object Game : ArrivalRoute
    data object Retry : ArrivalRoute
    data class Page(val address: String) : ArrivalRoute
}

internal suspend fun chooseRoute(
    preferences: SharedPreferences,
    fetch: suspend () -> String?
): ArrivalRoute {
    val saved = preferences.getString("landing_address", null)
    if (!saved.isNullOrBlank()) return ArrivalRoute.Page(saved)
    val reply = fetch()
    if (reply.isNullOrBlank()) return ArrivalRoute.Retry
    val destination = readDestination(reply) ?: return ArrivalRoute.Game
    preferences.edit()
        .putString("landing_address", destination.url)
        .putString("landing_key", destination.accessKey)
        .apply()
    return ArrivalRoute.Page(destination.url)
}
