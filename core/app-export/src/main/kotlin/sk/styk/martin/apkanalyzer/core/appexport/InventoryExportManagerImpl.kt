package sk.styk.martin.apkanalyzer.core.appexport

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.apps.AppDetailRepository
import sk.styk.martin.apkanalyzer.core.apps.InstalledAppsRepository
import sk.styk.martin.apkanalyzer.core.apps.model.InstalledApp
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigning
import sk.styk.martin.apkanalyzer.core.apps.signing.AppSigningRepository
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import sk.styk.martin.apkanalyzer.core.common.model.AppReference
import sk.styk.martin.apkanalyzer.core.common.model.PackageName
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "InventoryExport"
private const val PREVIEW_APP_COUNT = 3
private const val PREVIEW_MAX_LINES = 40
private const val SHARE_DIRECTORY = "inventory"

internal class InventoryExportManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppsRepository: InstalledAppsRepository,
    private val appDetailRepository: AppDetailRepository,
    private val appSigningRepository: AppSigningRepository,
    private val destinationWriter: ExportDestinationWriter,
    private val dispatcherProvider: DispatcherProvider,
) : InventoryExportManager {

    override suspend fun preview(request: InventoryExportRequest): Result<InventoryPreview> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val apps = request.copy(apps = request.apps.take(PREVIEW_APP_COUNT)).resolvedApps()
            val output = StringWriter()
            writeInventory(apps, request, output) {}
            val lines = output.toString().lines()
            InventoryPreview(
                content = lines.take(PREVIEW_MAX_LINES).joinToString(separator = "\n").trimEnd(),
                previewedAppCount = apps.size,
                isTruncated = lines.size > PREVIEW_MAX_LINES || request.apps.size > apps.size,
            )
        }.onFailure {
            Logger.e(TAG, it, "Can not build the inventory preview")
        }
    }

    override fun export(request: InventoryExportRequest, destination: Uri): Flow<InventoryExportProgress> = channelFlow {
        val totalApps = request.apps.size
        send(InventoryExportProgress.Running(processedApps = 0, totalApps = totalApps))
        val result = runCatchingCancellable {
            var incompleteAppCount = 0
            destinationWriter.write(destination) { stream ->
                val apps = request.resolvedApps()
                val output = stream.bufferedWriter(Charsets.UTF_8)
                incompleteAppCount = writeInventory(apps, request, output) { processedApps ->
                    send(InventoryExportProgress.Running(processedApps, totalApps))
                }
            }
            InventoryExportProgress.Completed(
                displayName = destinationWriter.displayNameOr(destination, request.format.fileName(LocalDateTime.now())),
                exportedAppCount = totalApps,
                incompleteAppCount = incompleteAppCount,
            )
        }.getOrElse {
            Logger.e(TAG, it, "Can not export the app inventory")
            InventoryExportProgress.Failed
        }
        send(result)
    }.flowOn(dispatcherProvider.io())

    override suspend fun createShareDestination(fileName: String): Result<Uri> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val directory = File(context.cacheDir, SHARE_DIRECTORY)
            directory.deleteRecursively()
            check(directory.mkdirs()) { "The share directory can not be created" }
            val file = File(directory, fileName)
            check(file.createNewFile()) { "The share file can not be created" }
            FileProvider.getUriForFile(context, context.packageName, file)
        }.onFailure {
            Logger.e(TAG, it, "Can not prepare the inventory for sharing")
        }
    }

    private suspend fun InventoryExportRequest.resolvedApps(): List<InstalledApp> {
        if (attributes.none { it.requiresUsageAccess }) return apps
        val enrichedApps = installedAppsRepository.awaitFullyEnrichedApps().associateBy { it.packageName }
        return apps.map { enrichedApps[it.packageName] ?: it }
    }

    private suspend fun writeInventory(
        apps: List<InstalledApp>,
        request: InventoryExportRequest,
        output: Writer,
        onAppWritten: suspend (processedApps: Int) -> Unit,
    ): Int {
        val signing = loadSigningIfNeeded(request.attributes)
        val needsAnalysis = request.attributes.any { it.requiresAppAnalysis }
        val writer = request.format.createWriter(request.attributes, output)
        var incompleteAppCount = 0
        writer.begin()
        apps.forEachIndexed { index, app ->
            currentCoroutineContext().ensureActive()
            val detail = if (needsAnalysis) {
                appDetailRepository.details(AppReference.InstalledPackage(app.packageName))
                    .onFailure { incompleteAppCount++ }
                    .getOrNull()
            } else {
                null
            }
            val data = InventoryAppData(app = app, signing = signing[app.packageName], detail = detail)
            writer.writeApp(request.attributes.map { it.valueOf(data) })
            onAppWritten(index + 1)
        }
        writer.end()
        return incompleteAppCount
    }

    private suspend fun loadSigningIfNeeded(attributes: List<InventoryAttribute>): Map<PackageName, AppSigning> =
        if (attributes.any { it.source == InventoryDataSource.SigningIndex }) {
            appSigningRepository.signing().first()
        } else {
            emptyMap()
        }
}
