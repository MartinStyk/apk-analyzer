package sk.styk.martin.apkanalyzer.core.appexport

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.appexport.column.valueOf
import sk.styk.martin.apkanalyzer.core.appexport.format.createWriter
import sk.styk.martin.apkanalyzer.core.appexport.source.InventoryAppDataLoader
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import sk.styk.martin.apkanalyzer.core.common.file.DocumentFileManager
import sk.styk.martin.apkanalyzer.core.common.file.ShareFileManager
import sk.styk.martin.apkanalyzer.core.common.logger.Logger
import java.io.StringWriter
import java.io.Writer
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

private const val TAG = "InventoryExport"
private const val SHARE_DIRECTORY = "inventory"
private const val PREVIEW_APP_COUNT = 3
private const val PREVIEW_MAX_LINES = 40
private const val PREVIEW_MAX_CHARACTERS = 4_000

internal class InventoryExportManagerImpl @Inject constructor(
    private val appDataLoader: InventoryAppDataLoader,
    private val documentFileManager: DocumentFileManager,
    private val shareFileManager: ShareFileManager,
    private val dispatcherProvider: DispatcherProvider,
) : InventoryExportManager {

    override suspend fun preview(request: InventoryExportRequest): Result<InventoryPreview> = withContext(dispatcherProvider.io()) {
        runCatchingCancellable {
            request.requireSupportedFormat()
            val previewRequest = request.copy(apps = request.apps.take(PREVIEW_APP_COUNT))
            val output = StringWriter()
            writeInventory(previewRequest, output, Instant.now()) {}
            output.toString().toPreview(previewedAppCount = previewRequest.apps.size, hasMoreApps = request.apps.size > previewRequest.apps.size)
        }.onFailure {
            Logger.e(TAG, it, "Can not build the inventory preview")
        }
    }

    override fun export(request: InventoryExportRequest, destination: InventoryDestination): Flow<InventoryExportProgress> = channelFlow {
        val totalApps = request.apps.size
        send(InventoryExportProgress.Running(processedApps = 0, totalApps = totalApps))
        val result = runCatchingCancellable {
            request.requireSupportedFormat()
            val createdAt = ZonedDateTime.now()
            val fileName = request.format.fileName(createdAt.toLocalDateTime())
            val uri = destination.resolve(fileName)
            val incompleteAppCount = documentFileManager.writeOrDeleteOnFailure(uri) { stream ->
                writeInventory(request, stream.bufferedWriter(Charsets.UTF_8), createdAt.toInstant()) { processedApps ->
                    send(InventoryExportProgress.Running(processedApps, totalApps))
                }
            }.getOrThrow()
            InventoryExportProgress.Completed(
                uri = uri,
                displayName = documentFileManager.displayNameOr(uri, fileName),
                exportedAppCount = totalApps,
                incompleteAppCount = incompleteAppCount,
            )
        }.getOrElse {
            Logger.e(TAG, it, "Can not export the app inventory")
            InventoryExportProgress.Failed
        }
        send(result)
    }.flowOn(dispatcherProvider.io())

    private suspend fun InventoryDestination.resolve(fileName: String): Uri = when (this) {
        is InventoryDestination.Document -> uri
        InventoryDestination.Share -> shareFileManager.createShareFile(SHARE_DIRECTORY, fileName).getOrThrow()
    }

    private fun InventoryExportRequest.requireSupportedFormat() {
        require(format.supports(columns)) { "Selected columns do not support $format" }
    }

    private fun String.toPreview(previewedAppCount: Int, hasMoreApps: Boolean): InventoryPreview {
        val lines = lines()
        val lineLimitedContent = lines.take(PREVIEW_MAX_LINES).joinToString(separator = "\n")
        return InventoryPreview(
            content = lineLimitedContent.take(PREVIEW_MAX_CHARACTERS).trimEnd(),
            previewedAppCount = previewedAppCount,
            isTruncated = hasMoreApps || lines.size > PREVIEW_MAX_LINES || lineLimitedContent.length > PREVIEW_MAX_CHARACTERS,
        )
    }

    private suspend fun writeInventory(
        request: InventoryExportRequest,
        output: Writer,
        generatedAt: Instant,
        onAppWritten: suspend (processedApps: Int) -> Unit,
    ): Int = request.format.createWriter(request.columns, output, generatedAt).use { writer ->
        var incompleteAppCount = 0
        writer.begin()
        appDataLoader.load(request.apps, request.columns.map { it.source }.toSet()).collectIndexed { index, data ->
            if (!data.isComplete) incompleteAppCount++
            writer.writeApp(request.columns.map { it.valueOf(data, request.fingerprintFormat) })
            onAppWritten(index + 1)
        }
        incompleteAppCount
    }
}
