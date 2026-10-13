package dev.dreamerslab.supabase

import com.google.appinventor.components.runtime.util.YailList
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

internal object Filters {

    fun apply(builder: PostgrestFilterBuilder, filters: YailList) {
        for (filter in filters.toArray()) {
            val (column, operator, value) = parts(filter)
            builder.filter(column, operator, format(value))
        }
    }

    private fun parts(filter: Any?): Triple<String, FilterOperator, Any?> {
        val items = (filter as? YailList)?.toArray()
        require(items != null && items.size == 3) { "Each filter is a list of column, operator and value. Use the Filter block." }
        val operator = FilterOperator.entries.firstOrNull { it.name.equals(items[1].toString(), ignoreCase = true) }
        requireNotNull(operator) { "Unknown filter operator ${items[1]}" }
        return Triple(items[0].toString(), operator, items[2])
    }

    private fun format(value: Any?): Any = (value as? YailList)?.toArray()?.map(Any?::toString) ?: value.toString()
}
