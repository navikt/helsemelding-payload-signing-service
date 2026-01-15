package no.nav.helsemelding.payloadsigning.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.helsemelding.payloadsigning.auth.AuthConfig.Companion.getTokenSupportConfig
import no.nav.helsemelding.payloadsigning.config
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication() {
    install(Authentication) {
        tokenValidationSupport(
            config().azureAuth.issuer.value,
            getTokenSupportConfig()
        )
    }
}
