package com.effekt3r.mrd.core.ui

import com.effekt3r.mrd.core.model.SyncError

/**
 * What went wrong with fetching data, in the terms the screens word it in (spec §5.4). Each screen has its own copy
 * for each: Browse speaks of restaurants and Details of the menu.
 */
enum class SyncProblem {
    /** The device has no connection. */
    Offline,

    /** There's a connection, but the fetch failed: a timeout, a server error or data the app can't use. */
    CouldNotConnect,

    /** This device couldn't store the data, for example because its disk is full. Reconnecting won't fix it. */
    Storage
}

/**
 * The problem to show for the last fetch's [error] while the device is [isOnline], or null when there's nothing to
 * say. Being offline is a problem even before a fetch fails, so cached data can say how old it is. Storage comes
 * first: connecting again wouldn't help.
 */
fun syncProblem(error: SyncError?, isOnline: Boolean): SyncProblem? = when {
    error == SyncError.Storage -> SyncProblem.Storage
    !isOnline -> SyncProblem.Offline
    error != null -> SyncProblem.CouldNotConnect
    else -> null
}
