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
import dev.dreamerslab.supabase.helpers.ChangeEvent
import dev.dreamerslab.supabase.helpers.ChannelStatus
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.realtime.HasOldRecord
import io.github.jan.supabase.realtime.HasRecord
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresChangeFilter
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast.BroadcastPayload
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Extension(
    description = "A realtime channel: receives database changes as they happen, and sends messages and " +
        "online status between the users of the same channel.",
    icon = "icon.png",
)
class SupabaseChannel(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener {

    private val requests = Requests(form, this) { operation, _, code, message -> reportError(operation, code, message) }
    private val listeners = mutableListOf<(RealtimeChannel, CoroutineScope) -> Unit>()
    private var supabase: Supabase? = null
    private var topic = ""
    private var receiveOwnBroadcasts = true
    private var presenceKey = ""
    private var isPrivate = false
    private var channel: RealtimeChannel? = null
    private var channelScope: CoroutineScope? = null

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

    @SimpleProperty(description = "The channel's name. Users on the same topic share broadcasts and presence.")
    fun Topic() = topic

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    fun Topic(name: String) {
        topic = name.trim()
    }

    @SimpleProperty(description = "Whether BroadcastReceived also fires for this app's own broadcasts.")
    fun ReceiveOwnBroadcasts() = receiveOwnBroadcasts

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ReceiveOwnBroadcasts(receive: Boolean) {
        receiveOwnBroadcasts = receive
    }

    @SimpleProperty(description = "The name this app's presence is tracked under, such as the user's id. " +
        "Leave empty for a random one.")
    fun PresenceKey() = presenceKey

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty
    fun PresenceKey(key: String) {
        presenceKey = key.trim()
    }

    @SimpleProperty(description = "Whether only users allowed by the project's policies on realtime.messages " +
        "may join the channel, send broadcasts and track presence. Set it before Subscribe.")
    fun Private() = isPrivate

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun Private(private: Boolean) {
        isPrivate = private
    }

    @SimpleProperty(description = "Whether the channel is subscribed.")
    fun IsSubscribed() = channel?.status?.value == RealtimeChannel.Status.SUBSCRIBED

    @OptIn(SupabaseInternal::class)
    @SimpleProperty(description = "Everyone on the channel right now, as a dictionary from presence key to " +
        "tracked state. Empty until the channel is subscribed.")
    fun PresenceState(): YailDictionary = presences(channel?.callbackManager?.presenceState().orEmpty())

    @SimpleFunction(description = "Receives changes to a table through ChangeReceived. Call it before Subscribe. " +
        "Filter is empty, or one filter from SupabaseDatabase's Filter block (Equals, NotEquals, GreaterThan, " +
        "GreaterOrEqual, LessThan, LessOrEqual or In). Realtime must be enabled for the table, and row-level " +
        "security decides which changes arrive.")
    fun ListenToTable(schema: String, table: String, @Options(ChangeEvent::class) event: String, filter: Any) =
        requests.run("ListenToTable") {
            requireUnsubscribed()
            val condition = (filter as? YailList)
                ?.takeIf { it.size > 0 }
                ?.let(Filters::parse)
            val kind = requireNotNull(ChangeEvent.fromUnderlyingValue(event)) { "Unknown change event $event" }
            val target: PostgresChangeFilter.() -> Unit = {
                this.table = table
                condition?.let { (column, operator, value) -> filter(column, operator, value) }
            }
            listeners += { channel, scope ->
                changes(channel, schema.ifBlank { "public" }, kind, target)
                    .onEach { change(table, it) }
                    .launchNow(scope)
            }
        }

    @SimpleFunction(description = "Receives broadcasts with the given event name through BroadcastReceived. " +
        "Call it before Subscribe.")
    fun ListenToBroadcast(event: String) = requests.run("ListenToBroadcast") {
        requireUnsubscribed()
        listeners += { channel, scope ->
            channel.broadcastFlow(event)
                .onEach { BroadcastReceived(it.event, payload(it.payload)) }
                .launchNow(scope)
        }
    }

    @SimpleFunction(description = "Connects the channel. StatusChanged fires with Subscribed once it receives. " +
        "Listeners added with ListenToTable and ListenToBroadcast stay, so Subscribe works again after " +
        "Unsubscribe.")
    fun Subscribe() = requests.launch("Subscribe") {
        requireUnsubscribed()
        require(topic.isNotBlank()) { "Set the Topic property" }
        val created = client().channel(topic) {
            isPrivate = this@SupabaseChannel.isPrivate
            broadcast { receiveOwnBroadcasts = this@SupabaseChannel.receiveOwnBroadcasts }
            if (presenceKey.isNotBlank()) presence { key = presenceKey }
        }
        val scope = CoroutineScope(requests.scope.coroutineContext + Job(requests.scope.coroutineContext[Job]))
        created.status.drop(1).mapNotNull(::channelStatus).onEach { StatusChanged(it.toUnderlyingValue()) }.launchNow(scope)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val leftBeforeJoining = created.status.drop(1)
                .takeWhile { it != RealtimeChannel.Status.SUBSCRIBED }
                .firstOrNull { it == RealtimeChannel.Status.UNSUBSCRIBING || it == RealtimeChannel.Status.UNSUBSCRIBED }
            if (leftBeforeJoining != null) requests.launch("Subscribe") { refused(created) }
        }
        created.presenceChangeFlow()
            .onEach { PresenceChanged(presences(it.joins), presences(it.leaves)) }
            .launchNow(scope)
        listeners.forEach { it(created, scope) }
        channel = created
        channelScope = scope
        created.subscribe()
    }

    @SimpleFunction(description = "Disconnects the channel. StatusChanged fires with Unsubscribed.")
    fun Unsubscribe() = requests.launch("Unsubscribe") {
        val current = channel ?: return@launch
        closeChannel(current)
        StatusChanged(ChannelStatus.Unsubscribed.toUnderlyingValue())
    }

    @SimpleFunction(description = "Sends a message to everyone subscribed to the channel. Payload is a dictionary.")
    fun Broadcast(event: String, payload: YailDictionary) = requests.launch("Broadcast") {
        subscribed().broadcast(event, BroadcastPayload.Json(YailJson.toJsonObject(payload)))
    }

    @SimpleFunction(description = "Shares this app's state, such as a name or status, with everyone on the " +
        "channel. PresenceChanged fires on every app, including this one.")
    fun Track(state: YailDictionary) = requests.launch("Track") {
        subscribed().track(YailJson.toJsonObject(state))
    }

    @SimpleFunction(description = "Stops sharing this app's state.")
    fun Untrack() = requests.launch("Untrack") {
        subscribed().untrack()
    }

    @SimpleEvent(description = "The channel connected or disconnected.")
    fun StatusChanged(@Options(ChannelStatus::class) status: String) {
        EventDispatcher.dispatchEvent(this, "StatusChanged", status)
    }

    @SimpleEvent(description = "A row changed. Record is the row after the change, and oldRecord the row " +
        "before it (for Update and Delete; it has only the primary key unless the table's replica identity is " +
        "full).")
    fun ChangeReceived(table: String, @Options(ChangeEvent::class) event: String, record: YailDictionary, oldRecord: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "ChangeReceived", table, event, record, oldRecord)
    }

    @SimpleEvent(description = "A broadcast arrived. Payload is usually a dictionary.")
    fun BroadcastReceived(event: String, payload: Any) {
        EventDispatcher.dispatchEvent(this, "BroadcastReceived", event, payload)
    }

    @SimpleEvent(description = "Users joined or left the channel, or changed their tracked state. Joins and " +
        "leaves are dictionaries from presence key to state.")
    fun PresenceChanged(joins: YailDictionary, leaves: YailDictionary) {
        EventDispatcher.dispatchEvent(this, "PresenceChanged", joins, leaves)
    }

    @SimpleEvent(description = "A channel call failed. Without this event, Screen.ErrorOccurred fires instead.")
    fun Error(operation: String, code: String, message: String) {
        reportError(operation, code, message)
    }

    override fun onDestroy() = requests.close()

    private fun reportError(operation: String, code: String, message: String) =
        EventDispatcher.dispatchEvent(this, "Error", operation, code, message)

    private fun client() = requireNotNull(supabase) { "Set the Client property to a Supabase component" }.client

    private fun subscribed() = requireNotNull(channel) { "Subscribe first" }

    private fun requireUnsubscribed() = require(channel == null) { "Unsubscribe first" }

    private suspend fun refused(current: RealtimeChannel) {
        if (channel !== current) return
        closeChannel(current)
        StatusChanged(ChannelStatus.Unsubscribed.toUnderlyingValue())
        throw CodedException(
            "channel_refused",
            "The server refused to join $topic. " + if (isPrivate) {
                "Add policies on realtime.messages that let this user in."
            } else {
                "The session may have expired."
            },
        )
    }

    private fun <T> Flow<T>.launchNow(scope: CoroutineScope) = scope.launch(start = CoroutineStart.UNDISPATCHED) { collect() }

    private suspend fun closeChannel(current: RealtimeChannel) {
        channel = null
        channelScope?.cancel()
        channelScope = null
        client().realtime.removeChannel(current)
    }

    private fun changes(
        channel: RealtimeChannel,
        schema: String,
        kind: ChangeEvent,
        target: PostgresChangeFilter.() -> Unit,
    ): Flow<PostgresAction> = when (kind) {
        ChangeEvent.All -> channel.postgresChangeFlow<PostgresAction>(schema, target)
        ChangeEvent.Insert -> channel.postgresChangeFlow<PostgresAction.Insert>(schema, target)
        ChangeEvent.Update -> channel.postgresChangeFlow<PostgresAction.Update>(schema, target)
        ChangeEvent.Delete -> channel.postgresChangeFlow<PostgresAction.Delete>(schema, target)
    }

    private fun change(table: String, action: PostgresAction) {
        val event = when (action) {
            is PostgresAction.Insert -> ChangeEvent.Insert
            is PostgresAction.Update -> ChangeEvent.Update
            is PostgresAction.Delete -> ChangeEvent.Delete
            else -> return
        }
        ChangeReceived(
            table,
            event.toUnderlyingValue(),
            dictionary((action as? HasRecord)?.record),
            dictionary((action as? HasOldRecord)?.oldRecord),
        )
    }

    private fun channelStatus(status: RealtimeChannel.Status) = when (status) {
        RealtimeChannel.Status.SUBSCRIBING -> ChannelStatus.Subscribing
        RealtimeChannel.Status.SUBSCRIBED -> ChannelStatus.Subscribed
        RealtimeChannel.Status.UNSUBSCRIBED -> ChannelStatus.Unsubscribed
        RealtimeChannel.Status.UNSUBSCRIBING -> null
    }

    private fun payload(payload: BroadcastPayload): Any = when (payload) {
        is BroadcastPayload.Json -> YailJson.toYail(payload.value)
        else -> ""
    }

    private fun presences(presences: Map<String, Presence>): YailDictionary =
        YailDictionary.makeDictionary(presences.mapValues { YailJson.toYail(it.value.state) })

    private fun dictionary(json: JsonObject?): YailDictionary =
        json?.let { YailJson.toYail(it) as YailDictionary } ?: YailDictionary()
}
