package no.nav.helsemelding.payloadprocessing.keystore

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

private val log = KotlinLogging.logger {}

sealed interface KeyStoreError {
    data class ResourceNotFound(val path: String) : KeyStoreError
    data class FileOpenFailed(val path: String, val cause: Throwable) : KeyStoreError
}

data class KeyStoreConfig(
    val keyStoreFilePath: String,
    val keyStorePass: String,
    val keyStoreType: String
) {
    fun openKeyStoreInputStream(): Either<KeyStoreError, InputStream> =
        run {
            log.debug { "Getting store file from $keyStoreFilePath" }
            openFromFile(keyStoreFilePath) ?: openFromClasspath(keyStoreFilePath)
        }
            .also { result ->
                result.onLeft { error ->
                    log.error { "Failed to load keystore $keyStoreFilePath: $error" }
                }
            }

    private fun openFromFile(path: String): Either<KeyStoreError, InputStream>? =
        File(path)
            .takeIf { it.exists() }
            ?.also { log.info { "Getting store file from file <$path>" } }
            ?.let { file ->
                Either.catch { FileInputStream(file) }
                    .mapLeft { KeyStoreError.FileOpenFailed(path, it) }
            }

    private fun openFromClasspath(path: String): Either<KeyStoreError, InputStream> =
        run {
            log.info { "Getting store file from resources <$path>" }
            readResourceBytes(path).map { bytes -> ByteArrayInputStream(bytes) }
        }

    private fun readResourceBytes(path: String): Either<KeyStoreError, ByteArray> =
        this::class.java.classLoader
            .getResourceAsStream(path)
            ?.use { it.readBytes() }
            ?.right()
            ?: KeyStoreError.ResourceNotFound(path).left()
}
