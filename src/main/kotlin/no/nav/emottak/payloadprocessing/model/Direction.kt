package no.nav.emottak.payloadprocessing.model

import kotlinx.serialization.Serializable

@Serializable
enum class Direction(val str: String) {
    IN("in"), OUT("out")
}
