package dev.dreamerslab.supabase

import android.os.Handler
import android.os.Looper
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.Component
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.ReplForm
import com.google.appinventor.components.runtime.util.OnInitializeListener
import com.google.appinventor.components.runtime.util.YailDictionary
import dev.dreamerslab.supabase.helpers.SessionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.github.jan.supabase.auth.status.SessionStatus as AuthStatus

@Extension(
    description = "Connects the app to a Supabase project and signs users in. " +
        "Other Supabase components use it through their Client property.",
    icon = "icon.png",
)
class Supabase(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), Component, OnInitializeListener, OnDestroyListener {

    private val requests = Requests(form, this) { operation, _, code, message -> reportError(operation, code, message) }
    private var projectUrl = ""
    private var publishableKey = ""
    private var connection: SupabaseClient? = null

    init {
        if (form is ReplForm) Handler(Looper.getMainLooper()).post(::restoreSession) else form.registerForOnInitialize(this)
        form.registerForOnDestroy(this)
    }

    internal val client: SupabaseClient
        get() = connection ?: connect().also { connection = it }

    @SimpleProperty(description = "The project URL, for example https://abcd.supabase.co.")
    fun ProjectUrl() = projectUrl

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    fun ProjectUrl(url: String) {
        projectUrl = url.trim().trimEnd('/')
        disconnect()
    }

    @SimpleProperty(description = "The project's publishable (anon) key. Never use a secret or service_role key.")
    fun PublishableKey() = publishableKey

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    fun PublishableKey(key: String) {
        publishableKey = key.trim()
        disconnect()
    }

    @SimpleProperty(description = "Whether a user is signed in.")
    fun IsSignedIn() = connection?.auth?.currentSessionOrNull() != null

    @SimpleProperty(description = "The signed-in user as a dictionary, or an empty dictionary.")
    fun CurrentUser(): YailDictionary = userDictionary(connection?.auth?.currentUserOrNull())

    @SimpleProperty(description = "The signed-in user's access token, for calling your own servers. Empty when signed out.")
    fun AccessToken() = connection?.auth?.currentAccessTokenOrNull().orEmpty()

