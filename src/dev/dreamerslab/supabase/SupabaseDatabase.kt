package dev.dreamerslab.supabase

import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.supabase.helpers.FilterOperator
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Extension(
    description = "Reads and writes the tables of a Supabase project, and calls its database functions. " +
        "Row-level security decides what the signed-in user may see and change.",
    icon = "icon.png",
)
class SupabaseDatabase(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener {

    private val requests = Requests(form, this, ::reportError)
    private var supabase: Supabase? = null

    init {
        form.registerForOnDestroy(this)
    }

    @SimpleProperty(description = "The Supabase component to use.")
    fun Client(): Supabase? = supabase

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT + ":dev.dreamerslab.supabase.Supabase")
    @SimpleProperty
    fun Client(client: Supabase) {
        supabase = client
    }

    @SimpleFunction(description = "Makes a filter for Select, Count, Update and Delete. For In, Contains, ContainedBy " +
        "and Overlaps, the value is a list.")
    fun Filter(column: String, @Options(FilterOperator::class) operator: String, value: Any): YailList =
        YailList.makeList(listOf(column, operator, value))

    @SimpleFunction(description = "Reads the rows of a table that match every filter. GotRows fires with the " +
        "tag and the rows, as a list of dictionaries. Columns is a comma-separated list, or * for all. Leave " +
        "orderBy empty to keep the database's order, and limit at 0 for no limit.")
    fun Select(
        tag: String,
        table: String,
        columns: String,
        filters: YailList,
        orderBy: String,
        ascending: Boolean,
        limit: Int,
        offset: Int,
    ) = requests.launch("Select", tag) {
        GotRows(tag, rows(postgrest().from(table).select(Columns.raw(columns.ifBlank { "*" })) {
            filter { Filters.apply(this, filters) }
            if (orderBy.isNotBlank()) order(orderBy, if (ascending) Order.ASCENDING else Order.DESCENDING)
            if (limit > 0) range(offset.toLong(), offset.toLong() + limit - 1)
        }))
    }

    @SimpleFunction(description = "Counts the rows of a table that match every filter. GotCount fires with the " +
        "tag and the count.")
    fun Count(tag: String, table: String, filters: YailList) = requests.launch("Count", tag) {
        val response = postgrest().from(table).select {
            head = true
            count(Count.EXACT)
            filter { Filters.apply(this, filters) }
        }
        GotCount(tag, response.countOrNull() ?: 0)
    }

    @SimpleFunction(description = "Adds rows to a table. Rows is a dictionary, or a list of dictionaries. " +
        "GotRows fires with the added rows, including the values the database filled in.")
    fun Insert(tag: String, table: String, rows: Any) = requests.launch("Insert", tag) {
        GotRows(tag, rows(postgrest().from(table).insert(rowList(rows)) { select() }))
    }

    @SimpleFunction(description = "Adds rows, or updates the rows that already exist. OnConflict lists the unique " +
        "columns that identify a row, comma-separated; leave it empty to use the primary key. GotRows fires " +
        "with the rows.")
    fun Upsert(tag: String, table: String, rows: Any, onConflict: String) = requests.launch("Upsert", tag) {
        GotRows(tag, rows(postgrest().from(table).upsert(rowList(rows)) {
            if (onConflict.isNotBlank()) this.onConflict = onConflict
            select()
        }))
    }

    @SimpleFunction(description = "Changes columns of every row that matches the filters. Values is a dictionary " +
        "of column names and new values. GotRows fires with the changed rows.")
    fun Update(tag: String, table: String, values: YailDictionary, filters: YailList) =
        requests.launch("Update", tag) {
            requireFilters(filters)
            GotRows(tag, rows(postgrest().from(table).update<JsonObject>(YailJson.toJsonObject(values)) {
                filter { Filters.apply(this, filters) }
                select()
            }))
        }

    @SimpleFunction(description = "Deletes every row that matches the filters. GotRows fires with the deleted rows.")
    fun Delete(tag: String, table: String, filters: YailList) = requests.launch("Delete", tag) {
        requireFilters(filters)
        GotRows(tag, rows(postgrest().from(table).delete {
            filter { Filters.apply(this, filters) }
            select()
        }))
    }

    @SimpleFunction(description = "Calls a database function (RPC) with a dictionary of arguments. GotResult fires " +
        "with its result: a list, a dictionary, or a single value.")
    fun CallFunction(tag: String, name: String, arguments: YailDictionary) = requests.launch("CallFunction", tag) {
        val response = postgrest().rpc(name, YailJson.toJsonObject(arguments))
        GotResult(tag, if (response.data.isBlank()) "" else YailJson.toYail(Json.parseToJsonElement(response.data)))
    }

    @SimpleEvent(description = "Rows arrived from Select, Insert, Upsert, Update or Delete.")
    fun GotRows(tag: String, rows: YailList) {
        EventDispatcher.dispatchEvent(this, "GotRows", tag, rows)
    }

    @SimpleEvent(description = "A count arrived from Count.")
    fun GotCount(tag: String, count: Long) {
        EventDispatcher.dispatchEvent(this, "GotCount", tag, count)
    }

    @SimpleEvent(description = "A database function called with CallFunction returned.")
    fun GotResult(tag: String, result: Any) {
        EventDispatcher.dispatchEvent(this, "GotResult", tag, result)
    }

    @SimpleEvent(description = "A database call failed. Code is the PostgREST or Postgres error code, or network. " +
        "Without this event, Screen.ErrorOccurred fires instead.")
    fun Error(tag: String, operation: String, code: String, message: String) {
        reportError(operation, tag, code, message)
    }

    override fun onDestroy() = requests.close()

    private fun reportError(operation: String, tag: String, code: String, message: String) =
        EventDispatcher.dispatchEvent(this, "Error", tag, operation, code, message)

    private fun postgrest() = requireNotNull(supabase) { "Set the Client property to a Supabase component" }.client.postgrest

    private fun rowList(rows: Any): List<JsonObject> = when (val json = YailJson.toJson(rows)) {
        is JsonArray -> json.map { requireNotNull(it as? JsonObject) { "Each row is a dictionary" } }
        else -> listOf(YailJson.toJsonObject(rows))
    }

    private fun requireFilters(filters: YailList) =
        require(filters.size > 0) { "Give at least one filter, so a missing filter can't change every row" }

    private fun rows(response: PostgrestResult): YailList =
        if (response.data.isBlank()) YailList() else YailJson.toYail(Json.parseToJsonElement(response.data)) as YailList
}
