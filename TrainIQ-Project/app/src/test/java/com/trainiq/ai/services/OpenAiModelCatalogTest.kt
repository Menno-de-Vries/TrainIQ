package com.trainiq.ai.services

import com.trainiq.data.model.OpenAiModelDescriptor
import com.trainiq.data.model.OpenAiModelsResponse
import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.remote.OpenAiApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OpenAiModelCatalogTest {
    @Test
    fun select_prefersGpt6LunaWhenAccountListsIt() = runTest {
        val api = CatalogApi(listOf(models("gpt-5.4-mini", "gpt-5.6-luna", "gpt-6-luna")))

        assertEquals("gpt-6-luna", OpenAiModelCatalog(api).select("synthetic-secret"))
    }

    @Test
    fun select_refreshesFallbackOnlyDiscoverySoonEnoughToFindGpt6() = runTest {
        val api = CatalogApi(
            listOf(
                models("gpt-5.4-mini", "gpt-5.6-luna"),
                models("gpt-5.4-mini", "gpt-5.6-luna", "gpt-6-luna"),
            ),
        )
        var now = 0L
        val catalog = OpenAiModelCatalog(api, nowMillis = { now })

        assertEquals("gpt-5.6-luna", catalog.select("synthetic-secret"))
        assertEquals("gpt-5.6-luna", catalog.select("synthetic-secret"))
        assertEquals(1, api.listCalls)

        now += 15 * 60 * 1_000L
        assertEquals("gpt-6-luna", catalog.select("synthetic-secret"))
        assertEquals(2, api.listCalls)
    }

    @Test
    fun select_keepsPreferredDiscoveryForSixHoursWithoutRepeatedModelCalls() = runTest {
        val api = CatalogApi(listOf(models("gpt-6-luna", "gpt-5.6-luna")))
        var now = 0L
        val catalog = OpenAiModelCatalog(api, nowMillis = { now })

        assertEquals("gpt-6-luna", catalog.select("synthetic-secret"))
        now += 15 * 60 * 1_000L
        assertEquals("gpt-6-luna", catalog.select("synthetic-secret"))
        assertEquals(1, api.listCalls)

        now += 6 * 60 * 60 * 1_000L
        assertEquals("gpt-6-luna", catalog.select("synthetic-secret"))
        assertEquals(2, api.listCalls)
    }

    @Test
    fun select_coalescesConcurrentDiscoveryForSameKey() = runTest {
        val releaseDiscovery = CompletableDeferred<Unit>()
        val api = CatalogApi(
            responses = listOf(models("gpt-6-luna")),
            beforeList = { releaseDiscovery.await() },
        )
        val catalog = OpenAiModelCatalog(api)

        val first = async { catalog.select("synthetic-secret") }
        val second = async { catalog.select("synthetic-secret") }
        yield()
        releaseDiscovery.complete(Unit)

        assertEquals("gpt-6-luna", first.await())
        assertEquals("gpt-6-luna", second.await())
        assertEquals(1, api.listCalls)
    }

    @Test
    fun select_backsOffFailedDiscoveryAndRetriesAfterShortWindow() = runTest {
        val api = CatalogApi(listOf(
            Response.error(503, "{\"error\":{\"code\":\"server_error\"}}".toResponseBody()),
            models("gpt-6-luna"),
        ))
        var now = 0L
        val catalog = OpenAiModelCatalog(api, nowMillis = { now })

        assertTrue(runCatching { catalog.select("synthetic-secret") }.exceptionOrNull() is OpenAiModelDiscoveryException)
        assertTrue(runCatching { catalog.select("synthetic-secret") }.exceptionOrNull() is OpenAiModelDiscoveryException)
        assertEquals(1, api.listCalls)
        now += 30_000L
        assertEquals("gpt-6-luna", catalog.select("synthetic-secret"))
        assertEquals(2, api.listCalls)
    }

    @Test
    fun select_excludesShutdownAndRejectedModelsAndFailsSafelyWhenNothingRemains() = runTest {
        val api = CatalogApi(listOf(models("gpt-5.6-luna", shutdownDate = "2000-01-01")))
        val catalog = OpenAiModelCatalog(api)

        assertTrue(runCatching { catalog.select("synthetic-secret") }.exceptionOrNull() is OpenAiNoUsableModelException)
        catalog.invalidate("synthetic-secret")
        assertTrue(
            runCatching { catalog.select("synthetic-secret", excluded = setOf("gpt-5.6-luna")) }
                .exceptionOrNull() is OpenAiNoUsableModelException,
        )
        assertEquals(2, api.listCalls)
    }

    private fun models(vararg ids: String, shutdownDate: String? = null): Response<OpenAiModelsResponse> =
        Response.success(OpenAiModelsResponse(ids.map { OpenAiModelDescriptor(id = it, shutdownDate = shutdownDate) }))

    private class CatalogApi(
        private val responses: List<Response<OpenAiModelsResponse>>,
        private val beforeList: suspend () -> Unit = {},
    ) : OpenAiApi {
        var listCalls = 0

        override suspend fun listModels(authorization: String): Response<OpenAiModelsResponse> {
            listCalls += 1
            beforeList()
            return responses[(listCalls - 1).coerceAtMost(responses.lastIndex)]
        }

        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> = error("Not used by model discovery")
    }
}
