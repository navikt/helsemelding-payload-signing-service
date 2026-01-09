package no.nav.helsemelding.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
enum class Direction(val str: String) {
    IN("in"), OUT("out")
}
