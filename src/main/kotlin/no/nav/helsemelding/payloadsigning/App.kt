package no.nav.helsemelding.payloadsigning

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
import no.nav.helsemelding.payloadsigning.keystore.KeyStoreManager
import no.nav.helsemelding.payloadsigning.plugin.configureAuthentication
import no.nav.helsemelding.payloadsigning.plugin.configureMetrics
import no.nav.helsemelding.payloadsigning.plugin.configureRoutes
import no.nav.helsemelding.payloadsigning.plugin.installContentNegotiation
import no.nav.helsemelding.payloadsigning.service.ProcessingService
import no.nav.helsemelding.payloadsigning.service.SigningService

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
    val keyStoreManager = KeyStoreManager(*config().keyStore.toTypedArray())
    val signingService = SigningService(keyStoreManager)
    val processingService = ProcessingService(signingService)

    return {
        installContentNegotiation()
        configureAuthentication()
        configureMetrics(meterRegistry)
        configureRoutes(processingService, meterRegistry)
    }
}

private fun logError(t: Throwable) = log.error { "Shutdown Helsemelding payload signing service due to: ${t.stackTraceToString()}" }
