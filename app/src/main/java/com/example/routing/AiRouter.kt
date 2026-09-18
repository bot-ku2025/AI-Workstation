package com.example.routing

import com.example.security.CredentialVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Deterministic routing policy. Key rotation, provider fallback and router fallback are separate.
 * This class never reports SUCCESS for a request that did not receive a valid HTTP response.
 */
class AiRouter(private val vault: CredentialVault) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generate(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        val routes = request.routes.filter { it.enabled }
        if (routes.isEmpty()) {
            return@withContext AiResult.failure("NO_ROUTE", "Tidak ada route AI yang aktif.")
        }

        var last = AiResult.failure("NO_ATTEMPT", "Belum ada route dicoba.")
        for (route in routes) {
            val candidates = if (route.credentialIds.isEmpty()) {
                listOf<String?>(null)
            } else {
                route.credentialIds.map { it }
            }

            for (credentialId in candidates) {
                val result = callRoute(route, credentialId, request)
                if (result.success) return@withContext result
                last = result
                if (result.code !in setOf("RATE_LIMIT", "QUOTA", "NETWORK", "AUTH")) break
            }
        }
        last
    }

    private fun callRoute(route: AiRoute, credentialId: String?, request: AiRequest): AiResult {
        val secret = credentialId?.let(vault::getSecret)
        if (credentialId != null && secret == null) {
            return AiResult.failure("AUTH", "Credential $credentialId tidak tersedia di vault.")
        }

        val url = when (route.type) {
            RouteType.GEMINI -> {
                if (secret.isNullOrBlank()) {
                    return AiResult.failure("AUTH", "Gemini membutuhkan credential API yang aktif.")
                }
                route.endpoint.trimEnd('/') + "/v1beta/models/${route.model}:generateContent?key=$secret"
            }
            RouteType.OPENAI_COMPATIBLE, RouteType.ROUTER -> {
                route.endpoint.trimEnd('/') + "/chat/completions"
            }
        }

        val body = when (route.type) {
            RouteType.GEMINI -> {
                val part = JSONObject().put("text", request.prompt)
                val parts = JSONArray().put(part)
                val content = JSONObject().put("role", "user").put("parts", parts)
                JSONObject().put("contents", JSONArray().put(content))
            }
            RouteType.OPENAI_COMPATIBLE, RouteType.ROUTER -> {
                val message = JSONObject()
                    .put("role", "user")
                    .put("content", request.prompt)
                JSONObject()
                    .put("model", route.model)
                    .put("messages", JSONArray().put(message))
            }
        }

        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))

        if (route.type != RouteType.GEMINI && !secret.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $secret")
        }
        route.headers.forEach { (key, value) -> builder.header(key, value) }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    AiResult(true, "OK", text)
                } else {
                    val code = when (response.code) {
                        401, 403 -> "AUTH"
                        429 -> "RATE_LIMIT"
                        402 -> "QUOTA"
                        else -> "PROVIDER"
                    }
                    AiResult.failure(code, "HTTP ${response.code}: ${text.take(500)}")
                }
            }
        } catch (t: Throwable) {
            AiResult.failure("NETWORK", t.message ?: "Network error")
        }
    }
}

data class AiRequest(val prompt: String, val routes: List<AiRoute>)

data class AiRoute(
    val id: String,
    val name: String,
    val type: RouteType,
    val endpoint: String,
    val model: String,
    val credentialIds: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

enum class RouteType { GEMINI, OPENAI_COMPATIBLE, ROUTER }

data class AiResult(val success: Boolean, val code: String, val output: String) {
    companion object {
        fun failure(code: String, message: String) = AiResult(false, code, message)
    }
}
