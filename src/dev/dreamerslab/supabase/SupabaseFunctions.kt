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
import dev.dreamerslab.supabase.helpers.Region
import dev.dreamerslab.supabase.helpers.RequestMethod
import io.github.jan.supabase.functions.FunctionRegion
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json

@Extension(
    description = "Calls the Edge Functions of a Supabase project: server code that runs with the signed-in " +
        "user's identity, for work an app can't do on its own, like calling paid APIs with secret keys.",
    icon = "icon.png",
)
class SupabaseFunctions(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener {

    private val requests = Requests(form, this, ::reportError)
    private var supabase: Supabase? = null
    private var region = Region.Any

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

    @SimpleProperty(description = "Where functions run. Choose the region of your database when a function " +
        "uses it a lot, so its queries are fast.")
    @Options(Region::class)
    fun Region(): String = region.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["any", "ap-northeast-1", "ap-northeast-2", "ap-south-1", "ap-southeast-1", "ap-southeast-2",
            "ca-central-1", "eu-central-1", "eu-west-1", "eu-west-2", "eu-west-3", "sa-east-1", "us-east-1",
            "us-west-1", "us-west-2"],
        defaultValue = "any",
    )
    @SimpleProperty
    fun Region(@Options(Region::class) region: String) = requests.run("Region") {
        this.region = requireNotNull(Region.fromUnderlyingValue(region)) { "Unknown region: $region" }
    }

    @SimpleFunction(description = "Calls an Edge Function. Body is a dictionary or list (sent as JSON), text, or " +
        "empty for none. Headers is a dictionary of extra HTTP headers. GotResponse fires with the response; " +
        "a JSON response arrives as a dictionary or list.")
    fun Invoke(
        tag: String,
        name: String,
        body: Any,
        headers: YailDictionary,
        @Options(RequestMethod::class) method: String,
    ) = requests.launch("Invoke", tag) {
        val response = client().functions.invoke(name, FunctionRegion.entries.first { it.value == region.toUnderlyingValue() }) {
            this.method = HttpMethod.parse(method)
            for ((key, value) in headers.entries) header(key.toString(), value.toString())
            body(this, body)
        }
        GotResponse(tag, response.status.value, content(response))
    }

    @SimpleEvent(description = "An Edge Function answered. Status is the HTTP status code.")
    fun GotResponse(tag: String, status: Int, body: Any) {
        EventDispatcher.dispatchEvent(this, "GotResponse", tag, status, body)
    }

    @SimpleEvent(description = "A function call failed, or the function answered with an error status. Code is " +
        "http_ and the status, such as http_404, or network. Without this event, Screen.ErrorOccurred fires " +
        "instead.")
    fun Error(tag: String, operation: String, code: String, message: String) {
        reportError(operation, tag, code, message)
    }

    override fun onDestroy() = requests.close()

    private fun reportError(operation: String, tag: String, code: String, message: String) =
        EventDispatcher.dispatchEvent(this, "Error", tag, operation, code, message)

    private fun client() = requireNotNull(supabase) { "Set the Client property to a Supabase component" }.client

    private fun body(request: HttpRequestBuilder, body: Any) = when {
        body is YailDictionary || body is YailList ->
            request.setBody(TextContent(YailJson.toJson(body).toString(), ContentType.Application.Json))
        body.toString().isNotEmpty() -> request.setBody(TextContent(body.toString(), ContentType.Text.Plain))
        else -> Unit
    }

    private suspend fun content(response: HttpResponse): Any {
        val text = response.bodyAsText()
        val isJson = response.headers["Content-Type"]?.startsWith("application/json") == true
        return if (isJson && text.isNotBlank()) YailJson.toYail(Json.parseToJsonElement(text)) else text
    }
}
