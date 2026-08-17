package no.nav.helsemelding.payloadsigning.model

/**
 * Error returned by the Payload Signing Service on failure.
 *
 * @property code the HTTP status code returned by the service (required)
 * @property message the error message returned by the service (required)
 */
data class MessageSigningError(
    val code: Int,
    val message: String
)
