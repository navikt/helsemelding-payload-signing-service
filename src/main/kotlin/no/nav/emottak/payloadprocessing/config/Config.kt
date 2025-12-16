package no.nav.emottak.payloadprocessing.config

import no.nav.emottak.payloadprocessing.keystore.KeyStoreConfig
import kotlin.time.Duration

data class Config(
    val server: Server,
    val signing: List<KeyStoreConfig>
)

data class Server(
    val port: Port,
    val preWait: Duration
)

@JvmInline
value class Port(val value: Int)
