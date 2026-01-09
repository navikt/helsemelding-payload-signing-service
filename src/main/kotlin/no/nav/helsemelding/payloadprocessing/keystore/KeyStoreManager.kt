package no.nav.helsemelding.payloadprocessing.keystore

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.iterator
import kotlin.collections.map
import kotlin.collections.toList
import kotlin.let
import kotlin.sequences.asSequence
import kotlin.sequences.firstOrNull

private val log = KotlinLogging.logger {}

class KeyStoreManager(private vararg val keyStoreConfig: KeyStoreConfig) {
    private val keyStores: List<Pair<KeyStore, KeyStoreConfig>>

    init {
        Security.addProvider(BouncyCastleProvider())
        keyStores = keyStoreResolver()
    }

    fun getPrivateKey(serialnumber: BigInteger): PrivateKey? {
        keyStores.forEach { (store, config) ->
            store.aliases().iterator().asSequence().firstOrNull { alias ->
                (store.getCertificate(alias) as X509Certificate).serialNumber == serialnumber
            }?.let { alias ->
                return store.getKey(alias, config.keyStorePass.toCharArray()) as PrivateKey?
            }
        }
        return null
    }

    fun getCertificate(alias: String): X509Certificate {
        return keyStores.firstNotNullOf { (store) -> store.getCertificate(alias) as X509Certificate }
    }

    private fun keyStoreResolver(): List<Pair<KeyStore, KeyStoreConfig>> {
        return keyStoreConfig.map { config ->
            Pair<KeyStore, KeyStoreConfig>(
                KeyStore.getInstance(config.keyStoreType)
                    .apply {
                        try {
                            load(config.openKeyStoreInputStream(), config.keyStorePass.toCharArray())
                            log.debug { "Keystore is loaded successfully: ${config.keyStoreFilePath}" }
                        } catch (e: Exception) {
                            log.error(e) { "Failed to load keystore: ${config.keyStoreFilePath}" }
                        }
                    },
                config
            )
        }.toList()
    }
}
