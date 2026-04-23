package ch.widmedia.guetetag.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object Calendar : Destination

    @Serializable
    data class Entry(val dateMillis: Long) : Destination
}
