package dev.dreamerslab.supabase

import com.google.appinventor.components.common.OptionList
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList

internal class InvalidRecordException(message: String) : IllegalArgumentException(message)

internal class Fields<F : OptionList<String>>(value: Any?, private val path: String, private val choices: List<F>) {

    private val group = choices.first().javaClass.simpleName

    private val values: Map<F, Any?> = run {
        val dictionary = value as? YailDictionary ?: throw InvalidRecordException("$path must be a dictionary.")
        dictionary.entries.associate { (key, fieldValue) -> fieldFor(key) to fieldValue }
    }

    private fun fieldFor(key: Any?): F {
        if (key is Enum<*> && key is OptionList<*> && choices.none { it === key }) {
            throw InvalidRecordException("$path: ${key.javaClass.simpleName} ${key.name} doesn't belong here. Use $group keys.")
        }
        val name = nameOf(key)
        return choices.firstOrNull { it.toUnderlyingValue() == name } ?: throw InvalidRecordException(
            "$path: \"$name\" isn't a $group. Use ${choices.joinToString { it.toUnderlyingValue() }}."
        )
    }

    fun text(field: F): String? = when (val value = values[field]) {
        null -> null
        is CharSequence, is Number -> value.toString()
        else -> throw invalid(field, "text")
    }

    fun <T> text(field: F, read: (String) -> T): T? = text(field)?.let { checked(field.toUnderlyingValue()) { read(it) } }

    fun count(field: F): Int? {
        val number = when (val value = values[field]) {
            null -> return null
            is Number -> value.toDouble()
            is CharSequence -> value.toString().toDoubleOrNull()
            else -> null
        }
        if (number == null || number < 0 || number % 1 != 0.0) throw invalid(field, "a whole number, 0 or more")
        return number.toInt()
    }

    fun <T> list(field: F, read: (Any?) -> T): List<T> = when (val value = values[field]) {
        null -> emptyList()
        is YailList -> value.toArray().mapIndexed { index, item ->
            checked("${field.toUnderlyingValue()}[${index + 1}]") { read(item) }
        }
        else -> throw invalid(field, "a list")
    }

    private fun <T> checked(at: String, read: () -> T): T = try {
        read()
    } catch (e: InvalidRecordException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw InvalidRecordException("$path.$at: ${e.message}")
    }

    private fun invalid(field: F, expected: String) =
        InvalidRecordException("$path.${field.toUnderlyingValue()} must be $expected.")

    private fun nameOf(value: Any?): String = (value as? OptionList<*>)?.toUnderlyingValue()?.toString() ?: value.toString()
}
