package com.matrimonyapp.data.notification

import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.security.TokenStore
import com.matrimonyapp.data.remote.MessageResponseDto
import com.matrimonyapp.data.remote.NotificationApi
import com.matrimonyapp.data.remote.NotificationDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NotificationRepositoryTest {
    private class FakeTokenStore(var value: String?) : TokenStore {
        override fun save(token: String) { value = token }
        override fun get(): String? = value
        override fun clear() { value = null }
    }

    private class FakeApi : NotificationApi {
        var listResult: () -> retrofit2.Response<List<NotificationDto>> = { retrofit2.Response.success(emptyList()) }
        var markResult: () -> retrofit2.Response<MessageResponseDto> =
            { retrofit2.Response.success(MessageResponseDto("Notification marked as read.")) }
        var listCalls = 0
        val listed = CountDownLatch(1)
        var slow = false
        val cancelled = CountDownLatch(1)

        override suspend fun list(): retrofit2.Response<List<NotificationDto>> {
            listCalls++
            listed.countDown()
            if (slow) {
                try {
                    kotlinx.coroutines.delay(60_000)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cancelled.countDown()
                    throw e
                }
            }
            return listResult()
        }

        override suspend fun markRead(id: String): retrofit2.Response<MessageResponseDto> = markResult()
    }

    private class FakeEventSource(private val request: Request) : EventSource {
        var cancelled = false
        override fun request(): Request = request
        override fun cancel() { cancelled = true }
    }

    private class FakeFactory : EventSource.Factory {
        val sources = mutableListOf<FakeEventSource>()
        val requests = mutableListOf<Request>()
        var listener: EventSourceListener? = null
        override fun newEventSource(request: Request, listener: EventSourceListener): EventSource {
            requests += request
            this.listener = listener
            return FakeEventSource(request).also { sources += it }
        }
    }

    private val api = FakeApi()
    private val factory = FakeFactory()
    private val tokenStore = FakeTokenStore("session-token")

    // Unconfined runs the stream coroutine eagerly on the calling thread, so tests are deterministic.
    private val repository = NotificationRepository(
        api, factory, "https://example.test/", tokenStore,
        CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    )

    private fun note(id: String, readAt: String? = null, title: String = "Title $id") =
        NotificationDto(id, "REPORT", title, "Body $id", readAt, "2026-09-19T10:00:00Z")

    private fun response(code: Int) = Response.Builder()
        .request(Request.Builder().url("https://example.test/").build())
        .protocol(Protocol.HTTP_1_1).code(code).message("x").body("".toResponseBody(null)).build()

    // ---- refresh ---------------------------------------------------------------------------

    @Test
    fun refreshPublishesTheServerList() = runBlocking {
        api.listResult = { retrofit2.Response.success(listOf(note("a"), note("b"))) }

        val result = repository.refresh()

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("a", "b"), repository.items.value.map { it.id })
    }

    @Test
    fun refreshCapsTheListSize() = runBlocking {
        api.listResult = { retrofit2.Response.success((1..250).map { note("n$it") }) }

        repository.refresh()

        assertEquals(100, repository.items.value.size)
    }

    @Test
    fun failedRefreshKeepsTheExistingList() = runBlocking {
        api.listResult = { retrofit2.Response.success(listOf(note("a"))) }
        repository.refresh()
        api.listResult = { throw IOException("offline") }

        val result = repository.refresh()

        assertEquals("NETWORK_ERROR", (result as AppResult.Failure).error.code)
        assertEquals(listOf("a"), repository.items.value.map { it.id })
    }

    @Test
    fun refreshUnauthorizedRequiresAuthentication() = runBlocking {
        api.listResult = { retrofit2.Response.error(401, """{"code":"UNAUTHORIZED","message":"x"}""".toResponseBody()) }

        assertTrue((repository.refresh() as AppResult.Failure).error.requiresAuthentication)
    }

    // ---- markRead --------------------------------------------------------------------------

    @Test
    fun markReadMarksOnlyThatItemAsRead() = runBlocking {
        api.listResult = { retrofit2.Response.success(listOf(note("a"), note("b"))) }
        repository.refresh()

        val result = repository.markRead("a")

        assertTrue(result is AppResult.Success)
        val items = repository.items.value.associateBy { it.id }
        assertTrue(items.getValue("a").readAt != null)
        assertNull(items.getValue("b").readAt)
    }

    @Test
    fun failedMarkReadLeavesTheItemUnread() = runBlocking {
        api.listResult = { retrofit2.Response.success(listOf(note("a"))) }
        repository.refresh()
        api.markResult = { retrofit2.Response.error(500, "".toResponseBody()) }

        val result = repository.markRead("a")

        assertTrue(result is AppResult.Failure)
        assertNull(repository.items.value.single().readAt)
    }

    @Test
    fun markReadRejectsABlankId() = runBlocking {
        assertEquals("VALIDATION_ERROR", (repository.markRead(" ") as AppResult.Failure).error.code)
    }

    // ---- live events -----------------------------------------------------------------------

    @Test
    fun notificationEventIsPrependedAndDeduplicated() {
        repository.handleEvent(
            "notification",
            """{"id":"n1","type":"REPORT","title":"First","body":"b","readAt":null,"createdAt":"2026-09-19T10:00:00Z"}"""
        )
        repository.handleEvent(
            "notification",
            """{"id":"n2","type":"REPORT","title":"Second","body":"b","readAt":null,"createdAt":"2026-09-19T10:01:00Z"}"""
        )
        repository.handleEvent(
            "notification",
            """{"id":"n1","type":"REPORT","title":"First again","body":"b","readAt":null,"createdAt":"2026-09-19T10:00:00Z"}"""
        )

        assertEquals(listOf("n1", "n2"), repository.items.value.map { it.id })
        assertEquals("First again", repository.items.value.first().title)
    }

    @Test
    fun readyEventResyncsFromTheServer() {
        repository.start()

        repository.handleEvent("ready", "connected")

        assertTrue("refresh should be triggered", api.listed.await(2, TimeUnit.SECONDS))
        repository.stop()
    }

    @Test
    fun malformedNotificationEventFallsBackToARefreshInsteadOfCrashing() {
        repository.start()

        repository.handleEvent("notification", "{not json")

        assertTrue(api.listed.await(2, TimeUnit.SECONDS))
        assertTrue(repository.items.value.isEmpty())
        repository.stop()
    }

    @Test
    fun stopCancelsARefreshThatIsStillInFlight() {
        api.slow = true
        repository.start()
        repository.handleEvent("ready", "connected")
        assertTrue("refresh should have started", api.listed.await(2, TimeUnit.SECONDS))

        repository.stop()

        assertTrue("in-flight refresh must be cancelled by stop()", api.cancelled.await(2, TimeUnit.SECONDS))
        assertFalse(repository.isStreaming)
    }

    @Test
    fun eventsArrivingAfterStopDoNotStartAnyWork() {
        repository.start()
        repository.stop()

        repository.handleEvent("ready", "connected")
        repository.handleEvent("notification", "{not json")

        assertEquals(0, api.listCalls)
    }

    @Test
    fun theStreamCanBeRestartedAfterStop() {
        repository.start()
        repository.stop()

        repository.start()

        assertEquals(2, factory.requests.size)
        assertTrue(repository.isStreaming)
        repository.stop()
    }

    @Test
    fun unknownEventTypesAreIgnored() {
        repository.handleEvent("something-else", "x")

        assertEquals(0, api.listCalls)
        assertTrue(repository.items.value.isEmpty())
    }

    // ---- stream lifecycle ------------------------------------------------------------------

    @Test
    fun startOpensTheAuthenticatedStreamAndStopCancelsIt() {
        repository.start()

        assertEquals(1, factory.requests.size)
        assertEquals("https://example.test/api/v1/notifications/stream", factory.requests[0].url.toString())
        assertEquals("text/event-stream", factory.requests[0].header("Accept"))
        assertTrue(repository.isStreaming)

        repository.stop()

        assertTrue(factory.sources[0].cancelled)
        assertFalse(repository.isStreaming)
    }

    @Test
    fun startIsIdempotent() {
        repository.start()
        repository.start()

        assertEquals(1, factory.requests.size)
        repository.stop()
    }

    @Test
    fun startDoesNothingWhenSignedOut() {
        tokenStore.value = null

        repository.start()

        assertEquals(0, factory.requests.size)
        assertFalse(repository.isStreaming)
    }

    @Test
    fun anUnauthorizedStreamStopsInsteadOfRetrying() {
        repository.start()
        val source = factory.sources[0]

        factory.listener!!.onFailure(source, null, response(401))

        assertFalse("must not keep retrying after HTTP 401", repository.isStreaming)
        assertEquals(1, factory.requests.size)
    }

    @Test
    fun aTransientFailureKeepsTheStreamAliveToReconnectLater() {
        repository.start()

        factory.listener!!.onFailure(factory.sources[0], IOException("reset"), null)

        assertTrue("should be waiting to reconnect", repository.isStreaming)
        repository.stop()
        assertFalse(repository.isStreaming)
    }

    @Test
    fun aMalformedBaseUrlStopsTheStreamInsteadOfCrashing() {
        val broken = NotificationRepository(
            api, factory, "not a url", tokenStore, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )

        broken.start()

        assertFalse(broken.isStreaming)
        assertEquals(0, factory.requests.size)
    }
}
