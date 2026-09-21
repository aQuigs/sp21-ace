package com.aquigs.sp21ace.data

/**
 * A history line's named fields, `key=value` apart by spaces. Fields are found by name, so a later one can join without breaking
 * the lines already saved.
 */
internal class LineFields(line: String) {
    private val fields = line.split(' ').groupBy({ it.substringBefore('=') }, { it.substringAfter('=') })

    // A field named twice, as in a cut-short line run on into the next, leaves no telling which record is meant
    operator fun get(key: String): String = fields.getValue(key).single()

    /** The field, or null on a line saved before it joined. */
    fun ifSaved(key: String): String? = fields[key]?.single()

    companion object {
        fun print(vararg fields: Pair<String, Any>): String = fields.joinToString(" ") { (key, value) -> "$key=$value" }
    }
}
