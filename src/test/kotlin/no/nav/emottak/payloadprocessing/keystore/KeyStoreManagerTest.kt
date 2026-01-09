package no.nav.helsemelding.payloadprocessing.keystore

import io.kotest.core.spec.style.StringSpec
import no.nav.helsemelding.payloadprocessing.config
import org.junit.jupiter.api.Assertions
import java.io.ByteArrayInputStream
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

private const val VALID_CERTIFICATE = "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"
private const val CERTIFICATE_ALIAS = "nav_virksomhet"

class KeyStoreManagerTest : StringSpec({

    "getCertificate should return a certificate from the keystore" {
        val keyStoreManager = KeyStoreManager(*config().keyStore.toTypedArray())

        val certificate = keyStoreManager.getCertificate(CERTIFICATE_ALIAS)
        val encodedCertificate = Base64.getEncoder().encodeToString(certificate.encoded)

        Assertions.assertEquals(VALID_CERTIFICATE, encodedCertificate)
    }

    "getPrivateKey should return a matching private key from the keystore" {
        val keyStoreManager = KeyStoreManager(*config().keyStore.toTypedArray())
        val validCertificate = parseCertificate(VALID_CERTIFICATE)

        val privateKey = keyStoreManager.getPrivateKey(validCertificate.serialNumber)

        Assertions.assertNotNull(privateKey)
        Assertions.assertTrue(keysMatch(privateKey!!, validCertificate.publicKey))
    }
}) {
    companion object {
        fun parseCertificate(certificateString: String): X509Certificate {
            val certificateBytes = Base64.getDecoder().decode(certificateString)
            val certificateInputStream = ByteArrayInputStream(certificateBytes)
            val certificateFactory = CertificateFactory.getInstance("X.509")
            return certificateFactory.generateCertificate(certificateInputStream) as X509Certificate
        }

        fun keysMatch(privateKey: PrivateKey, publicKey: PublicKey): Boolean {
            val testData = "test-data".toByteArray()
            val signature = Signature.getInstance("SHA256withRSA")

            signature.initSign(privateKey)
            signature.update(testData)
            val signedData = signature.sign()

            signature.initVerify(publicKey)
            signature.update(testData)
            return signature.verify(signedData)
        }
    }
}
