package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

@Serializable
data class PayloadRequest(
    val direction: Direction,
    val bytes: ByteArray
)
