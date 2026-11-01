package sk.styk.martin.apkanalyzer.core.appexport

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appexport.appdata.InventoryAppDataLoader
import sk.styk.martin.apkanalyzer.core.appexport.appdata.valueOf
import sk.styk.martin.apkanalyzer.core.appexport.destination.ExportDestinations
import sk.styk.martin.apkanalyzer.core.appexport.writer.createWriter
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.file.ShareFileManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.StringWriter
import java.io.Writer
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "InventoryExport"
private const val PREVIEW_APP_COUNT = 3
private const val PREVIEW_MAX_LINES = 40
private const val SHARE_DIRECTORY = "inventory"

internal class InventoryExportManagerImpl @Inject constructor(
    private val appDataLoader: InventoryAppDataLoader,
    private val destinations: ExportDestinations,
    private val shareFileManager: ShareFileManager,
    private val dispatcherProvider: DispatcherProvider,
) : InventoryExportManager {

    override suspend fun preview(request: InventoryExportRequest): Result<InventoryPreview> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            val previewRequest = request.copy(apps = request.apps.take(PREVIEW_APP_COUNT))
            val output = StringWriter()
            writeInventory(previewRequest, output) {}
            val lines = output.toString().lines()
            InventoryPreview(
                content = lines.take(PREVIEW_MAX_LINES).joinToString(separator = "\n").trimEnd(),
                previewedAppCount = previewRequest.apps.size,
                isTruncated = lines.size > PREVIEW_MAX_LINES || request.apps.size > previewRequest.apps.size,
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
            destinations.write(destination) { stream ->
                incompleteAppCount = writeInventory(request, stream.bufferedWriter(Charsets.UTF_8)) { processedApps ->
                    send(InventoryExportProgress.Running(processedApps, totalApps))
                }
            }
            InventoryExportProgress.Completed(
                displayName = destinations.displayNameOr(destination, request.format.fileName(LocalDateTime.now())),
                exportedAppCount = totalApps,
                incompleteAppCount = incompleteAppCount,
            )
        }.getOrElse {
            Logger.e(TAG, it, "Can not export the app inventory")
            InventoryExportProgress.Failed
        }
        send(result)
    }.flowOn(dispatcherProvider.io())

    override suspend fun createShareDestination(fileName: String): Result<Uri> =
        shareFileManager.createShareFile(directory = SHARE_DIRECTORY, fileName = fileName)

    private suspend fun writeInventory(
        request: InventoryExportRequest,
        output: Writer,
        onAppWritten: suspend (processedApps: Int) -> Unit,
    ): Int {
        val writer = request.format.createWriter(request.attributes, output)
        var incompleteAppCount = 0
        writer.begin()
        appDataLoader.load(request.apps, request.attributes).collectIndexed { index, data ->
            if (!data.isComplete) incompleteAppCount++
            writer.writeApp(request.attributes.map { it.valueOf(data) })
            onAppWritten(index + 1)
        }
        writer.end()
        return incompleteAppCount
    }
}
