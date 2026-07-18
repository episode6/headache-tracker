package com.episode6.headachetracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Calendar : Route

    @Serializable
    data class EditEntry(val date: String) : Route

    @Serializable
    data class FullYear(val year: Int) : Route

    @Serializable
    data object NotesSummary : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Licenses : Route
}
