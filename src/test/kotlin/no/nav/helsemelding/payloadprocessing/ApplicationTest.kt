package no.nav.helsemelding.payloadprocessing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.payloadprocessing.model.Direction
import no.nav.helsemelding.payloadprocessing.model.PayloadRequest
import no.nav.helsemelding.payloadprocessing.model.PayloadResponse
import org.junit.jupiter.api.Assertions
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ApplicationTest : StringSpec({

    val withTestApplication = fun (testBlock: suspend (HttpClient) -> Unit) {
        testApplication {
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            application(
                payloadProcessingModule(meterRegistry)
            )

            val httpClient = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            testBlock(httpClient)
        }
    }

    "Root endpoint should return OK" {
        withTestApplication { httpClient ->
            httpClient.get("/").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "POST /payload should process incoming messages" {
        withTestApplication { httpClient ->
            // Prepare unsigned XML message
            val unsignedMessageBytes = this::class.java.classLoader.getResourceAsStream("test.xml")!!.readBytes()
            val unsignedDocument = createDocument(unsignedMessageBytes.inputStream())
            Assertions.assertEquals(0, unsignedDocument.getElementsByTagName("Signature").length)

            // Send POST request to /payload
            val request = PayloadRequest(
                direction = Direction.OUT,
                bytes = unsignedMessageBytes
            )
            val response = httpClient.post("/payload") {
                contentType(Json)
                setBody(request)
            }
            response.status shouldBe HttpStatusCode.OK

            // Verify that message is signed now
            val payloadResponse = response.body<PayloadResponse>()
            val signedDocument = createDocument(payloadResponse.bytes.inputStream())
            Assertions.assertEquals(1, signedDocument.getElementsByTagName("Signature").length)
        }
    }
}) {
    companion object {
        fun createDocument(inputStream: InputStream): Document {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            documentBuilderFactory.isNamespaceAware = true
            return documentBuilderFactory.newDocumentBuilder().parse(inputStream)
        }
    }
}
