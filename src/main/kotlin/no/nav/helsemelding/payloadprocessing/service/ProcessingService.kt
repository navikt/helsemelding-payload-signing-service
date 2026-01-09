package no.nav.helsemelding.payloadprocessing.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.payloadprocessing.model.PayloadRequest
import no.nav.helsemelding.payloadprocessing.model.PayloadResponse
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val log = KotlinLogging.logger {}

class ProcessingService(
    val signingService: SigningService
) {

    fun processIncoming(request: PayloadRequest): PayloadResponse {
        log.info { "Processing incoming payload..." }
        return PayloadResponse(request.bytes)
    }

    fun processOutgoing(request: PayloadRequest): PayloadResponse {
        val xmlDocument = request.bytes.toXmlDocument()
        val signedDocument: Document = signingService.signXml(xmlDocument)

        return PayloadResponse(signedDocument.toByteArray())
    }
}

fun ByteArray.toXmlDocument(): Document {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.isNamespaceAware = true

    val inputStream = ByteArrayInputStream(this)

    return documentBuilderFactory.newDocumentBuilder().parse(inputStream)
}

fun Document.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val xmlSource = DOMSource(this)
    val result = StreamResult(outputStream)
    TransformerFactory.newInstance().newTransformer().transform(xmlSource, result)
    return outputStream.toByteArray()
}
