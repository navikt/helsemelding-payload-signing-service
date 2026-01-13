package no.nav.helsemelding.payloadsigning.config

import no.nav.helsemelding.payloadsigning.keystore.KeyStoreConfig
import kotlin.time.Duration

data class Config(
    val server: Server,
    val keyStore: List<KeyStoreConfig>,
    val signing: Signing
)

data class Server(
    val port: Port,
    val preWait: Duration
)

@JvmInline
value class Port(val value: Int)

data class Signing(
    val certificateAlias: String
)
