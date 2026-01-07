package no.nav.helsemelding.payloadprocessing.keystore

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate

private val log = KotlinLogging.logger {}

sealed interface KeyStoreResolverError {
    data class Input(val config: KeyStoreConfig, val error: KeyStoreError) : KeyStoreResolverError
    data class LoadFailed(val config: KeyStoreConfig, val cause: Throwable) : KeyStoreResolverError
}

class KeyStoreManager(private vararg val keyStoreConfig: KeyStoreConfig) {
    private val keyStores: List<Pair<KeyStore, KeyStoreConfig>>

    init {
        Security.addProvider(BouncyCastleProvider())
        keyStores = keyStoreResolver()
    }

    fun getPrivateKey(serialNumber: BigInteger): PrivateKey? =
        keyStores.asSequence()
            .mapNotNull { (store, config) ->
                store.aliases()
                    .asSequence()
                    .firstOrNull { alias ->
                        (store.getCertificate(alias) as? X509Certificate)
                            ?.serialNumber == serialNumber
                    }
                    ?.let { alias ->
                        store.getKey(alias, config.keyStorePass.toCharArray()) as? PrivateKey
                    }
            }
            .firstOrNull()

    fun getCertificate(alias: String): X509Certificate =
        keyStores
            .firstNotNullOf { (store) -> store.getCertificate(alias) as X509Certificate }

    private fun keyStoreResolver(): List<Pair<KeyStore, KeyStoreConfig>> =
        either {
            keyStoreConfig.map { config -> loadKeyStore(config).bind() to config }
        }
            .getOrElse { e -> error("Keystore initialization failed: $e") }

    private fun loadKeyStore(config: KeyStoreConfig): Either<KeyStoreResolverError, KeyStore> =
        config.openKeyStoreInputStream()
            .mapLeft { KeyStoreResolverError.Input(config, it) }
            .flatMap { input ->
                val keyStore = KeyStore.getInstance(config.keyStoreType)
                Either.catch {
                    input.use { keyStore.load(it, config.keyStorePass.toCharArray()) }
                    log.debug { "Keystore loaded successfully: ${config.keyStoreFilePath}" }
                    keyStore
                }
                    .mapLeft { t -> KeyStoreResolverError.LoadFailed(config, t) }
            }
            .onLeft { err ->
                log.error { "Failed to load keystore: ${config.keyStoreFilePath}. Reason: $err" }
            }
}
