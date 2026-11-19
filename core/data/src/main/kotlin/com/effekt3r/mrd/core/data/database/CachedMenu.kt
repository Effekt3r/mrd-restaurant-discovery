package com.effekt3r.mrd.core.data.database

import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuOption
import com.effekt3r.mrd.core.model.Money
import kotlinx.serialization.Serializable

// The menu cache's JSON: the validated model, so :core:model stays free of serialization.

@Serializable
internal data class CachedMenu(val categories: List<CachedCategory>)

@Serializable
internal data class CachedCategory(val id: String, val name: String, val items: List<CachedItem>)

@Serializable
internal data class CachedItem(
    val id: String,
    val name: String,
    val priceCents: Long,
    val isAvailable: Boolean,
    val description: String?,
    val imageUrl: String?,
    val options: List<CachedOption>
)

@Serializable
internal data class CachedOption(val id: String, val name: String, val priceCents: Long)

internal fun Menu.toCached() = CachedMenu(
    categories.map { category ->
        CachedCategory(
            category.id,
            category.name,
            category.items.map { item ->
                CachedItem(
                    item.id,
                    item.name,
                    item.price.cents,
                    item.isAvailable,
                    item.description,
                    item.imageUrl,
                    item.options.map { CachedOption(it.id, it.name, it.price.cents) }
                )
            }
        )
    }
)

internal fun CachedMenu.toModel() = Menu(
    categories.map { category ->
        MenuCategory(
            category.id,
            category.name,
            category.items.map { item ->
                MenuItem(
                    item.id,
                    item.name,
                    Money(item.priceCents),
                    item.isAvailable,
                    item.description,
                    item.imageUrl,
                    item.options.map { MenuOption(it.id, it.name, Money(it.priceCents)) }
                )
            }
        )
    }
)
