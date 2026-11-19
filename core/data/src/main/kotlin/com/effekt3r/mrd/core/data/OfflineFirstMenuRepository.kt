package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.common.IoDispatcher
import com.effekt3r.mrd.core.data.database.CachedMenu
import com.effekt3r.mrd.core.data.database.MenuDao
import com.effekt3r.mrd.core.data.database.MenuEntity
import com.effekt3r.mrd.core.data.database.toCached
import com.effekt3r.mrd.core.data.database.toModel
import com.effekt3r.mrd.core.data.network.DiscoveryApi
import com.effekt3r.mrd.core.data.network.MenuParser
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.SyncError
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json

internal class OfflineFirstMenuRepository @Inject constructor(
    private val api: DiscoveryApi,
    private val dao: MenuDao,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MenuRepository {

    // flowOn: the response body is read where the flow runs, which must not be the main thread.
    override fun menu(id: RestaurantId): Flow<MenuState> = flow {
        val cached = dao.menu(id.value)?.let {
            MenuState.Content(
                json.decodeFromString(CachedMenu.serializer(), it.menuJson).toModel(),
                Instant.ofEpochMilli(it.fetchedAt)
            )
        }
        emit(cached ?: MenuState.Loading)
        emit(fetch(id.value, cached))
    }.flowOn(ioDispatcher)

    // The same client, ETag revalidation and error mapping as the list (spec §5.9), plus NotAvailable for a 404.
    private suspend fun fetch(restaurantId: String, cached: MenuState.Content?): MenuState {
        val menu = try {
            val response = api.menu(restaurantId)
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                if (response.code() == HTTP_NOT_FOUND) return notAvailable(restaurantId)
                return MenuState.Error(SyncError.Server(response.code()), cached)
            }
            response.body()?.use { MenuParser.parse(json.parseToJsonElement(it.string()), restaurantId) }
        } catch (e: IOException) {
            return MenuState.Error(SyncError.Network, cached)
        } catch (e: IllegalArgumentException) {
            null // malformed JSON
        }
        return when {
            menu == null -> MenuState.Error(SyncError.BadData, cached)
            menu.categories.isEmpty() -> notAvailable(restaurantId)
            else -> {
                val fetchedAt = clock.instant()
                val menuJson = json.encodeToString(CachedMenu.serializer(), menu.toCached())
                dao.upsert(MenuEntity(restaurantId, menuJson, fetchedAt.toEpochMilli()))
                MenuState.Content(menu, fetchedAt)
            }
        }
    }

    // A 404 or an empty menu: "hasn't shared a menu yet", and no older menu is kept for it.
    private suspend fun notAvailable(restaurantId: String): MenuState {
        dao.delete(restaurantId)
        return MenuState.NotAvailable
    }
}
