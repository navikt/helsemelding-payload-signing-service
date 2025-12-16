package no.nav.emottak.payloadprocessing.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.emottak.payloadprocessing.model.PayloadRequest
import no.nav.emottak.payloadprocessing.model.PayloadResponse
import no.nav.emottak.payloadprocessing.model.SignatureDetails
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
        val signatureDetails = SignatureDetails(
            certificate = "deferfr".toByteArray(),
            signatureAlgorithm = "rsa-sha256",
            hashFunction = "sha256"
        )

        val xmlDocument = request.bytes.toXmlDocument()
        val signedDocument: Document = signingService.signXml(xmlDocument, signatureDetails)

        return PayloadResponse(signedDocument.toByteArray())
    }
}

fun ByteArray.toXmlDocument(): Document {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = true

    val inputStream = ByteArrayInputStream(this)

    return dbf.newDocumentBuilder().parse(inputStream)
}

fun Document.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val xmlSource = DOMSource(this)
    val result = StreamResult(outputStream)
    TransformerFactory.newInstance().newTransformer().transform(xmlSource, result)
    return outputStream.toByteArray()
}
