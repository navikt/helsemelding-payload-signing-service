package no.nav.helsemelding.payloadprocessing.plugin

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.payloadprocessing.model.Direction
import no.nav.helsemelding.payloadprocessing.model.PayloadRequest
import no.nav.helsemelding.payloadprocessing.model.PayloadResponse
import no.nav.helsemelding.payloadprocessing.service.ProcessingError
import no.nav.helsemelding.payloadprocessing.service.ProcessingService

private val log = KotlinLogging.logger {}

data class ErrorResponse(val error: String)

fun Application.configureRoutes(
    processingService: ProcessingService,
    registry: PrometheusMeterRegistry
) {
    routing {
        internalRoutes(processingService, registry)
        externalRoutes()
    }
}

fun Route.internalRoutes(
    processingService: ProcessingService,
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
    postPayload(processingService)
}

fun Route.externalRoutes() {
    get("/") {
        call.respondText("Payload Processing Service is online")
    }
}

fun Route.postPayload(processingService: ProcessingService) = post("/payload") {
    val request = call.receive<PayloadRequest>()

    val result: Either<ProcessingError, PayloadResponse> = when (request.direction) {
        Direction.IN -> processingService.processIncoming(request)
        Direction.OUT -> processingService.processOutgoing(request)
    }

    val (status, body) =
        result
            .map { HttpStatusCode.OK to it }
            .mapLeft(ProcessingError::toHttpResponse)
            .getOrElse { it }

    call.respond(status, body)
}

private fun ProcessingError.toHttpResponse(): Pair<HttpStatusCode, ErrorResponse> {
    log.error { "POST /payload failed: $this" }

    return when (this) {
        is ProcessingError.InvalidRequest ->
            HttpStatusCode.BadRequest to ErrorResponse(message)

        is ProcessingError.XmlParseFailed ->
            HttpStatusCode.BadRequest to ErrorResponse("Invalid XML payload")

        is ProcessingError.SigningFailed ->
            HttpStatusCode.UnprocessableEntity to ErrorResponse("Unable to sign payload")
    }
}