    @SimpleFunction(description = "Creates an account with an email and password. Metadata is a dictionary " +
        "saved with the user. SignedUp fires with the new user; when the project doesn't require email " +
        "confirmation, the user is also signed in and SessionChanged fires.")
    fun SignUp(email: String, password: String, metadata: YailDictionary) = requests.launch("SignUp") {
        val user = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = YailJson.toJsonObject(metadata)
        }
        SignedUp(userDictionary(user ?: client.auth.currentUserOrNull()))
    }

    @SimpleFunction(description = "Signs in with an email and password. SessionChanged fires when it succeeds.")
    fun SignIn(email: String, password: String) = requests.launch("SignIn") {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    @SimpleFunction(description = "Signs in as a new anonymous user. Anonymous sign-ins must be enabled in the " +
        "project. SessionChanged fires when it succeeds.")
    fun SignInAnonymously() = requests.launch("SignInAnonymously") {
        client.auth.signInAnonymously()
    }

    @SimpleFunction(description = "Signs the user out and forgets the saved session. SessionChanged fires.")
    fun SignOut() = requests.launch("SignOut") {
        client.auth.signOut()
    }

    @SimpleFunction(description = "Changes the signed-in user. Leave email or password empty, or metadata an " +
        "empty dictionary, to keep them; metadata is merged into the saved metadata. A new email must be " +
        "confirmed through the link sent to it before it is used. Code is the code sent by Reauthenticate, needed " +
        "for a new password when the project turns on Secure password change; otherwise leave it empty. " +
        "Giving an anonymous user an email and then a password makes it a full account. UserUpdated fires.")
    fun UpdateUser(email: String, password: String, metadata: YailDictionary, code: String) =
        requests.launch("UpdateUser") {
            val user = client.auth.updateUser {
                if (email.isNotEmpty()) this.email = email
                if (password.isNotEmpty()) this.password = password
                if (metadata.isNotEmpty()) data = YailJson.toJsonObject(metadata)
                if (code.isNotEmpty()) nonce = code
            }
            UserUpdated(userDictionary(user))
        }

    @SimpleFunction(description = "Sends the sign-up confirmation email again. MessageSent fires.")
    fun ResendConfirmation(email: String) = requests.launch("ResendConfirmation") {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email)
        MessageSent("ResendConfirmation")
    }

    @SimpleFunction(description = "Sends the signed-in user a code by email, or by text message for a phone " +
        "account, to confirm a password change with UpdateUser. MessageSent fires.")
    fun Reauthenticate() = requests.launch("Reauthenticate") {
        client.auth.reauthenticate()
        MessageSent("Reauthenticate")
    }

    @SimpleEvent(description = "An account was created by SignUp.")
    fun SignedUp(user: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "SignedUp", user)
    }

    @SimpleEvent(description = "UpdateUser changed the user.")
    fun UserUpdated(user: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "UserUpdated", user)
    }

    @SimpleEvent(description = "An email or text message was sent. Operation is the block that sent it, such as " +
        "Reauthenticate.")
    fun MessageSent(operation: String) {
        EventDispatcher.dispatchEvent(this, "MessageSent", operation)
    }

    @SimpleEvent(description = "The user signed in or out, or a saved session was restored when the app started.")
    fun SessionChanged(@Options(SessionStatus::class) status: String, user: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "SessionChanged", status, user)
    }

    @SimpleEvent(description = "A Supabase call failed. Code is Supabase's error code, such as invalid_credentials, " +
        "or network when Supabase couldn't be reached. Without this event, Screen.ErrorOccurred fires instead.")
    fun Error(operation: String, code: String, message: String) {
        reportError(operation, code, message)
    }

    override fun onInitialize() = restoreSession()

    override fun onDestroy() {
        disconnect()
        requests.close()
    }

    private fun reportError(operation: String, code: String, message: String) =
        EventDispatcher.dispatchEvent(this, "Error", operation, code, message)

    private fun connect(): SupabaseClient {
        require(projectUrl.isNotBlank()) { "ProjectUrl is empty" }
        ApiKeys.requirePublishable(publishableKey)
        return createSupabaseClient(projectUrl, publishableKey) {
            httpEngine = OkHttp.create()
            defaultLogLevel = LogLevel.NONE
            install(Auth) {
                sessionManager = PreferencesSessionManager(form, projectUrl)
                codeVerifierCache = MemoryCodeVerifierCache()
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }.also(::watchSession)
    }

    private fun watchSession(client: SupabaseClient) {
        client.auth.sessionStatus
            .mapNotNull(::sessionStatus)
            .onEach { (status, user) -> SessionChanged(status.toUnderlyingValue(), userDictionary(user)) }
            .launchIn(requests.scope)
    }

    private fun sessionStatus(status: AuthStatus): Pair<SessionStatus, UserInfo?>? = when (status) {
        is AuthStatus.Authenticated -> SessionStatus.SignedIn to status.session.user
        is AuthStatus.NotAuthenticated -> SessionStatus.SignedOut to null
        is AuthStatus.RefreshFailure -> SessionStatus.RefreshFailed to null
        AuthStatus.Initializing -> null
    }

    private fun restoreSession() {
        if (projectUrl.isBlank() || publishableKey.isBlank()) return
        try {
            client
        } catch (e: IllegalArgumentException) {
            requests.fail("Connect", "", e)
        }
    }

    private fun disconnect() {
        connection?.let { requests.scope.launch { it.close() } }
        connection = null
    }

    private fun userDictionary(user: UserInfo?): YailDictionary =
        user?.let { YailJson.toYail(Json.encodeToJsonElement(UserInfo.serializer(), it)) as YailDictionary }
            ?: YailDictionary()
}
