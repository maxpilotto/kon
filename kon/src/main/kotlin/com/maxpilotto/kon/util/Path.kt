package com.maxpilotto.kon.util

/**
 * Representation of a path in a JsonObject
 *
 * A path can be formed by keys (String) and indexes (either String or Number), the path's separators are slashes (/)
 * and a path can be surrounded by separators
 */
class Path {
    /**
     * Segments that form this path
     */
    val segments: List<Any>

    /**
     * Number of segments in this path
     */
    val length: Int
        get() = segments.size

    /**
     * Create a path from the given [segments]
     */
    constructor(vararg segments: Any) : this(segments.toList())

    /**
     * Creates a path from the given [source] containing the segments separated by the given [delimiter]
     */
    constructor(source: String, delimiter: String = DELIMITER) : this(
        source.removeSurrounding(DELIMITER).split(delimiter).let { list ->
            List(list.size) {
                list[it].toIntOrNull() ?: list[it]
            }
        }
    )

    /**
     * Creates a path from the given [segments]
     */
    constructor(segments: List<Any>) {
        if (segments.isEmpty()) {
            throw Exception("Path cannot be empty")
        } else {
            this.segments = segments
        }
    }

    override fun toString(): String {
        return segments.joinToString(DELIMITER, DELIMITER)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is String -> other == toString()
            is Path -> segments == other.segments

            else -> false
        }
    }

    override fun hashCode(): Int {
        return segments.hashCode()
    }

    operator fun iterator(): Iterator<Any> {
        return segments.iterator()
    }

    companion object {
        private const val DELIMITER = "/"
    }
}