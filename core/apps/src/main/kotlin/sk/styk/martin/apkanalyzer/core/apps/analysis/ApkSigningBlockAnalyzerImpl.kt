package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.apps.model.SigningSchemeVersion
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.util.jar.JarFile
import javax.inject.Inject

private const val TAG = "ApkSigningBlockAnalyzerImpl"
private const val ANDROID_MANIFEST_ENTRY = "AndroidManifest.xml"

internal class ApkSigningBlockAnalyzerImpl @Inject constructor(private val signingBlockParser: ApkSigningBlockParser) : ApkSigningBlockAnalyzer {

    override fun detectSchemeVersions(apkPath: String): List<SigningSchemeVersion>? = runCatching {
        val blockVersions = signingBlockParser.parseSchemeVersions(apkPath) ?: return null
        buildSet {
            if (hasVerifiedJarSignature(apkPath)) add(SigningSchemeVersion.V1)
            addAll(blockVersions)
        }
            .takeIf { it.isNotEmpty() }
            ?.sorted()
    }.onFailure {
        Logger.e(TAG, it, "Could not determine APK signing schemes")
    }.getOrNull()

    private fun hasVerifiedJarSignature(apkPath: String): Boolean = JarFile(apkPath, true).use { jarFile ->
        val manifestEntry = jarFile.getJarEntry(ANDROID_MANIFEST_ENTRY) ?: return@use false
        jarFile.getInputStream(manifestEntry).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var readCount: Int
            do {
                readCount = input.read(buffer)
            } while (readCount != -1)
        }
        manifestEntry.certificates?.isNotEmpty() == true
    }
}
