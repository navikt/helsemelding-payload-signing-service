package no.nav.helsemelding.payloadsigning.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import no.nav.helsemelding.payloadsigning.config
import no.nav.helsemelding.payloadsigning.keystore.KeyStoreManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.math.BigInteger
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

sealed interface SigningContextError {
    data class KeyNotFound(
        val subject: String,
        val serialNumber: BigInteger
    ) : SigningContextError
}

sealed interface CertError {
    data class ParseFailed(val cause: Throwable) : CertError
    data class NotX509(val actualType: String?) : CertError
}

sealed interface SignXmlError {
    data class Certificate(val error: CertError) : SignXmlError
    data class SigningContext(val error: SigningContextError) : SignXmlError
    data class SignatureFailed(val message: String, val cause: Throwable) : SignXmlError
}

class SigningService(
    val keyStoreManager: KeyStoreManager
) {

    private val digestAlgorithm: String = "http://www.w3.org/2001/04/xmlenc#sha256"
    private val canonicalizationMethod: String = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
    private val signatureAlgorithm: String = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"

    private val factory = XMLSignatureFactory.getInstance("DOM")
    private val provider = BouncyCastleProvider()

    fun signXml(document: Document): Either<SignXmlError, Document> =
        either {
            val certificate = loadSigningCertificate().bind()
            val signingContext = createSigningContext(certificate, document).bind()
            val signature = buildXmlSignature(certificate)

            sign(signature, signingContext, document).bind()
        }

    private fun loadSigningCertificate(): Either<SignXmlError, X509Certificate> =
        Either.catch {
            keyStoreManager
                .getCertificate(config().signing.certificateAlias)
                .encoded
        }
            .mapLeft { t ->
                SignXmlError.SignatureFailed(
                    message = "Failed to read certificate bytes (alias=${config().signing.certificateAlias})",
                    cause = t
                )
            }
            .flatMap { bytes ->
                createX509Certificate(bytes)
                    .mapLeft(SignXmlError::Certificate)
            }

    private fun createSigningContext(
        certificate: X509Certificate,
        document: Document
    ): Either<SignXmlError, DOMSignContext> =
        buildSigningContext(certificate, document)
            .mapLeft(SignXmlError::SigningContext)

    private fun sign(
        signature: XMLSignature,
        signingContext: DOMSignContext,
        document: Document
    ): Either<SignXmlError, Document> =
        Either.catch {
            signature.sign(signingContext)
            document
        }
            .mapLeft { t ->
                SignXmlError.SignatureFailed(
                    message = "Failed to sign XML document",
                    cause = t
                )
            }

    private fun createX509Certificate(bytes: ByteArray): Either<CertError, X509Certificate> =
        Either
            .catch {
                val cf = CertificateFactory.getInstance("X.509", provider)
                cf.generateCertificate(ByteArrayInputStream(bytes))
            }
            .mapLeft { CertError.ParseFailed(it) }
            .flatMap { cert ->
                (cert as? X509Certificate)
                    ?.let { Either.Right(it) }
                    ?: Either.Left(CertError.NotX509(cert::class.qualifiedName))
            }

    private fun buildSigningContext(
        signerCertificate: X509Certificate,
        document: Document
    ): Either<SigningContextError, DOMSignContext> =
        either {
            val privateKey = ensureNotNull(keyStoreManager.getPrivateKey(signerCertificate.serialNumber)) {
                SigningContextError.KeyNotFound(
                    subject = signerCertificate.subjectX500Principal.name,
                    serialNumber = signerCertificate.serialNumber
                )
            }
            DOMSignContext(privateKey, document.documentElement)
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
