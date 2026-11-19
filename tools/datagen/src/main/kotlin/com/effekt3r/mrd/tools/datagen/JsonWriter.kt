package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Writes JSON the same way every time: keys in insertion order, LF line endings and one trailing newline. Whoever
 * builds the [JsonObject] decides what's missing (a key left out) and what's an explicit `null` (`JsonNull`).
 * kotlinx.serialization escapes the strings; non-ASCII text such as "Café" is written as UTF-8.
 */
internal object JsonWriter {

    private const val INDENT = "  "

    /** The longest object or array that [pretty] keeps on one line. */
    private const val ONE_LINE_WIDTH = 80

    /** One line with no spaces, for the 10,000-restaurant file, where size matters more than reading it. */
    fun compact(element: JsonElement): String = Json.encodeToString(JsonElement.serializer(), element) + "\n"

    /**
     * Two-space indents, for files people read. An object or array that holds only values (no nested objects or
     * arrays) stays on one line when it fits, so cuisines and opening-hours entries read like spec §5.1's example.
     */
    fun pretty(element: JsonElement): String = buildString {
        appendPretty(element, depth = 0)
        append('\n')
    }

    private fun StringBuilder.appendPretty(element: JsonElement, depth: Int) {
        val oneLine = oneLine(element)
        when {
            oneLine != null -> append(oneLine)
            element is JsonObject -> appendBlock("{", "}", element.entries, depth) { (key, value) ->
                append(JsonPrimitive(key)).append(": ")
                appendPretty(value, depth + 1)
            }
            element is JsonArray -> appendBlock("[", "]", element, depth) { appendPretty(it, depth + 1) }
        }
    }

    private fun <T> StringBuilder.appendBlock(
        open: String,
        close: String,
        items: Collection<T>,
        depth: Int,
        appendItem: StringBuilder.(T) -> Unit
    ) {
        append(open).append('\n')
        items.forEachIndexed { index, item ->
            append(INDENT.repeat(depth + 1))
            appendItem(item)
            if (index < items.size - 1) append(',')
            append('\n')
        }
        append(INDENT.repeat(depth)).append(close)
    }

    /** [element] on one line, or null when it holds an object or array or is too long. Values always fit. */
    private fun oneLine(element: JsonElement): String? = when (element) {
        is JsonPrimitive -> element.toString()
        is JsonArray -> if (element.all { it is JsonPrimitive }) element.joinToString(", ", "[", "]") else null
        is JsonObject -> when {
            element.isEmpty() -> "{}"
            element.values.all { it is JsonPrimitive } ->
                element.entries.joinToString(", ", "{ ", " }") { (key, value) -> "${JsonPrimitive(key)}: $value" }
            else -> null
        }
    }?.takeIf { element is JsonPrimitive || it.length <= ONE_LINE_WIDTH }
}
