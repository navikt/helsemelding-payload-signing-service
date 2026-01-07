package no.nav.emottak.payloadprocessing.service

import io.kotest.core.spec.style.StringSpec
import no.nav.emottak.payloadprocessing.config
import no.nav.emottak.payloadprocessing.keystore.KeyStoreManager
import org.junit.jupiter.api.Assertions
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class SigningServiceTest : StringSpec({

    "signXml should return a signed document" {
        val keyStoreManager = KeyStoreManager(*config().keyStore.toTypedArray())
        val signingService = SigningService(keyStoreManager)

        val unsignedXMLInputStream = this::class.java.classLoader.getResourceAsStream("test.xml")
        val unsignedDocument = createDocument(unsignedXMLInputStream!!)
        Assertions.assertEquals(0, unsignedDocument.getElementsByTagName("Signature").length)

        val signedDocument = signingService.signXml(unsignedDocument)

        Assertions.assertEquals(1, signedDocument.getElementsByTagName("Signature").length)
    }
}) {
    companion object {
        fun createDocument(inputStream: InputStream): Document {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            documentBuilderFactory.isNamespaceAware = true
            return documentBuilderFactory.newDocumentBuilder().parse(inputStream)
        }
    }
}
