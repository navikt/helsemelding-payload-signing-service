package no.nav.emottak.payloadprocessing

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.resourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import no.nav.emottak.payloadprocessing.plugin.configureMetrics
import no.nav.emottak.payloadprocessing.plugin.configureRoutes
import no.nav.emottak.payloadprocessing.service.ProcessingService

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = payloadProcessingModule(deps.meterRegistry)
            )

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun payloadProcessingModule(
    meterRegistry: PrometheusMeterRegistry
): Application.() -> Unit {
    val processingService = ProcessingService()

    return {
        configureMetrics(meterRegistry)
        configureRoutes(processingService, meterRegistry)
    }
}

private fun logError(t: Throwable) = log.error { "Shutdown payload-processing-service due to: ${t.stackTraceToString()}" }
