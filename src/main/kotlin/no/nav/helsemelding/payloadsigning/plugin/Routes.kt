package no.nav.helsemelding.payloadsigning.plugin

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.payloadsigning.config
import no.nav.helsemelding.payloadsigning.model.Direction
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse
import no.nav.helsemelding.payloadsigning.service.ProcessingError
import no.nav.helsemelding.payloadsigning.service.ProcessingService
import no.nav.helsemelding.payloadsigning.service.SignXmlError
import kotlin.random.Random

private val log = KotlinLogging.logger {}

data class ErrorResponse(val error: String)

fun Application.configureRoutes(
    processingService: ProcessingService,
    registry: PrometheusMeterRegistry
) {
    routing {
        internalRoutes(registry)

        authenticate(config().azureAuth.issuer.value) {
            externalRoutes(processingService)
        }
    }
}

fun Route.internalRoutes(
    registry: PrometheusMeterRegistry
) {
    get("/prometheus") {
        call.respond(registry.scrape())
    }
    route("/internal") {
        get("/health/liveness") {
            call.respondText("I'm alive! :)")
        }
        get("/health/readiness") {
            call.respondText("I'm ready! :)")
        }
    }
}

fun Route.externalRoutes(processingService: ProcessingService) {
    get("/") {
        call.respondText("Payload Processing Service is online")
    }

    postPayload(processingService)
}

fun Route.postPayload(processingService: ProcessingService) = post("/payload") {
    val request = call.receive<PayloadRequest>()
    log.debug { "PayloadRequest request received" }

    // Simulate error with given probability
    if (Random.nextDouble() < 0.2) {
        log.warn { "Simulated error triggered for /payload endpoint" }
        val simulatedError = ProcessingError.SigningFailed(
            SignXmlError.SignatureFailed(
                "Simulated error",
                Throwable("Simulated")
            )
        )
        val (status, body) = simulatedError.toHttpResponse()
        call.respond(status, body)
        return@post
    }

    val result: Either<ProcessingError, PayloadResponse> = when (request.direction) {
        Direction.IN -> processingService.processIncoming(request)
        Direction.OUT -> processingService.processOutgoing(request)
    }

    val (status, body) =
        result
            .onLeft { log.error { "POSTing payload failed: $it" } }
            .map { HttpStatusCode.OK to it }
            .mapLeft(ProcessingError::toHttpResponse)
            .getOrElse { it }

    call.respond(status, body)
}

private fun ProcessingError.toHttpResponse(): Pair<HttpStatusCode, ErrorResponse> =
    when (this) {
        is ProcessingError.InvalidRequest ->
            HttpStatusCode.BadRequest to ErrorResponse(message)

        is ProcessingError.XmlParseFailed ->
            HttpStatusCode.BadRequest to ErrorResponse("Invalid XML payload")

        is ProcessingError.SigningFailed ->
            HttpStatusCode.UnprocessableEntity to ErrorResponse("Unable to sign payload")
    }
