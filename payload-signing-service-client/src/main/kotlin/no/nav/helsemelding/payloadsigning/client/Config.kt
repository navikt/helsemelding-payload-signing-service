package no.nav.helsemelding.payloadsigning.client

import java.net.URL
import java.time.Duration

internal data class Config(
    val azureAuth: AzureAuth,
    val httpClient: HttpClient,
    val httpTokenClient: HttpClient,
    val payloadSigningService: PayloadSigningService
) {
    data class AzureAuth(
        val grantType: GrantType,
        val tokenEndpoint: TokenEndpoint,
        val appClientId: ApplicationId,
        val appClientSecret: ApplicationSecret
    ) {
        @JvmInline
        value class GrantType(val value: String)

        @JvmInline
        value class TokenEndpoint(val value: String)

        @JvmInline
        value class ApplicationId(val value: String)

        @JvmInline
        value class ApplicationSecret(val value: String)
    }

    data class HttpClient(
        val connectionTimeout: Duration
    )

    data class PayloadSigningService(
        val url: URL
    )
}
