package sk.styk.martin.apkanalyzer.core.common.file

import android.net.Uri
import sk.styk.martin.apkanalyzer.core.common.coroutines.runCatchingCancellable
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

class DocumentFileManagerFake : DocumentFileManager {
    private val documents = mutableMapOf<Uri, ByteArray>()

    operator fun get(uri: Uri): ByteArray? = documents[uri]

    operator fun set(uri: Uri, content: ByteArray) {
        documents[uri] = content
    }

    override suspend fun <T> writeOrDeleteOnFailure(destination: Uri, write: suspend (OutputStream) -> T): Result<T> = runCatchingCancellable {
        val output = ByteArrayOutputStream()
        val written = write(output)
        documents[destination] = output.toByteArray()
        written
    }

    override suspend fun <T> read(source: Uri, read: suspend (InputStream) -> T): Result<T> = runCatchingCancellable {
        val content = documents[source] ?: throw FileNotFoundException(source.toString())
        read(content.inputStream())
    }

    override suspend fun displayNameOr(destination: Uri, fallback: String): String = fallback
}
