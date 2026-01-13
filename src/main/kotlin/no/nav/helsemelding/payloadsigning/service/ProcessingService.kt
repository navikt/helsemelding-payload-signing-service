package no.nav.helsemelding.payloadsigning.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.payloadsigning.model.PayloadResponse
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val log = KotlinLogging.logger {}

sealed interface ProcessingError {
    data class InvalidRequest(val message: String) : ProcessingError
    data class XmlParseFailed(val cause: Throwable) : ProcessingError
    data class SigningFailed(val error: SignXmlError) : ProcessingError
}

class ProcessingService(
    val signingService: SigningService
) {
    fun processIncoming(request: PayloadRequest): Either<ProcessingError, PayloadResponse> =
        PayloadResponse(request.bytes)
            .right()
            .also { log.info { "Processing incoming payload..." } }

    fun processOutgoing(request: PayloadRequest): Either<ProcessingError, PayloadResponse> =
        either {
            val xmlDocument =
                Either.catch { request.bytes.toXmlDocument() }
                    .mapLeft(ProcessingError::XmlParseFailed)
                    .bind()

            val signedDocument =
                signingService.signXml(xmlDocument)
                    .mapLeft(ProcessingError::SigningFailed)
                    .bind()

            PayloadResponse(signedDocument.toByteArray())
        }

    private fun ByteArray.toXmlDocument(): Document =
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(this))

    private fun Document.toByteArray(): ByteArray =
        ByteArrayOutputStream().use { output ->
            TransformerFactory.newInstance()
                .newTransformer()
                .transform(DOMSource(this), StreamResult(output))
            output.toByteArray()
        }
}
