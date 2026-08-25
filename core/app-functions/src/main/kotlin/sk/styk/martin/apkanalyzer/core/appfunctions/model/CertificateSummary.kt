package sk.styk.martin.apkanalyzer.core.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable
import java.time.Instant

/** One certificate an installed app is currently signed with. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CertificateSummary(
    /** The cryptographic algorithm used to sign the certificate, for example "SHA256withRSA". */
    val signatureAlgorithm: String,
    /**
     * True if the app signed itself rather than being signed by a recognized certificate
     * authority. Most Android apps are self-signed; this is normal and not a sign of tampering by
     * itself.
     */
    val isSelfSigned: Boolean,
    /**
     * The certificate's SHA-256 fingerprint, formatted as colon-separated hex pairs. Use this to
     * compare whether two apps share the same signer.
     */
    val sha256Fingerprint: String,
    /** When the certificate became valid. */
    val validFrom: Instant,
    /** When the certificate expires. */
    val validUntil: Instant,
    /** The organization named on the certificate, or null if none is set. */
    val issuerOrganization: String?,
)
