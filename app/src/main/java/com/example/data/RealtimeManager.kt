package com.example.data

import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.Authorizer
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed interface RealtimeEvent {
    data class MessageCreated(val message: MessageResource) : RealtimeEvent
    data class ConversationSummaryUpdated(val conversation: ConversationResource) : RealtimeEvent
}

class RealtimeManager(
    private val sessionManager: SessionManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authorizer = Authorizer { channelName, socketId ->
        val token = sessionManager.getAuthToken().orEmpty()
        require(token.isNotBlank()) { "Token utilisateur manquant pour l'auth WebSocket." }

        val request = Request.Builder()
            .url("https://bizo.aiko.qzz.io/broadcasting/auth")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(
                FormBody.Builder()
                    .add("channel_name", channelName)
                    .add("socket_id", socketId)
                    .build()
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Auth WebSocket KO: HTTP ${response.code}")
            }

            response.body?.string().orEmpty()
        }
    }

    private val options = PusherOptions()
        .setUseTLS(true)
        .setHost("bizo.aiko.qzz.io")
        .setWsPort(80)
        .setWssPort(443)
        .setAuthorizer(authorizer)

    private val pusher = Pusher("eert8x7wnwzya7scgtan", options)
    private var didConnect = false

    private var threadChannelName: String? = null
    private var threadChannel: PrivateChannel? = null
    private var inboxChannelName: String? = null
    private var inboxChannel: PrivateChannel? = null

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events

    init {
        pusher.connection.bind(ConnectionState.ALL, object : ConnectionEventListener {
            override fun onConnectionStateChange(change: com.pusher.client.connection.ConnectionStateChange) {
                DebugLogger.info(
                    LogCategory.NAV,
                    "Etat WebSocket",
                    "${change.previousState} -> ${change.currentState}"
                )
            }

            override fun onError(message: String, code: String?, e: Exception?) {
                DebugLogger.error(
                    LogCategory.ERROR,
                    "Erreur WebSocket",
                    listOfNotNull(message, code, e?.message).joinToString(" | ")
                )
            }
        })
    }

    @Synchronized
    fun ensureConnected() {
        if (!didConnect) {
            DebugLogger.info(LogCategory.NAV, "Connexion WebSocket", "Host: bizo.aiko.qzz.io")
            pusher.connect()
            didConnect = true
        } else if (pusher.connection.state == ConnectionState.DISCONNECTED) {
            pusher.connect()
        }
    }

    @Synchronized
    fun subscribeToThread(conversationId: String) {
        ensureConnected()

        val targetChannel = "private-conversation.$conversationId"
        if (threadChannelName == targetChannel && threadChannel != null) {
            return
        }

        unsubscribeFromThread()

        val listener = object : PrivateChannelEventListener {
            override fun onAuthenticationFailure(message: String?, e: Exception?) {
                DebugLogger.error(
                    LogCategory.CONVERSATION,
                    "Auth canal thread echouee",
                    listOfNotNull(targetChannel, message, e?.message).joinToString(" | ")
                )
            }

            override fun onSubscriptionSucceeded(channelName: String?) {
                DebugLogger.success(LogCategory.CONVERSATION, "Abonnement thread OK", targetChannel)
            }

            override fun onEvent(event: com.pusher.client.channel.PusherEvent?) = Unit
        }

        val channel = pusher.subscribePrivate(targetChannel, listener)
        channel.bind("conversation.message.created", object : SubscriptionEventListener {
            override fun onEvent(event: com.pusher.client.channel.PusherEvent) {
                try {
                    val payload = json.decodeFromString<RealtimeMessageCreatedPayload>(event.data)
                    _events.tryEmit(RealtimeEvent.MessageCreated(payload.message))
                    DebugLogger.success(
                        LogCategory.MESSAGE,
                        "Message temps reel recu",
                        "Conv: ${payload.message.conv_id}, Msg: ${payload.message.id}"
                    )
                } catch (e: Exception) {
                    DebugLogger.error(LogCategory.ERROR, "Erreur parsing message temps reel", e.message)
                }
            }
        })

        threadChannelName = targetChannel
        threadChannel = channel
    }

    @Synchronized
    fun unsubscribeFromThread() {
        threadChannelName?.let { name ->
            runCatching { pusher.unsubscribe(name) }
            DebugLogger.info(LogCategory.CONVERSATION, "Desabonnement thread", name)
        }
        threadChannel = null
        threadChannelName = null
    }

    @Synchronized
    fun subscribeToInbox(userId: String) {
        ensureConnected()

        val targetChannel = "private-users.$userId.conversations"
        if (inboxChannelName == targetChannel && inboxChannel != null) {
            return
        }

        unsubscribeFromInbox()

        val listener = object : PrivateChannelEventListener {
            override fun onAuthenticationFailure(message: String?, e: Exception?) {
                DebugLogger.error(
                    LogCategory.CONVERSATION,
                    "Auth canal inbox echouee",
                    listOfNotNull(targetChannel, message, e?.message).joinToString(" | ")
                )
            }

            override fun onSubscriptionSucceeded(channelName: String?) {
                DebugLogger.success(LogCategory.CONVERSATION, "Abonnement inbox OK", targetChannel)
            }

            override fun onEvent(event: com.pusher.client.channel.PusherEvent?) = Unit
        }

        val channel = pusher.subscribePrivate(targetChannel, listener)
        channel.bind("conversation.summary.updated", object : SubscriptionEventListener {
            override fun onEvent(event: com.pusher.client.channel.PusherEvent) {
                try {
                    val payload = json.decodeFromString<RealtimeConversationSummaryPayload>(event.data)
                    _events.tryEmit(RealtimeEvent.ConversationSummaryUpdated(payload.conversation))
                    DebugLogger.success(
                        LogCategory.CONVERSATION,
                        "Resume conversation recu",
                        "Conv: ${payload.conversation.id}"
                    )
                } catch (e: Exception) {
                    DebugLogger.error(LogCategory.ERROR, "Erreur parsing resume conversation", e.message)
                }
            }
        })

        inboxChannelName = targetChannel
        inboxChannel = channel
    }

    @Synchronized
    fun unsubscribeFromInbox() {
        inboxChannelName?.let { name ->
            runCatching { pusher.unsubscribe(name) }
            DebugLogger.info(LogCategory.CONVERSATION, "Desabonnement inbox", name)
        }
        inboxChannel = null
        inboxChannelName = null
    }
}
