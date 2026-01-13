package no.nav.helsemelding.payloadsigning

import arrow.core.memoize
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import no.nav.helsemelding.payloadsigning.config.Config

@OptIn(ExperimentalHoplite::class)
val config: () -> Config = {
    ConfigLoader.builder()
        .addResourceSource("/application-personal.conf", optional = true)
        .addResourceSource("/application.conf")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<Config>()
}
    .memoize()
