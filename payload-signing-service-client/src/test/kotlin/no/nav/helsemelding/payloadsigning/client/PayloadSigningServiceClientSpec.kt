package no.nav.helsemelding.payloadsigning.client

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headers
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsemelding.payloadsigning.model.Direction
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse
import kotlin.text.toByteArray
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json as JsonUtil

@OptIn(ExperimentalTime::class)
class PayloadSigningServiceClientSpec : StringSpec(
    {

        val payloadBytes = "<MsgHead><Body>hello world</Body></MsgHead>".toByteArray()

        "signPayload returns PayloadResponse when payload is successfully signed" {
            val payloadRequest = PayloadRequest(
                bytes = payloadBytes,
                direction = Direction.OUT
            )
            val payloadResponse = PayloadResponse(bytes = payloadBytes)

            val client = payloadSigningServiceClient {
                fakeScopedAuthHttpClient { request ->
                    request.method shouldBe HttpMethod.Post
                    request.url.fullPath shouldBeEqual "/payload"

                    respond(
                        content = JsonUtil.encodeToString(payloadResponse),
                        headers = headersOf(ContentType, Json.toString()),
                        status = HttpStatusCode.OK
                    )
                }
            }

            val response = client.signPayload(payloadRequest)

            response.shouldNotBeNull()
            response.bytes shouldBe payloadResponse.bytes
        }
    }
)

private fun payloadSigningServiceClient(httpClient: () -> HttpClient) = PayloadSigningServiceClient(
    payloadSigningServiceUrl = "http://localhost",
    clientProvider = httpClient
)

private fun fakeScopedAuthHttpClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
) = HttpClient(MockEngine) {
    engine { addHandler(handler) }
    install(ContentNegotiation) {
        json(
            JsonUtil {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }
}
