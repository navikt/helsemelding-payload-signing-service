package no.nav.emottak.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
data class PayloadRequest(
    val direction: Direction,
    val bytes: ByteArray
)
