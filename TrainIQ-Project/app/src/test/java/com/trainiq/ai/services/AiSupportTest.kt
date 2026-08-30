package com.trainiq.ai.services

import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AiSupportTest {
    @Test
    fun toAiUserMessage_mapsConfigurationPermissionNetworkAndServiceFailuresWithoutResponseBodies() {
        val responseBody = "secret-bearing response body".toResponseBody()

        assertEquals(
            "De API-sleutel is ongeldig of niet geautoriseerd. Controleer de AI-instellingen.",
            HttpException(Response.error<Unit>(401, responseBody)).toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider heeft geen toestemming voor dit verzoek. Controleer de providerrechten.",
            HttpException(Response.error<Unit>(403, responseBody)).toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider kan niet worden bereikt. Controleer je internetverbinding en probeer opnieuw.",
            IOException("network diagnostic").toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider is tijdelijk niet beschikbaar. Probeer later opnieuw.",
            HttpException(Response.error<Unit>(500, responseBody)).toAiUserMessage("fallback"),
        )
    }
}
