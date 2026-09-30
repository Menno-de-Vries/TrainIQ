package com.trainiq.data.remote

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class MealProductTransportTest {
    @Test fun totalDeadlineIncludesResponseConsumption() = runBlocking {
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            try {
                exchange.sendResponseHeaders(200, 100)
                exchange.responseBody.write('x'.code)
                exchange.responseBody.flush()
                release.await(3, TimeUnit.SECONDS)
            } finally { exchange.close() }
        }
        server.start()
        try {
            val started = System.nanoTime()
            val failure = runCatching { MealProductTransport(OkHttpClient(), 150).get("http://127.0.0.1:${server.address.port}/") }.exceptionOrNull()
            assertTrue(failure is java.io.IOException)
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2_000)
        } finally { release.countDown(); server.stop(0) }
    }

    @Test fun cancellationClosesInFlightCallWithoutWaitingForBody() = runBlocking {
        val headers = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            try {
                exchange.sendResponseHeaders(200, 100)
                exchange.responseBody.write('x'.code)
                exchange.responseBody.flush()
                headers.countDown()
                release.await(3, TimeUnit.SECONDS)
            } finally { exchange.close() }
        }
        server.start()
        try {
            val job = launch(Dispatchers.Default) { MealProductTransport(OkHttpClient(), 2_000).get("http://127.0.0.1:${server.address.port}/") }
            assertTrue(headers.await(2, TimeUnit.SECONDS))
            val started = System.nanoTime()
            job.cancel()
            job.join()
            assertTrue(job.isCancelled)
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1_000)
        } finally { release.countDown(); server.stop(0) }
    }
}
