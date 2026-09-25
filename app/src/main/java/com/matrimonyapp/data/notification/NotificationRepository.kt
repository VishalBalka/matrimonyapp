package com.matrimonyapp.data.notification

import com.google.gson.Gson
import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.security.TokenStore
import com.matrimonyapp.data.remote.ApiErrorMapper
import com.matrimonyapp.data.remote.NotificationApi
import com.matrimonyapp.data.remote.NotificationDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Notification list plus a live SSE stream.
 *
 * - refresh() loads GET /api/v1/notifications and replaces the list.
 * - start() opens GET /api/v1/notifications/stream and keeps it open, reconnecting with backoff.
 *   The event-source factory is built by ApiClient on the shared authentication interceptor, so
 *   the Bearer header and the clear-token-on-401 behaviour are not duplicated here.
 * - stop() cancels the stream.
 *
 * Server events: "ready" (connected) and "notification" (one NotificationDto as JSON).
 */
class NotificationRepository(
    private val api: NotificationApi,
    private val eventSourceFactory: EventSource.Factory,
    private val baseUrl: String,
    private val tokenStore: TokenStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val gson = Gson()
    private val _items = MutableStateFlow<List<NotificationDto>>(emptyList())
    val items: StateFlow<List<NotificationDto>> = _items.asStateFlow()

    // Everything started by start() (the stream loop and any refresh it triggers) lives under one
    // session job, so stop() cancels all of it in a single call.
    @Volatile
    private var session: Job? = null

    @Volatile
    private var job: Job? = null

    @Volatile
    private var source: EventSource? = null

    suspend fun refresh(): AppResult<List<NotificationDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.list()
            if (response.isSuccessful) {
                val list = (response.body() ?: emptyList()).take(MAX_ITEMS)
                _items.value = list
                AppResult.Success(list)
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun markRead(id: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (id.isBlank()) {
            return@withContext AppResult.Failure(AppError("VALIDATION_ERROR", "No notification was selected."))
        }
        try {
            val response = api.markRead(id)
            if (response.isSuccessful) {
                val readAt = Instant.now().toString()
                _items.update { current ->
                    current.map { if (it.id == id && it.readAt == null) it.copy(readAt = readAt) else it }
                }
                AppResult.Success(response.body()?.message ?: "Notification marked as read.")
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    /** True while the stream job is running (connected, or waiting to reconnect). */
    internal val isStreaming: Boolean
        get() = job?.isActive == true

    fun start() {
        if (job?.isActive == true) return
        session?.cancel()
        val newSession = SupervisorJob(scope.coroutineContext[Job])
        session = newSession
        job = scope.launch(newSession) { streamUntilStopped() }
    }

    fun stop() {
        session?.cancel()
        session = null
        job = null
        source?.cancel()
        source = null
    }

    private suspend fun streamUntilStopped() {
        var backoffMs = INITIAL_BACKOFF_MS
        while (currentCoroutineContext().isActive) {
            // Nothing to stream once signed out.
            if (tokenStore.get().isNullOrBlank()) return

            val outcome = connectOnce()
            if (outcome == StreamOutcome.STOP) return

            delay(backoffMs)
            backoffMs = if (outcome == StreamOutcome.ENDED_AFTER_OPEN) {
                INITIAL_BACKOFF_MS
            } else {
                (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private suspend fun connectOnce(): StreamOutcome {
        val request = try {
            Request.Builder()
                .url(baseUrl + STREAM_PATH)
                .header("Accept", "text/event-stream")
                .build()
        } catch (_: IllegalArgumentException) {
            return StreamOutcome.STOP
        }

        val ended = CompletableDeferred<StreamOutcome>()
        val opened = AtomicBoolean(false)

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                opened.set(true)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                handleEvent(type, data)
            }

            override fun onClosed(eventSource: EventSource) {
                ended.complete(if (opened.get()) StreamOutcome.ENDED_AFTER_OPEN else StreamOutcome.FAILED)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                ended.complete(
                    when {
                        // The shared interceptor has already cleared the token; do not keep retrying.
                        response?.code == 401 -> StreamOutcome.STOP
                        opened.get() -> StreamOutcome.ENDED_AFTER_OPEN
                        else -> StreamOutcome.FAILED
                    }
                )
            }
        }

        val eventSource = eventSourceFactory.newEventSource(request, listener)
        source = eventSource
        return try {
            ended.await()
        } finally {
            eventSource.cancel()
            if (source === eventSource) source = null
        }
    }

    internal fun handleEvent(type: String?, data: String) {
        when (type) {
            EVENT_NOTIFICATION -> {
                val parsed = runCatching { gson.fromJson(data, NotificationDto::class.java) }.getOrNull()
                if (parsed == null || parsed.id.isBlank()) {
                    launchRefresh()
                } else {
                    _items.update { current ->
                        (listOf(parsed) + current.filterNot { it.id == parsed.id }).take(MAX_ITEMS)
                    }
                }
            }
            // Connected (or reconnected): re-sync so nothing missed while offline is lost.
            EVENT_READY -> launchRefresh()
            else -> Unit
        }
    }

    /** Refresh triggered by the stream. Ignored once stop() has run, and cancelled by stop(). */
    private fun launchRefresh() {
        val active = session ?: return
        scope.launch(active) { refresh() }
    }

    private enum class StreamOutcome { ENDED_AFTER_OPEN, FAILED, STOP }

    private companion object {
        const val STREAM_PATH = "api/v1/notifications/stream"
        const val EVENT_NOTIFICATION = "notification"
        const val EVENT_READY = "ready"
        const val MAX_ITEMS = 100
        const val INITIAL_BACKOFF_MS = 3_000L
        const val MAX_BACKOFF_MS = 60_000L
    }
}
