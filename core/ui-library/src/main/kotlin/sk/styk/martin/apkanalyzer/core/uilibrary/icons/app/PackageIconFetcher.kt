package sk.styk.martin.apkanalyzer.core.uilibrary.icons.app

import android.content.pm.PackageManager
import android.content.res.Resources
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
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference

internal class PackageIconKeyer : Keyer<AppReference> {
    override fun key(data: AppReference, options: Options): String = when (data) {
        is AppReference.InstalledPackage -> "package_icon:installed:${data.packageName}"
        is AppReference.ApkFile -> "package_icon:apk:${data.path}"
    }
}

internal class PackageIconFetcher(private val data: AppReference, private val packageManager: PackageManager) : Fetcher {

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

        val bitmap = drawable.toBitmap()
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory(private val packageManager: PackageManager) : Fetcher.Factory<AppReference> {
        override fun create(
            data: AppReference,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = PackageIconFetcher(data, packageManager)
    }

    private companion object {
        const val TAG = "PackageIconFetcher"
    }
}

private class ApkArchiveUnreadableException(path: String) : IllegalStateException("Can not parse APK archive: $path")

private class ApkIconUnresolvableException(path: String) : IllegalStateException("Can not resolve APK icon resource: $path")
