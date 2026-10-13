package dev.dreamerslab.supabase

import com.google.appinventor.components.runtime.Component
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.util.ErrorMessages
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class Requests(
    private val form: Form,
    private val component: Component,
    private val reportError: (operation: String, tag: String, code: String, message: String) -> Boolean,
) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun launch(operation: String, tag: String = "", block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(operation, tag, e)
            }
        }
    }

    fun fail(operation: String, tag: String, error: Throwable) {
        val code = errorCode(error)
        val message = errorMessage(error)
        if (!reportError(operation, tag, code, message)) {
            form.ErrorOccurred(component, operation, ErrorMessages.ERROR_EXTENSION_ERROR, message)
        }
    }

    fun close() = scope.cancel()

    private fun errorCode(error: Throwable) = when (error) {
        is PostgrestRestException -> error.code ?: error.error
        is AuthRestException -> error.error
        is RestException -> "http_${error.statusCode}"
        is HttpRequestException -> "network"
        is IllegalArgumentException -> "invalid_argument"
        else -> error::class.simpleName ?: "unknown"
    }

    private fun errorMessage(error: Throwable) = when (error) {
        is PostgrestRestException -> listOfNotNull(error.error, error.hint).joinToString(". ")
        is RestException -> error.description ?: error.error
        is HttpRequestException -> "Couldn't reach Supabase: ${error.cause?.message ?: error.message}"
        else -> error.message ?: error.toString()
    }
}
