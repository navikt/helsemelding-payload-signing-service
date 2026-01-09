package no.nav.helsemelding.payloadprocessing.keystore

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

private val log = KotlinLogging.logger {}

data class KeyStoreConfig(
    val keyStoreFilePath: String,
    val keyStorePass: String,
    val keyStoreType: String
) {
    fun openKeyStoreInputStream(): InputStream {
        return try {
            log.debug { "Getting store file from $keyStoreFilePath" }
            if (File(keyStoreFilePath).exists()) {
                log.info { "Getting store file from file <$keyStoreFilePath>" }
                FileInputStream(keyStoreFilePath)
            } else {
                log.info { "Getting store file from resources <$keyStoreFilePath>" }
                val resourceBytes = this::class.java.classLoader.getResourceAsStream(keyStoreFilePath)?.readBytes()
                    ?: throw RuntimeException("Resource not found: $keyStoreFilePath")
                ByteArrayInputStream(resourceBytes)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to load keystore $keyStoreFilePath" }
            throw RuntimeException("Failed to load keystore $keyStoreFilePath", e)
        }
    }
}
