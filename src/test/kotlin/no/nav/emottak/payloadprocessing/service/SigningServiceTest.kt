package no.nav.helsemelding.payloadprocessing.service

import arrow.core.getOrElse
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.helsemelding.payloadprocessing.config
import no.nav.helsemelding.payloadprocessing.keystore.KeyStoreManager
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class SigningServiceTest : StringSpec({

    "signXml should return a signed document" {
        val keyStoreManager = KeyStoreManager(*config().keyStore.toTypedArray())
        val signingService = SigningService(keyStoreManager)

        val unsignedXmlStream =
            this::class.java.classLoader.getResourceAsStream("test.xml")
                ?: error("Missing test resource: test.xml")

        val unsignedDocument = createDocument(unsignedXmlStream)
        unsignedDocument.getElementsByTagName("Signature").length shouldBe 0

        val signedDocument = signingService
            .signXml(unsignedDocument)
            .getOrElse { e -> fail("signXml returned Left: $e") }

        signedDocument.getElementsByTagName("Signature").length shouldBe 1
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
