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
import no.nav.helsemelding.payloadsigning.client.model.MessageSigningError
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse

private val log = KotlinLogging.logger {}

interface PayloadSigningClient {
    suspend fun signPayload(payloadRequest: PayloadRequest): Either<MessageSigningError, PayloadResponse>

    fun close()
}

class HttpPayloadSigningClient(
    clientProvider: () -> HttpClient,
    private val payloadSigningServiceUrl: String = config().payloadSigningService.url.toString()
) : PayloadSigningClient {
    private var httpClient = clientProvider.invoke()

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

    override fun close() = httpClient.close()
}

suspend fun HttpResponse.withLogging(): HttpResponse {
    val body = this.bodyAsText()
    log.debug { "Response from ${request.method} ${request.url} is $status: $body" }
    return this
}
