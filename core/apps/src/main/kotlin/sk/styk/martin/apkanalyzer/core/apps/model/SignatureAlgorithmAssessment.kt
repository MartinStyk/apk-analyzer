package sk.styk.martin.apkanalyzer.core.apps.model

import java.util.Locale

enum class SignatureAlgorithmAssessment {
    ModernDigest,
    WeakSha1Digest,
    WeakMd5Digest,
    WeakMd2Digest,
    Unknown,
    ;

    internal companion object {
        private val recognizedAlgorithm = Regex("^(MD2|MD5|SHA1|SHA256|SHA384|SHA512)WITH(RSA|DSA|ECDSA)$")

        fun from(signAlgorithm: String): SignatureAlgorithmAssessment {
            val normalized = signAlgorithm.uppercase(Locale.ROOT).replace("-", "")
            val digest = recognizedAlgorithm.matchEntire(normalized)?.groupValues?.get(1) ?: return Unknown
            return when (digest) {
                "MD2" -> WeakMd2Digest
                "MD5" -> WeakMd5Digest
                "SHA1" -> WeakSha1Digest
                else -> ModernDigest
            }
        }
    }
}
