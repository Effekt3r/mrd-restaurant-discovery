package com.effekt3r.mrd.core.model

import java.time.Instant

/** What the screens know about the cached snapshot (spec §5.4). [lastSyncedAt] is null until the first sync. */
data class SyncStatus(val lastSyncedAt: Instant?, val isRefreshing: Boolean, val lastError: SyncError?)

/** Why a refresh failed. :core:data maps every failure to exactly one of these. */
sealed interface SyncError {
    /** No connection, a DNS failure or a timeout. */
    data object Network : SyncError

    data class Server(val httpCode: Int) : SyncError

    /** Malformed JSON, or a payload whose records all failed validation. */
    data object BadData : SyncError

    /** The database failed, for example because the disk is full. */
    data object Storage : SyncError
}
