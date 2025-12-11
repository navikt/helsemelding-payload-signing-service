package no.nav.emottak.payloadprocessing.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.emottak.payloadprocessing.model.PayloadRequest
import no.nav.emottak.payloadprocessing.model.PayloadResponse
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

private val log = KotlinLogging.logger {}

class ProcessingService {

    fun processIncoming(request: PayloadRequest): PayloadResponse {
        log.info { "Processing incoming payload..." }
        return PayloadResponse(request.bytes)
    }

    fun processOutgoing(request: PayloadRequest): PayloadResponse {
        val xmlDocument = request.bytes.toXmlDocument()

        log.info { "Processing outgoing payload..." }
        return PayloadResponse(request.bytes)
    }
}

fun ByteArray.toXmlDocument(): Document {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = true

    val inputStream = ByteArrayInputStream(this)

    return dbf.newDocumentBuilder().parse(inputStream)
}
