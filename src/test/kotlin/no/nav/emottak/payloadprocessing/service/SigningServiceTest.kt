package no.nav.emottak.payloadprocessing.service

import io.kotest.core.spec.style.StringSpec
import no.nav.emottak.payloadprocessing.config
import no.nav.emottak.payloadprocessing.keystore.KeyStoreManager
import no.nav.emottak.payloadprocessing.model.SignatureDetails
import org.apache.commons.codec.binary.Base64.decodeBase64
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class SigningServiceTest : StringSpec({

    "signXml should return a signed document" {
        val keyStoreManager = KeyStoreManager(*config().signing.toTypedArray())
        val signingService = SigningService(keyStoreManager)

        val unsignedXMLInputStream = this::class.java.classLoader.getResourceAsStream("test.xml")
        val usignertDokument = createDocument(unsignedXMLInputStream!!)
        assertEquals(0, usignertDokument.getElementsByTagName("Signature").length)

        val signertDokument = signingService.signXml(
            document = usignertDokument,
            signatureDetails = signatureDetails()
        )

        assertEquals(1, signertDokument.getElementsByTagName("Signature").length)
    }
}) {
    companion object {
        fun createDocument(inputstream: InputStream): Document {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            return dbf.newDocumentBuilder().parse(inputstream)
        }

        private fun signatureDetails() = SignatureDetails(
            certificate = decodeBase64(
                "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".toByteArray()
            ),
            signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
            hashFunction = "http://www.w3.org/2001/04/xmlenc#sha256"
        )
    }
}
