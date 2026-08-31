package com.trainiq.data.remote

import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST

interface OpenAiApi {
    @GET("v1/models")
    suspend fun listModels(
        @Header("Authorization") authorization: String,
    ): Response<com.trainiq.data.model.OpenAiModelsResponse>

    @POST("v1/responses")
    suspend fun createResponse(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiResponseRequest,
    ): Response<OpenAiResponse>
}
