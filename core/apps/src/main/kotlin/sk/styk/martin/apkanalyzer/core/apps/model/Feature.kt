package sk.styk.martin.apkanalyzer.core.apps.model

sealed interface Feature {
    val isRequired: Boolean

    data class Hardware(val name: String, override val isRequired: Boolean = false) : Feature

    data class OpenGlEs(val reqGlEsVersion: Int, override val isRequired: Boolean = false) : Feature {
        val versionName: String
            get() = versionName(reqGlEsVersion)

        companion object {
            fun versionName(reqGlEsVersion: Int): String {
                val major = (reqGlEsVersion shr 16) and 0xFFFF
                val minor = reqGlEsVersion and 0xFFFF
                return "$major.$minor"
            }
        }
    }
}
