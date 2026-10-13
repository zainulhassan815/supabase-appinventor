package dev.dreamerslab.supabase

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object ApiKeys {

    fun requirePublishable(key: String) {
        require(key.isNotBlank()) { "PublishableKey is empty" }
        require(!key.startsWith("sb_secret_") && jwtRole(key) != "service_role") {
            "PublishableKey is a secret key. Never put secret or service_role keys in an app: anyone can extract them. Use the publishable (anon) key."
        }
    }

    private fun jwtRole(key: String): String? = runCatching {
        val payload = String(Base64.decode(key.split('.')[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        Json.parseToJsonElement(payload).jsonObject["role"]?.jsonPrimitive?.content
    }.getOrNull()
}
