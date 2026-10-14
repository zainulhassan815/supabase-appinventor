package dev.dreamerslab.supabase

internal object Sorting {

    fun parse(text: String, ascending: Boolean): List<Pair<String, Boolean>> =
        text.split(',').map { it.trim().split(Regex("\\s+")) }.filter { it.first().isNotEmpty() }.map { words ->
            require(words.size <= 2) { "Can't sort by \"${words.joinToString(" ")}\"" }
            val isAscending = when (words.getOrNull(1)?.lowercase()) {
                null -> ascending
                "asc" -> true
                "desc" -> false
                else -> throw IllegalArgumentException("Order must be asc or desc, not ${words[1]}")
            }
            words.first() to isAscending
        }
}
