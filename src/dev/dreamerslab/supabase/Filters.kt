package dev.dreamerslab.supabase

import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

internal sealed interface Filter {
    data class Condition(val column: String, val operator: FilterOperator, val value: Any) : Filter
    data class AnyOf(val filters: List<Filter>) : Filter
    data class AllOf(val filters: List<Filter>) : Filter
    data class Not(val filter: Filter) : Filter
}

internal object Filters {

    const val ANY_OF = "anyOf"
    const val ALL_OF = "allOf"
    const val NOT = "not"

    fun apply(builder: PostgrestFilterBuilder, filters: YailList) = apply(builder, parseAll(filters, "filters"))

    fun apply(builder: PostgrestFilterBuilder, filters: List<Filter>) = filters.forEach { add(builder, it, negate = false) }

    fun parseAll(filters: YailList, path: String): List<Filter> =
        filters.toArray().mapIndexed { index, filter -> parse(filter, "$path[${index + 1}]") }

    fun parse(filter: Any?, path: String): Filter = when (filter) {
        is YailDictionary -> group(filter, path)
        else -> condition(filter, path)
    }

    fun condition(filter: Any?, path: String): Filter.Condition {
        val items = (filter as? YailList)?.toArray()
        if (items == null || items.size != 3) {
            throw InvalidRecordException("$path: Each filter is a list of column, operator and value. Use the Filter block.")
        }
        val operator = FilterOperator.entries.firstOrNull { it.name.equals(items[1].toString(), ignoreCase = true) }
            ?: throw InvalidRecordException("$path: Unknown filter operator ${items[1]}")
        return Filter.Condition(items[0].toString(), operator, format(items[2]))
    }

    private fun add(builder: PostgrestFilterBuilder, filter: Filter, negate: Boolean) {
        when (filter) {
            is Filter.Condition ->
                if (negate) builder.filterNot(filter.column, filter.operator, filter.value)
                else builder.filter(filter.column, filter.operator, filter.value)
            is Filter.AnyOf -> builder.or(negate) { filter.filters.forEach { add(this, it, negate = false) } }
            is Filter.AllOf -> builder.and(negate) { filter.filters.forEach { add(this, it, negate = false) } }
            is Filter.Not -> add(builder, filter.filter, !negate)
        }
    }

    private fun group(filter: YailDictionary, path: String): Filter {
        val (key, value) = filter.entries.singleOrNull()
            ?: throw InvalidRecordException("$path: a group of filters comes from the AnyOf, AllOf or Not block.")
        return when (key.toString()) {
            ANY_OF -> Filter.AnyOf(list(value, "$path.$ANY_OF"))
            ALL_OF -> Filter.AllOf(list(value, "$path.$ALL_OF"))
            NOT -> Filter.Not(parse(value, "$path.$NOT"))
            else -> throw InvalidRecordException("$path: \"$key\" isn't $ANY_OF, $ALL_OF or $NOT.")
        }
    }

    private fun list(filters: Any?, path: String): List<Filter> {
        val items = (filters as? YailList)?.takeIf { it.size > 0 }
            ?: throw InvalidRecordException("$path must be a list of at least one filter.")
        return parseAll(items, path)
    }

    private fun format(value: Any?): Any = (value as? YailList)?.toArray()?.map(Any?::toString) ?: value.toString()
}
