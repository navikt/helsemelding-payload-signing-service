package no.nav.helsemelding.payloadsigning.client

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helsemelding.payloadsigning.model.MessageSigningError
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse

private val log = KotlinLogging.logger {}

/**
 * Client for signing and verifying EDI message payloads via the Payload Signing Service.
 *
 * All operations return [Either] where [Either.Left] contains a [MessageSigningError]
 * on failure, and [Either.Right] contains the successful result.
 *
 */
interface PayloadSigningClient {
    /**
     * Signs or verifies a message payload.
     *
     * @param payloadRequest the payload bytes and direction (required)
     * @return a [PayloadResponse] containing the processed bytes, or a [MessageSigningError] on failure
     */
    suspend fun signPayload(payloadRequest: PayloadRequest): Either<MessageSigningError, PayloadResponse>

    /**
     * Closes the underlying HTTP client and releases resources.
     *
     * Should be called when the client is no longer needed to free connections.
     */
    fun close()
}

/**
 * HTTP-based implementation of [PayloadSigningClient].
 *
 * Communicates with the Payload Signing Service over HTTP using the provided [HttpClient].
 * Use [scopedAuthHttpClient] to create a pre-configured client with Azure AD bearer token support.
 *
 * @param clientProvider factory function that creates the underlying [HttpClient]
 * @param payloadSigningServiceUrl base URL of the Payload Signing Service
 */
class HttpPayloadSigningClient(
    clientProvider: () -> HttpClient,
    private val payloadSigningServiceUrl: String = config().payloadSigningService.url.toString()
) : PayloadSigningClient {
    private var httpClient = clientProvider.invoke()

    /**
     * Signs or verifies a message payload.
     *
     * @param payloadRequest the payload bytes and direction (required)
     * @return a [PayloadResponse] containing the processed bytes, or a [MessageSigningError] on failure
     */
    override suspend fun signPayload(payloadRequest: PayloadRequest): Either<MessageSigningError, PayloadResponse> {
        val url = "$payloadSigningServiceUrl/payload"

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payloadRequest)
        }.withLogging()

        if (response.status != HttpStatusCode.OK) {
            val messageSigningError = MessageSigningError(
                code = response.status.value,
                message = response.bodyAsText()
            )
            return Either.Left(messageSigningError)
        }

        return Either.Right(response.body())
    }

    /**
     * Closes the underlying HTTP client and releases resources.
     *
     * Should be called when the client is no longer needed to free connections.
     */
    override fun close() = httpClient.close()
}

suspend fun HttpResponse.withLogging(): HttpResponse {
    log.debug { "Response from ${request.method} ${request.url} is $status" }
    return this
}
