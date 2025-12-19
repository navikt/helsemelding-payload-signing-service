package no.nav.emottak.payloadprocessing.service

import no.nav.emottak.payloadprocessing.config
import no.nav.emottak.payloadprocessing.keystore.KeyStoreManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.xml.crypto.dsig.Reference
import javax.xml.crypto.dsig.SignedInfo
import javax.xml.crypto.dsig.Transform
import javax.xml.crypto.dsig.XMLSignature
import javax.xml.crypto.dsig.XMLSignatureFactory
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec

class SigningService(
    val keyStoreManager: KeyStoreManager
) {

    private val digestAlgorithm: String = "http://www.w3.org/2001/04/xmlenc#sha256"
    private val canonicalizationMethod: String = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
    private val signatureAlgorithm: String = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"

    private val factory = XMLSignatureFactory.getInstance("DOM")
    private val provider = BouncyCastleProvider()

    fun signXml(document: Document): Document {
        val certificateBytes = keyStoreManager.getCertificate(config().signing.certificateAlias).encoded
        val certificate: X509Certificate = createX509Certificate(certificateBytes)

        val signingContext = buildSigningContext(certificate, document)
        val signature = buildXmlSignature(certificate)

        signature.sign(signingContext)
        return document
    }

    private fun createX509Certificate(byteArray: ByteArray): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509", provider)
        return try {
            cf.generateCertificate(ByteArrayInputStream(byteArray)) as X509Certificate
        } catch (e: CertificateException) {
            throw RuntimeException("Can not create X509Certificate from ByteArray", e)
        }
    }

    private fun buildSigningContext(
        signerCertificate: X509Certificate,
        document: Document
    ): DOMSignContext {
        val signerKey = keyStoreManager.getPrivateKey(signerCertificate.serialNumber)
            ?: throw SignatureException(
                "Fant ikke key for sertifikat med subject ${signerCertificate.subjectX500Principal.name} " +
                    "og serienummer ${signerCertificate.serialNumber} i keystore"
            )
        val signingContext = DOMSignContext(signerKey, document.documentElement)
        return signingContext
    }

    private fun buildXmlSignature(signerCertificate: X509Certificate): XMLSignature {
        val keyInfoFactory = factory.keyInfoFactory
        val x509Content: MutableList<Any?> = ArrayList()
        x509Content.add(signerCertificate)
        val x509data = keyInfoFactory.newX509Data(x509Content)
        val keyInfo = keyInfoFactory.newKeyInfo(listOf(x509data))
        val signature = factory.newXMLSignature(createSignedInfo(), keyInfo)
        return signature
    }

    private fun createSignedInfo(): SignedInfo {
        return factory.newSignedInfo(
            factory.newCanonicalizationMethod(
                canonicalizationMethod,
                null as C14NMethodParameterSpec?
            ),
            factory.newSignatureMethod(signatureAlgorithm, null),
            listOf(createReference())
        )
    }

    private fun createReference(): Reference {
        return factory.newReference(
            "",
            factory.newDigestMethod(digestAlgorithm, null),
            listOf(factory.newTransform(Transform.ENVELOPED, null as TransformParameterSpec?)),
            null,
            null
        )
    }
}

class SignatureException(override val message: String, e: Exception? = null) : Exception(message, e)
