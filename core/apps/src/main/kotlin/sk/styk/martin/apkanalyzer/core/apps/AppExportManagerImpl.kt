package sk.styk.martin.apkanalyzer.core.apps

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class AppExportManagerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
) : AppExportManager {

    private val contentResolver = context.contentResolver

    override suspend fun exportApk(reference: AppReference, destination: Uri): Result<ApkExportResult> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val source = resolveApkSource(reference)
            require(source.file.isFile) { "The APK file is no longer available" }
            writeTo(destination) { output ->
                source.file.inputStream().use { input ->
                    input.copyCancellableTo(output)
                }
            }
            ApkExportResult(
                displayName = destination.displayNameOr(source.defaultName),
                baseApkOnly = source.baseApkOnly,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not export APK for $reference")
        }
    }

    override suspend fun exportIcon(reference: AppReference, destination: Uri): Result<IconExportResult> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val source = resolveIconSource(reference)
            writeIcon(source.drawable, destination)
            IconExportResult(destination.displayNameOr(source.defaultName))
        }.onFailure {
            Logger.e(TAG, it, "Can not export icon for $reference")
        }
    }

    private fun resolveApkSource(reference: AppReference): ApkSource = when (reference) {
        is AppReference.InstalledPackage -> {
            val applicationInfo = packageManager.getApplicationInfo(reference.packageName.value, 0)
            ApkSource(
                file = File(applicationInfo.sourceDir),
                defaultName = "${reference.packageName}.apk",
                baseApkOnly = !applicationInfo.splitSourceDirs.isNullOrEmpty(),
            )
        }

        is AppReference.ApkFile -> ApkSource(
            file = File(reference.path),
            defaultName = File(reference.path).name,
            baseApkOnly = false,
        )
    }

    private fun resolveIconSource(reference: AppReference): IconSource = when (reference) {
        is AppReference.InstalledPackage -> IconSource(
            drawable = packageManager.getApplicationInfo(reference.packageName.value, 0).loadIcon(packageManager),
            defaultName = "${reference.packageName}.png",
        )

        is AppReference.ApkFile -> {
            val applicationInfo = packageManager.getPackageArchiveInfo(reference.path, 0)
                ?.applicationInfo
                ?: error("Can not read the APK file")
            applicationInfo.sourceDir = reference.path
            applicationInfo.publicSourceDir = reference.path
            IconSource(
                drawable = if (applicationInfo.icon == 0) {
                    packageManager.defaultActivityIcon
                } else {
                    packageManager.getResourcesForApplication(applicationInfo)
                        .getDrawable(applicationInfo.icon, null)
                },
                defaultName = "${applicationInfo.packageName}.png",
            )
        }
    }

    private suspend fun writeIcon(drawable: Drawable, destination: Uri) {
        val bitmap = drawable.toNaturalBitmap()
        writeTo(destination) { output ->
            currentCoroutineContext().ensureActive()
            check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESSION_QUALITY, output)) {
                "The icon could not be encoded as PNG"
            }
        }
    }

    private suspend fun writeTo(destination: Uri, write: suspend (OutputStream) -> Unit) {
        var writeCompleted = false
        try {
            val output = contentResolver.openOutputStream(destination, "w")
                ?: error("The selected destination can not be opened")
            output.use { write(it) }
            writeCompleted = true
        } finally {
            if (!writeCompleted) {
                runCatching { contentResolver.delete(destination, null, null) }
                    .onFailure { Logger.w(TAG, it, "Can not remove incomplete export") }
            }
        }
    }

    private suspend fun InputStream.copyCancellableTo(output: OutputStream) {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        while (true) {
            val readCount = read(buffer)
            if (readCount < 0) break
            currentCoroutineContext().ensureActive()
            output.write(buffer, 0, readCount)
        }
    }

    private fun Uri.displayNameOr(fallback: String): String {
        val displayName = runCatching {
            contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.onFailure {
            Logger.w(TAG, it, "Can not read the exported file name")
        }.getOrNull()
        return displayName?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun Drawable.toNaturalBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap
        }
        val width = intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
        val height = intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val previousBounds = android.graphics.Rect(bounds)
            setBounds(0, 0, width, height)
            draw(Canvas(bitmap))
            bounds = previousBounds
        }
    }

    private companion object {
        const val TAG = "AppExportManager"
        const val DEFAULT_ICON_SIZE = 512
        const val COPY_BUFFER_SIZE = 64 * 1024
        const val PNG_COMPRESSION_QUALITY = 100
    }

    private data class ApkSource(
        val file: File,
        val defaultName: String,
        val baseApkOnly: Boolean,
    )

    private data class IconSource(val drawable: Drawable, val defaultName: String)
}
