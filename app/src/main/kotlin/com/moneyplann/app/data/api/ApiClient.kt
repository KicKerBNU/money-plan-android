package com.moneyplann.app.data.api

import com.moneyplann.app.BuildConfig
import com.moneyplann.app.data.auth.AuthRepository
import com.moneyplann.app.data.models.ApiErrorBody
import com.moneyplann.app.data.models.EmptyResponse
import com.moneyplann.app.util.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient(private val authRepository: AuthRepository) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun <T> fetch(
        path: String,
        method: String = "GET",
        body: String? = null,
        silentError: Boolean = false,
        silentSuccess: Boolean = true,
        successMessage: String? = null,
        decode: (String) -> T,
    ): T = withContext(Dispatchers.IO) {
        val isMutation = method.uppercase() in setOf("POST", "PUT", "PATCH", "DELETE")
        val user = authRepository.activeUser() ?: throw ApiClientException.NotAuthenticated

        suspend fun perform(forceRefresh: Boolean): Pair<String, Int> {
            val token = authRepository.idToken(forceRefresh)
            val normalized = if (path.startsWith("/")) path else "/$path"
            val requestBuilder = Request.Builder()
                .url("$baseUrl$normalized")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")

            when (method.uppercase()) {
                "GET" -> requestBuilder.get()
                "DELETE" -> requestBuilder.delete()
                else -> requestBuilder.method(method.uppercase(), (body ?: "{}").toRequestBody(JSON_MEDIA))
            }

            return try {
                http.newCall(requestBuilder.build()).execute().use { response ->
                    response.body?.string().orEmpty() to response.code
                }
            } catch (e: Exception) {
                ApiDebug.logFailure(path, e)
                if (!silentError) ToastManager.error(e.message ?: "Network error")
                throw ApiClientException.Network(e)
            }
        }

        var (bodyText, status) = perform(forceRefresh = false)
        if (status == 401) {
            val retry = perform(forceRefresh = true)
            bodyText = retry.first
            status = retry.second
        }

        if (status == 401) {
            val message = runCatching { json.decodeFromString<ApiErrorBody>(bodyText).message }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "Unauthorized"
            ApiDebug.logHttpFailure(path, status, message)
            if (!silentError) ToastManager.error(message)
            throw ApiClientException.Unauthorized
        }

        if (status !in 200..299) {
            val message = runCatching { json.decodeFromString<ApiErrorBody>(bodyText).message }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "Request failed with status $status"
            ApiDebug.logHttpFailure(path, status, message)
            if (!silentError) ToastManager.error(message)
            throw ApiClientException.Server(status, message)
        }

        if (status == 204) {
            if (isMutation && !silentSuccess) {
                ToastManager.success(successMessage ?: "Saved successfully")
            }
            @Suppress("UNCHECKED_CAST")
            return@withContext EmptyResponse() as T
        }

        if (bodyText.isBlank()) {
            throw ApiClientException.Decoding(IllegalStateException("Empty response body for $path"))
        }

        try {
            val decoded = decode(bodyText)
            ApiDebug.logSuccess(path, method)
            if (isMutation && !silentSuccess) {
                ToastManager.success(successMessage ?: "Saved successfully")
            }
            decoded
        } catch (e: Exception) {
            ApiDebug.logDecodeFailure(path, bodyText, e)
            if (!silentError) ToastManager.error("Unexpected error")
            throw ApiClientException.Decoding(e)
        }
    }

    suspend inline fun <reified T> fetchJson(
        path: String,
        method: String = "GET",
        body: String? = null,
        silentError: Boolean = false,
        silentSuccess: Boolean = true,
        successMessage: String? = null,
    ): T = fetch(
        path = path,
        method = method,
        body = body,
        silentError = silentError,
        silentSuccess = silentSuccess,
        successMessage = successMessage,
        decode = { json.decodeFromString<T>(it) },
    )

    suspend fun fetchVoid(
        path: String,
        method: String = "DELETE",
        body: String? = null,
        silentError: Boolean = false,
        silentSuccess: Boolean = true,
        successMessage: String? = null,
    ) {
        fetch<EmptyResponse>(
            path = path,
            method = method,
            body = body,
            silentError = silentError,
            silentSuccess = silentSuccess,
            successMessage = successMessage,
            decode = { EmptyResponse() },
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}

sealed class ApiClientException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    data object NotAuthenticated : ApiClientException("Not authenticated")
    data object Unauthorized : ApiClientException("Unauthorized")
    data class Server(val status: Int, override val message: String) : ApiClientException(message)
    data class Network(override val cause: Throwable) : ApiClientException(cause.message, cause)
    data class Decoding(override val cause: Throwable) : ApiClientException(cause.message, cause)
}
