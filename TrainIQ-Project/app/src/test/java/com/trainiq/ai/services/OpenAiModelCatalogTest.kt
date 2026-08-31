package com.trainiq.ai.services

import com.trainiq.data.model.OpenAiModelDescriptor
import com.trainiq.data.model.OpenAiModelsResponse
import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.remote.OpenAiApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OpenAiModelCatalogTest {
    @Test
    fun select_prefersTheFirstAvailableBudgetCandidateAndCachesTheDiscovery() = runTest {
        val api = CatalogApi(
            listOf(
                models("gpt-5.4-mini", "gpt-5.6-luna"),
                models("gpt-5.4-mini"),
            ),
        )
        var now = 0L
        val catalog = OpenAiModelCatalog(api, nowMillis = { now })

        assertEquals("gpt-5.6-luna", catalog.select("synthetic-secret"))
        assertEquals("gpt-5.6-luna", catalog.select("synthetic-secret"))
        assertEquals(1, api.listCalls)

        now += 6 * 60 * 60 * 1_000L
        assertEquals("gpt-5.4-mini", catalog.select("synthetic-secret"))
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
    ) : OpenAiApi {
        var listCalls = 0

        override suspend fun listModels(authorization: String): Response<OpenAiModelsResponse> =
            responses[(listCalls++).coerceAtMost(responses.lastIndex)]

        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> = error("Not used by model discovery")
    }
}
