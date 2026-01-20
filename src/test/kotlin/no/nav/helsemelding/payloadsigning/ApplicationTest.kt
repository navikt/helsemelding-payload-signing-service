package no.nav.helsemelding.payloadsigning

import com.nimbusds.jwt.SignedJWT
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.helsemelding.payloadsigning.auth.AuthConfig
import no.nav.helsemelding.payloadsigning.model.Direction
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse
import no.nav.helsemelding.payloadsigning.plugin.externalRoutes
import no.nav.helsemelding.payloadsigning.service.ProcessingService
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v3.tokenValidationSupport
import org.junit.jupiter.api.Assertions
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ApplicationTest : StringSpec({

    lateinit var mockOAuth2Server: MockOAuth2Server

    val validAudience = config().azureAuth.appScope.value
    val invalidAudience = "api://dev-fss.team-emottak.some-other-service/.default"

    val getToken: (String) -> SignedJWT = { audience: String ->
        mockOAuth2Server.issueToken(
            issuerId = config().azureAuth.issuer.value,
            audience = audience,
            subject = "testUser"
        )
    }

    beforeSpec {
        mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }
    }

    "Root endpoint should return OK" {
        val mockProcessingService = mockk<ProcessingService>()
        withExternalRoutes(mockProcessingService) { client ->

            client.get("/").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "Root endpoint should return OK with authentication" {
        val mockProcessingService = mockk<ProcessingService>()

        withExternalRoutes(mockProcessingService) { client ->
            client.get("/") {
                header(
                    "Authorization",
                    "Bearer ${getToken(validAudience).serialize()}"
                )
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }

    "Root endpoint should return Unauthorised if access token is missing" {
        val mockProcessingService = mockk<ProcessingService>()

        withExternalRoutes(mockProcessingService, useAuthentication = true) { client ->
            client.get("/").apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    "Root endpoint should return Unauthorised if access token is invalid" {
        val mockProcessingService = mockk<ProcessingService>()

        withExternalRoutes(mockProcessingService, useAuthentication = true) { client ->
            client.get("/") {
                header(
                    "Authorization",
                    "Bearer ${getToken(invalidAudience).serialize()}"
                )
            }.apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    "POST /payload should process incoming messages" {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        withTestApplication(meterRegistry) { client ->
            // Prepare unsigned XML message
            val unsignedMessageBytes = this::class.java.classLoader.getResourceAsStream("test.xml")!!.readBytes()
            val unsignedDocument = createDocument(unsignedMessageBytes.inputStream())
            Assertions.assertEquals(0, unsignedDocument.getElementsByTagName("Signature").length)

            // Send POST request to /payload
            val request = PayloadRequest(
                direction = Direction.OUT,
                bytes = unsignedMessageBytes
            )
            val response = client.post("/payload") {
                contentType(Json)
                setBody(request)
                header("Authorization", "Bearer ${getToken(validAudience).serialize()}")
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

fun withExternalRoutes(
    processingService: ProcessingService,
    useAuthentication: Boolean = false,
    block: suspend (HttpClient) -> Unit
) {
    testApplication {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
        if (useAuthentication) {
            install(Authentication) {
                tokenValidationSupport(config().azureAuth.issuer.value, AuthConfig.getTokenSupportConfig())
            }
        }
        routing {
            if (useAuthentication) {
                authenticate(config().azureAuth.issuer.value) {
                    externalRoutes(processingService)
                }
            } else {
                externalRoutes(processingService)
            }
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        block(client)
    }
}

fun withTestApplication(
    meterRegistry: PrometheusMeterRegistry,
    block: suspend (HttpClient) -> Unit
) {
    testApplication {
        environment {
            config = MapApplicationConfig(
                "azureAuth.issuer" to config().azureAuth.issuer.value,
                "azureAuth.azureWellKnownUrl" to config().azureAuth.azureWellKnownUrl.value,
                "azureAuth.acceptedAudience" to config().azureAuth.acceptedAudience.value,
                "azureAuth.appScope" to config().azureAuth.appScope.value
            )
        }
        application(payloadProcessingModule(meterRegistry))
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        block(client)
    }
}
