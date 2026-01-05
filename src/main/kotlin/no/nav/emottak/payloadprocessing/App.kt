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
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.awaitCancellation
import no.nav.emottak.payloadprocessing.keystore.KeyStoreManager
import no.nav.emottak.payloadprocessing.plugin.configureMetrics
import no.nav.emottak.payloadprocessing.plugin.configureRoutes
import no.nav.emottak.payloadprocessing.plugin.installContentNegotiation
import no.nav.emottak.payloadprocessing.service.ProcessingService
import no.nav.emottak.payloadprocessing.service.SigningService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

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
    logDirectoryContents()
    logFileContent("/var/run/secrets/dialog-keystore/nav_signing_test.p12")

    return {
        installContentNegotiation()
        configureMetrics(meterRegistry)
        configureRoutes(processingService, meterRegistry)
    }
}

private fun logError(t: Throwable) = log.error { "Shutdown payload-processing-service due to: ${t.stackTraceToString()}" }

fun logDirectoryContents() {
    val dir = "/var/run/secrets/dialog-keystore"
    val entries = runCatching { listDirectory(Path.of(dir)) }.getOrElse { e ->
        log.warn { "Could not list directory $dir: $e" }
        return
    }
    log.info { "Directory listing for $dir : $entries" }
}

fun listDirectory(dir: Path): List<String> {
    if (!dir.exists()) {
        throw IllegalArgumentException("Directory does not exist: $dir")
    }
    if (!dir.isDirectory()) {
        throw IllegalArgumentException("Path is not a directory: $dir")
    }

    Files.newDirectoryStream(dir).use { stream ->
        return stream.map { p ->
            p.fileName.toString()
        }
    }
}

fun logFileContent(
    path: String,
    maxBytes: Long = 64 * 1024
) {
    val file = Path.of(path)

    if (!file.exists()) {
        log.warn { "File does not exist: $path" }
        return
    }
    if (!file.isRegularFile()) {
        log.warn { "Path is not a regular file: $path" }
        return
    }
    if (!file.isReadable()) {
        log.warn { "File is not readable: $path" }
        return
    }

    val size = Files.size(file)
    if (size > maxBytes) {
        log.warn { "File $path is too large to log ($size bytes, limit $maxBytes bytes)" }
        return
    }

    val content = Files.readString(file, StandardCharsets.UTF_8)
    log.info { "Content of file $path:\n{}$content" }
}
