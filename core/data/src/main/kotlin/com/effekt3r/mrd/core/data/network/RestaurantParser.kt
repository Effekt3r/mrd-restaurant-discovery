package com.effekt3r.mrd.core.data.network

import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.MINUTES_PER_DAY
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import java.time.DayOfWeek
import kotlin.math.roundToInt
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** One restaurant that passed spec §5.1's validation, in the server's order. */
internal data class ParsedRestaurant(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val rating: Rating?,
    val deliveryFeeCents: Long?,
    val etaMinutes: Int?,
    val isOpen: Boolean?,
    val imageUrl: String?,
    val hours: OpeningHours?
)

/** How many records the payload held, and the ones that passed. All of them failing is bad data (spec §5.3). */
internal data class ParsedPayload(val recordCount: Int, val restaurants: List<ParsedRestaurant>)

/**
 * Reads `{"restaurants": [...]}` one record at a time and validates each as soon as it's read, so a 10,000-restaurant
 * payload never sits in memory as a JSON tree. Malformed JSON, or a payload without the array, throws
 * [SerializationException].
 */
internal object RestaurantsPayloadReader : DeserializationStrategy<ParsedPayload> {
    private val records = ListSerializer(JsonElement.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RestaurantsPayload") {
        element("restaurants", records.descriptor)
    }

    override fun deserialize(decoder: Decoder): ParsedPayload = decoder.decodeStructure(descriptor) {
        var payload: ParsedPayload? = null
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> payload = decodeSerializableElement(descriptor, 0, RecordsReader)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected element $index")
            }
        }
        payload ?: throw SerializationException("The payload has no restaurants array")
    }

    private object RecordsReader : DeserializationStrategy<ParsedPayload> {
        override val descriptor: SerialDescriptor = records.descriptor

        override fun deserialize(decoder: Decoder): ParsedPayload = decoder.decodeStructure(descriptor) {
            var count = 0
            val ids = HashSet<String>()
            val restaurants = ArrayList<ParsedRestaurant>()
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                count++
                // The id is the primary key, so a repeated id keeps the first record (spec §5.5).
                RestaurantValidator.validate(decodeSerializableElement(descriptor, index, JsonElement.serializer()))
                    ?.takeIf { ids.add(it.id) }
                    ?.let(restaurants::add)
            }
            ParsedPayload(count, restaurants)
        }
    }
}

/** Spec §5.1's rules for one record: no usable id or name drops it, and any other bad value reads as missing. */
internal object RestaurantValidator {
    private val days = DayOfWeek.entries.associateBy { it.name }
    private val time = Regex("""([01]\d|2[0-4]):([0-5]\d)""")

    fun validate(record: JsonElement): ParsedRestaurant? {
        val fields = record as? JsonObject ?: return null
        return ParsedRestaurant(
            id = fields.text("id") ?: return null,
            name = fields.text("name") ?: return null,
            cuisines = fields.array("cuisines").mapNotNull { it.textValue() }.distinct(),
            rating = fields.double("rating")?.takeIf { it in 0.0..5.0 }?.let { Rating((it * 10).roundToInt()) },
            deliveryFeeCents = fields.long("delivery_fee_cents")?.takeIf { it >= 0 },
            etaMinutes = fields.long("eta_minutes")?.takeIf { it in 1..Int.MAX_VALUE }?.toInt(),
            isOpen = fields.boolean("is_open"),
            imageUrl = fields.httpsUrl("image_url"),
            hours = OpeningHours.from(fields.array("opening_hours").mapNotNull(::window))
        )
    }

    // A cuisine: a string that isn't blank. Blank entries are removed, and so are repeats (by distinct()).
    private fun JsonElement.textValue(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

    // An entry with an unknown day, a time that isn't HH:mm, or the same opening and closing time is dropped.
    private fun window(entry: JsonElement): DailyWindow? {
        val fields = entry as? JsonObject ?: return null
        val day = fields.string("day")?.let(days::get) ?: return null
        val opens = fields.string("opens")?.let(::minutes)?.takeIf { it < MINUTES_PER_DAY } ?: return null
        val closes = fields.string("closes")?.let(::minutes) ?: return null
        return if (opens == closes) null else DailyWindow(day, opens, closes)
    }

    /** "HH:mm" as minutes after midnight, where "24:00" is 1,440. "25:00" or "24:30" are null. */
    private fun minutes(text: String): Int? = time.matchEntire(text)
        ?.destructured
        ?.let { (hours, minutes) -> hours.toInt() * 60 + minutes.toInt() }
        ?.takeIf { it <= MINUTES_PER_DAY }
}
