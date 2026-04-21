package sk.styk.martin.apkanalyzer.core.uilibrary.icons.app

import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import sk.styk.martin.apkanalyzer.core.common.logger.Logger

internal class PackageIconKeyer : Keyer<PackageIcon> {
    override fun key(data: PackageIcon, options: Options): String = "package_icon:${data.packageName}"
}

internal class PackageIconFetcher(
    private val data: PackageIcon,
    private val packageManager: PackageManager,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val drawable = runCatching {
            packageManager.getApplicationInfo(data.packageName, 0).loadIcon(packageManager)
        }.onFailure {
            Logger.e("PackageIconFetcher", it, "Icon not available for ${data.packageName}")
        }.getOrNull() ?: return null

        val bitmap = drawable.toBitmap()
        return ImageFetchResult(
            image = BitmapDrawable(null, bitmap).asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory(
        private val packageManager: PackageManager,
    ) : Fetcher.Factory<PackageIcon> {
        override fun create(data: PackageIcon, options: Options, imageLoader: ImageLoader): Fetcher =
            PackageIconFetcher(data, packageManager)
    }
}

