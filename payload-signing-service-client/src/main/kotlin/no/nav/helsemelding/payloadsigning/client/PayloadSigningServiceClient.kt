package no.nav.helsemelding.payloadsigning.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse

private val log = KotlinLogging.logger {}

class PayloadSigningServiceClient(
    clientProvider: () -> HttpClient,
    private val ediAdapterUrl: String = config().payloadSigningService.url.toString()
) {
    private var httpClient = clientProvider.invoke()

    suspend fun signPayload(payloadRequest: PayloadRequest): PayloadResponse {
        val url = "$ediAdapterUrl/payload"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payloadRequest)
        }.withLogging()

        return response.body()
    }

    fun close() = httpClient.close()
}

suspend fun HttpResponse.withLogging(): HttpResponse {
    val body = this.bodyAsText()
    log.debug { "Response from ${request.method} ${request.url} is $status: $body" }
    return this
}
