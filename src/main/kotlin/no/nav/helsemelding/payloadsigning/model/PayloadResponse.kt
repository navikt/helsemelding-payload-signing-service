package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

@Serializable
data class PayloadResponse(
    val bytes: ByteArray
)
