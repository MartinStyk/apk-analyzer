package sk.styk.martin.apkanalyzer.core.apps.model

import java.util.Locale

enum class SignatureAlgorithmStrength {
    Strong,
    Weak,
    Unknown,
    ;

    internal companion object {
        fun from(signAlgorithm: String): SignatureAlgorithmStrength {
            val normalized = signAlgorithm.uppercase(Locale.ROOT).replace("-", "")
            return when {
                normalized.contains("MD2") || normalized.contains("MD5") || normalized.contains("SHA1") -> Weak
                normalized.contains("SHA256") || normalized.contains("SHA384") || normalized.contains("SHA512") -> Strong
                else -> Unknown
            }
        }
    }
}
