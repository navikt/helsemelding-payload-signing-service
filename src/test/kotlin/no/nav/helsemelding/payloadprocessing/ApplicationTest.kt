package no.nav.helsemelding.payloadprocessing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

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
})
