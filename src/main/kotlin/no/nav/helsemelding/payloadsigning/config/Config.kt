package no.nav.helsemelding.payloadsigning.config

import no.nav.helsemelding.payloadsigning.keystore.KeyStoreConfig
import kotlin.time.Duration

data class Config(
    val server: Server,
    val keyStore: List<KeyStoreConfig>,
    val signing: Signing,
    val azureAuth: AzureAuth
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

data class AzureAuth(
    val issuer: Issuer,
    val appName: AppName,
    val appScope: AppScope,
    val clusterName: ClusterName,
    val azureWellKnownUrl: AzureWellKnownUrl,
    val acceptedAudience: AcceptedAudience
) {
    @JvmInline
    value class Issuer(val value: String)

    @JvmInline
    value class AppName(val value: String)

    @JvmInline
    value class ClusterName(val value: String)

    @JvmInline
    value class AppScope(val value: String)

    @JvmInline
    value class AzureWellKnownUrl(val value: String)

    @JvmInline
    value class AcceptedAudience(val value: String)
}
