package no.nav.helsemelding.payloadsigning.model

import kotlinx.serialization.Serializable

@Serializable
enum class Direction(val str: String) {
    IN("in"), OUT("out")
}
