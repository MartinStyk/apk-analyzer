package sk.styk.martin.apkanalyzer.core.appexport.column

import sk.styk.martin.apkanalyzer.core.apps.signing.Certificate
import sk.styk.martin.apkanalyzer.core.apps.signing.CertificatePrincipal

internal fun Certificate.toInventoryValue(): InventoryValue = record(
    "signature_algorithm" to text(signAlgorithm),
    "certificate_md5" to text(certificateHashMd5),
    "certificate_sha1" to text(certificateHashSha1),
    "certificate_sha256" to text(certificateHashSha256),
    "public_key_md5" to text(publicKeyMd5),
    "public_key_sha1" to text(publicKeySha1),
    "public_key_sha256" to text(publicKeySha256),
    "valid_from" to text(validFrom.toString()),
    "valid_until" to text(validUntil.toString()),
    "serial_number" to text(serialNumber),
    "issuer" to issuer.toInventoryValue(),
    "subject" to subject.toInventoryValue(),
    "trust_level" to text(trustLevel.token),
    "self_signed" to flag(isSelfSigned),
)

private fun CertificatePrincipal.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "organization" to text(organization),
    "country" to text(country),
)
