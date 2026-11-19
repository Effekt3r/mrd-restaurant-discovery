package com.effekt3r.mrd.core.data.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Cuisines are stored as a JSON array (spec §5.5). */
internal class Converters {
    private val strings = ListSerializer(String.serializer())

    @TypeConverter
    fun cuisinesToJson(cuisines: List<String>): String = Json.encodeToString(strings, cuisines)

    @TypeConverter
    fun cuisinesFromJson(json: String): List<String> = Json.decodeFromString(strings, json)
}
