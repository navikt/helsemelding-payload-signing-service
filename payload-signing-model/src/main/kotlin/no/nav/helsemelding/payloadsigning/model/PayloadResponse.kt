package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

/**
 * Response containing the processed payload bytes after signing or verification.
 *
 * @property bytes the processed payload bytes (required)
 */
@Serializable
data class PayloadResponse(
    val bytes: ByteArray
)
