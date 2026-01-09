package no.nav.helsemelding.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
data class PayloadRequest(
    val direction: Direction,
    val bytes: ByteArray
)
