package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

/**
 * Request for signing or verifying a message payload.
 *
 * @property direction the direction of the message — [Direction.OUT] to sign, [Direction.IN] to verify (required)
 * @property bytes the raw payload bytes to process (required)
 */
@Serializable
data class PayloadRequest(
    val direction: Direction,
    val bytes: ByteArray
)
