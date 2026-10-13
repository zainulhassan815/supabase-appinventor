package dev.dreamerslab.supabase

import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import gnu.math.IntFraction
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal object YailJson {

    fun toYail(element: JsonElement): Any = when (element) {
        is JsonObject -> YailDictionary.makeDictionary(element.mapValues { toYail(it.value) })
        is JsonArray -> YailList.makeList(element.map(::toYail))
        JsonNull -> ""
        is JsonPrimitive -> primitive(element)
    }

    fun toJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is YailDictionary -> JsonObject(value.entries.associate { (key, item) -> key.toString() to toJson(item) })
        is YailList -> JsonArray(value.toArray().map(::toJson))
        is Map<*, *> -> JsonObject(value.entries.associate { (key, item) -> key.toString() to toJson(item) })
        is List<*> -> JsonArray(value.map(::toJson))
        is Boolean -> JsonPrimitive(value)
        is IntFraction -> JsonPrimitive(value.toDouble())
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    fun toJsonObject(value: Any?): JsonObject =
        toJson(value) as? JsonObject ?: throw IllegalArgumentException("Expected a dictionary")

    private fun primitive(element: JsonPrimitive): Any = when {
        element.isString -> element.content
        else -> element.booleanOrNull ?: element.longOrNull ?: element.doubleOrNull ?: element.content
    }
}
