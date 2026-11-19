package com.effekt3r.mrd.core.data.database

import java.text.Normalizer

private val combiningMarks = Regex("""\p{Mn}+""")
private val whitespace = Regex("""\s+""")

/** Lowercase, without accents, whitespace collapsed: "  Café   Fynbos " is "cafe fynbos" (spec §5.5). */
internal fun searchKey(text: String): String = Normalizer.normalize(
    text,
    Normalizer.Form.NFD
).replace(combiningMarks, "").lowercase().trim().replace(whitespace, " ")

/** [query]'s search key with LIKE's wildcards escaped, so "100%" matches a literal percent sign. */
internal fun likePattern(query: String): String =
    searchKey(query).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
