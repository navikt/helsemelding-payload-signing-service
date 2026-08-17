package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

/**
 * The direction of a message payload, determining whether it should be signed or verified.
 *
 * @property str the lowercase string value used by the service
 */
@Serializable
enum class Direction(val str: String) {
    /** Incoming message — the payload signature will be verified. */
    IN("in"),

    /** Outgoing message — the payload will be signed. */
    OUT("out")
}
