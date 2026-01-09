package no.nav.helsemelding.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
data class SignatureDetails(
    val certificate: ByteArray,
    val signatureAlgorithm: String,
    val hashFunction: String
)
