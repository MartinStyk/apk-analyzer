package sk.styk.martin.apkanalyzer.feature.whatchanged.phone.access

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface LockableRow<out T> {
    val key: String

    data class Readable<out T>(val item: T, override val key: String) : LockableRow<T>

    data class Locked(override val key: String, val packageName: String, val appName: String) : LockableRow<Nothing>
}

internal fun <T> T.toLockableRow(
    key: String,
    isReadable: Boolean,
    packageName: String,
    appName: String,
): LockableRow<T> = if (isReadable) LockableRow.Readable(this, key) else LockableRow.Locked(key, packageName, appName)
