package sk.styk.martin.apkanalyzer.core.uilibrary.icons.app

import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.Size
import coil3.size.pxOrElse
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

internal class PackageIconKeyer : Keyer<AppReference> {
    override fun key(data: AppReference, options: Options): String = when (data) {
        is AppReference.InstalledPackage -> "package_icon:installed:${data.packageName}"
        is AppReference.ApkFile -> "package_icon:apk:${data.path}"
    }
}

internal class PackageIconFetcher(
    private val data: AppReference,
    private val packageManager: PackageManager,
    private val size: Size,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val drawable = runCatchingCancellable {
            when (data) {
                is AppReference.InstalledPackage ->
                    packageManager
                        .getApplicationInfo(data.packageName.value, 0)
                        .loadIcon(packageManager)

                is AppReference.ApkFile -> {
                    val applicationInfo = packageManager
                        .getPackageArchiveInfo(data.path, 0)
                        ?.applicationInfo
                        ?.apply {
                            sourceDir = data.path
                            publicSourceDir = data.path
                        }
                        ?: throw ApkArchiveUnreadableException(data.path)

                    if (applicationInfo.icon == 0) {
                        packageManager.defaultActivityIcon
                    } else {
                        ResourcesCompat.getDrawable(
                            packageManager.getResourcesForApplication(applicationInfo),
                            applicationInfo.icon,
                            null,
                        ) ?: throw ApkIconUnresolvableException(data.path)
                    }
                }
            }
        }.onFailure { error ->
            val expectedIconMiss = error is PackageManager.NameNotFoundException ||
                error is Resources.NotFoundException ||
                error is ApkArchiveUnreadableException ||
                error is ApkIconUnresolvableException
            if (expectedIconMiss) {
                Logger.w(TAG, "Icon not available for $data: ${error.message}")
            } else {
                Logger.e(TAG, error, "Icon not available for $data")
            }
        }.getOrNull() ?: return null

        val bitmap = drawable.toBoundedBitmap(size)
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.MEMORY,
        )
    }

    private fun Drawable.toBoundedBitmap(size: Size): Bitmap {
        val intrinsicWidth = intrinsicWidth.takeIf { it > 0 } ?: MAX_ICON_PX
        val intrinsicHeight = intrinsicHeight.takeIf { it > 0 } ?: MAX_ICON_PX
        val maxWidth = size.width.pxOrElse { MAX_ICON_PX }.coerceAtMost(MAX_ICON_PX)
        val maxHeight = size.height.pxOrElse { MAX_ICON_PX }.coerceAtMost(MAX_ICON_PX)
        val scale = minOf(1f, maxWidth.toFloat() / intrinsicWidth, maxHeight.toFloat() / intrinsicHeight)
        val width = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        return toBitmap(width = width, height = height)
    }

    class Factory(private val packageManager: PackageManager) : Fetcher.Factory<AppReference> {
        override fun create(
            data: AppReference,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = PackageIconFetcher(data, packageManager, options.size)
    }

    private companion object {
        const val TAG = "PackageIconFetcher"
        const val MAX_ICON_PX = 2048
    }
}

private class ApkArchiveUnreadableException(path: String) : IllegalStateException("Can not parse APK archive: $path")

private class ApkIconUnresolvableException(path: String) : IllegalStateException("Can not resolve APK icon resource: $path")
