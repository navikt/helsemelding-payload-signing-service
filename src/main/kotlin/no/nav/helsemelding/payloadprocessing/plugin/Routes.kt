package no.nav.helsemelding.payloadprocessing.plugin

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
import no.nav.emottak.payloadprocessing.model.Direction
import no.nav.emottak.payloadprocessing.model.PayloadRequest
import no.nav.emottak.payloadprocessing.service.ProcessingService

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
        call.respondText("Payload Processing Service os online")
    }
}

fun Route.postPayload(
    processingService: ProcessingService
) = post("/payload") {
    val request: PayloadRequest = call.receive(PayloadRequest::class)

    val response = when (request.direction) {
        Direction.IN -> processingService.processIncoming(request)
        Direction.OUT -> processingService.processOutgoing(request)
    }

    call.respond(response)
}
