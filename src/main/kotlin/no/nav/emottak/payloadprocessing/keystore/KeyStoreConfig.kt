package no.nav.emottak.payloadprocessing.keystore

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.io.readBytes
import kotlin.jvm.java

private val log = KotlinLogging.logger {}

data class KeyStoreConfig(
    val keyStoreFilePath: String,
    val keyStorePass: String,
    val keyStoreType: String
) {
    val keyStoreFile: InputStream = getKeyStoreFile(keyStoreFilePath)

    private fun getKeyStoreFile(path: String): InputStream {
        return try {
            log.debug { "Getting store file from $path" }
            if (File(path).exists()) {
                log.info { "Getting store file from file <$path>" }
                FileInputStream(path)
            } else {
                log.info { "Getting store file from resources <$path>" }
                ByteArrayInputStream(this::class.java.classLoader.getResourceAsStream(path).readBytes())
            }
        } catch (e: Exception) {
            log.error(e) { "${"Failed to load keystore $path"}" }
            throw kotlin.RuntimeException("Failed to load keystore $path", e)
        }
    }
}
