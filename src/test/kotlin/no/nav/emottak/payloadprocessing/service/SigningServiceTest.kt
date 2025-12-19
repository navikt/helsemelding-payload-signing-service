package no.nav.emottak.payloadprocessing.service

import io.kotest.core.spec.style.StringSpec
import no.nav.emottak.payloadprocessing.config
import no.nav.emottak.payloadprocessing.keystore.KeyStoreManager
import no.nav.emottak.payloadprocessing.model.SignatureDetails
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.Assertions
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class SigningServiceTest : StringSpec({

    "signXml should return a signed document" {
        val keyStoreManager = KeyStoreManager(*config().signing.toTypedArray())
        val signingService = SigningService(keyStoreManager)

        val unsignedXMLInputStream = this::class.java.classLoader.getResourceAsStream("test.xml")
        val unsignedDocument = createDocument(unsignedXMLInputStream!!)
        Assertions.assertEquals(0, unsignedDocument.getElementsByTagName("Signature").length)

        val signedDocument = signingService.signXml(
            document = unsignedDocument,
            signatureDetails = signatureDetails()
        )

        Assertions.assertEquals(1, signedDocument.getElementsByTagName("Signature").length)
    }
}) {
    companion object {
        fun createDocument(inputstream: InputStream): Document {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            return dbf.newDocumentBuilder().parse(inputstream)
        }

        private fun signatureDetails() = SignatureDetails(
            certificate = Base64.decodeBase64(
                "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".toByteArray()
            ),
            signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
            hashFunction = "http://www.w3.org/2001/04/xmlenc#sha256"
        )
    }
}
