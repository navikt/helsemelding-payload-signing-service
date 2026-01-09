package no.nav.helsemelding.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
data class PayloadResponse(
    val bytes: ByteArray
)
