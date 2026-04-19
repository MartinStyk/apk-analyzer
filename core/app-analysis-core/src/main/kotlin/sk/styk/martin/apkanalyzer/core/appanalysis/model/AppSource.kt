package sk.styk.martin.apkanalyzer.core.appanalysis.model

enum class AppSource(val sourceName: String, val installerPackageName: String? = null) {
    GOOGLE_PLAY("Google Play", "com.android.vending"),
    AMAZON_STORE("Amazon App Store", "com.amazon.venezia"),
    SYSTEM_PREINSTALED("Pre-installed", "system"),
    UNKNOWN("Unknown"),
    ;

    override fun toString(): String = sourceName
}
