package com.effekt3r.mrd.core.model

import java.time.Instant

/** A restaurant's validated menu (spec §5.9). Categories and items keep the API's order. */
data class Menu(val categories: List<MenuCategory>)

data class MenuCategory(val id: String, val name: String, val items: List<MenuItem>)

/** [isAvailable] false means sold out. [description] and [imageUrl] are null when missing. */
data class MenuItem(
    val id: String,
    val name: String,
    val price: Money,
    val isAvailable: Boolean,
    val description: String?,
    val imageUrl: String?,
    val options: List<MenuOption>
)

/** An item's extra. A price of zero shows as "Free". */
data class MenuOption(val id: String, val name: String, val price: Money)

/** The menu as Details shows it. The restaurant itself always renders from the list's cache. */
sealed interface MenuState {
    data object Loading : MenuState

    data class Content(val menu: Menu, val fetchedAt: Instant) : MenuState

    /** The restaurant hasn't shared a menu: the API answered 404, or the menu has no categories. */
    data object NotAvailable : MenuState

    /** The refresh failed. [cached] is the menu seen before, if any. */
    data class Error(val error: SyncError, val cached: Content?) : MenuState
}
