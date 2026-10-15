package com.example.kisanai

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApiService {
    @Headers("anthropic-version: 2023-06-01", "Content-Type: application/json")
    @POST("v1/messages")
    fun getCompletion(
        @Header("x-api-key") apiKey: String,
        @Body request: ClaudeRequest
    ): Call<ClaudeResponse>
}

data class ClaudeRequest(
    val model: String = "claude-haiku-4-5-20251001",
    val max_tokens: Int = 1024,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val content: List<Content>
)

data class Content(
    val text: String
)
