package dev.dreamerslab.supabase

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

internal class PreferencesSessionManager(context: Context, private val key: String) : SessionManager {

    private val preferences = context.getSharedPreferences("dev.dreamerslab.supabase.sessions", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        preferences.edit().putString(key, Json.encodeToString(UserSession.serializer(), session)).apply()
    }

    override suspend fun loadSession(): UserSession =
        Json.decodeFromString(UserSession.serializer(), checkNotNull(preferences.getString(key, null)))

    override suspend fun deleteSession() {
        preferences.edit().remove(key).apply()
    }
}
