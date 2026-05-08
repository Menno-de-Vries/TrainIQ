package com.trainiq.data.remote

import com.trainiq.core.di.AppModule
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiNetworkPolicyTest {
    @Test
    fun geminiHttpClientHasExplicitBoundedCallTimeouts() {
        val client = AppModule.provideOkHttpClient()

        assertTrue(client.callTimeoutMillis in 1..TimeUnit.SECONDS.toMillis(30).toInt())
        assertTrue(client.connectTimeoutMillis in 1..TimeUnit.SECONDS.toMillis(15).toInt())
        assertTrue(client.readTimeoutMillis in 1..TimeUnit.SECONDS.toMillis(30).toInt())
        assertTrue(client.writeTimeoutMillis in 1..TimeUnit.SECONDS.toMillis(30).toInt())
    }
}
